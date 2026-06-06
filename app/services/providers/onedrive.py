"""OneDrive cloud provider adapter — uses Microsoft Graph API via httpx."""
from __future__ import annotations

import mimetypes

import httpx

from app.services.providers.base import (
    BaseCloudProvider,
    ProviderAccount,
    QuotaInfo,
    RemoteFile,
)

GRAPH_BASE = "https://graph.microsoft.com/v1.0"


class OneDriveProvider(BaseCloudProvider):
    """Adapter for Microsoft OneDrive (personal / work) via Graph API."""

    # ---------------------------------------------------------------------------
    # Helpers
    # ---------------------------------------------------------------------------

    def _headers(self, credentials: dict) -> dict:
        return {"Authorization": f"Bearer {credentials['access_token']}"}

    # ---------------------------------------------------------------------------
    # BaseCloudProvider interface
    # ---------------------------------------------------------------------------

    async def create_account(self, email: str, password: str) -> ProviderAccount:
        raise NotImplementedError(
            "OneDrive requires a Microsoft OAuth flow — use /api/providers/connect?provider=onedrive"
        )

    async def upload_file(
        self,
        file_path: str,
        filename: str,
        remote_folder: str,
        credentials: dict,
    ) -> RemoteFile:
        upload_url = f"{GRAPH_BASE}/me/drive/root:/{remote_folder}/{filename}:/content"
        mime_type, _ = mimetypes.guess_type(filename)
        mime_type = mime_type or "application/octet-stream"

        with open(file_path, "rb") as f:
            content = f.read()

        async with httpx.AsyncClient(timeout=120) as client:
            resp = await client.put(
                upload_url,
                headers={**self._headers(credentials), "Content-Type": mime_type},
                content=content,
            )
            resp.raise_for_status()
            data = resp.json()

        remote_id = data.get("id", "")
        remote_path = data.get("parentReference", {}).get("path", "") + f"/{filename}"
        share_url = data.get("webUrl", "")
        return RemoteFile(remote_id=remote_id, remote_path=remote_path, share_url=share_url)

    async def get_quota(self, credentials: dict) -> QuotaInfo:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.get(
                f"{GRAPH_BASE}/me/drive",
                headers=self._headers(credentials),
            )
            resp.raise_for_status()
            data = resp.json()

        quota = data.get("quota", {})
        total = int(quota.get("total", 0))
        used = int(quota.get("used", 0))
        free = int(quota.get("remaining", max(0, total - used)))
        return QuotaInfo(total_bytes=total, used_bytes=used, free_bytes=free)

    async def delete_file(self, remote_id: str, credentials: dict) -> bool:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.delete(
                f"{GRAPH_BASE}/me/drive/items/{remote_id}",
                headers=self._headers(credentials),
            )
            return resp.status_code in (200, 204)

    async def get_share_link(self, remote_id: str, credentials: dict) -> str:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                f"{GRAPH_BASE}/me/drive/items/{remote_id}/createLink",
                headers={**self._headers(credentials), "Content-Type": "application/json"},
                json={"type": "view", "scope": "anonymous"},
            )
            resp.raise_for_status()
            data = resp.json()
        return data.get("link", {}).get("webUrl", "")
