"""Dropbox cloud provider adapter — Dropbox API v2 via httpx."""
from __future__ import annotations

import json
import mimetypes

import httpx

from app.services.providers.base import (
    BaseCloudProvider,
    ProviderAccount,
    QuotaInfo,
    RemoteFile,
)

CONTENT_API = "https://content.dropboxapi.com/2"
API = "https://api.dropboxapi.com/2"


class DropboxProvider(BaseCloudProvider):
    """Adapter for Dropbox using API v2."""

    def _auth_headers(self, credentials: dict) -> dict:
        return {"Authorization": f"Bearer {credentials['access_token']}"}

    # ---------------------------------------------------------------------------
    # BaseCloudProvider interface
    # ---------------------------------------------------------------------------

    async def create_account(self, email: str, password: str) -> ProviderAccount:
        raise NotImplementedError(
            "Dropbox requires an OAuth 2.0 flow — use /api/providers/connect?provider=dropbox"
        )

    async def upload_file(
        self,
        file_path: str,
        filename: str,
        remote_folder: str,
        credentials: dict,
    ) -> RemoteFile:
        dropbox_path = f"/{remote_folder}/{filename}"

        with open(file_path, "rb") as f:
            content = f.read()

        dropbox_api_arg = json.dumps(
            {
                "path": dropbox_path,
                "mode": "add",
                "autorename": True,
                "mute": False,
            }
        )

        async with httpx.AsyncClient(timeout=300) as client:
            resp = await client.post(
                f"{CONTENT_API}/files/upload",
                headers={
                    **self._auth_headers(credentials),
                    "Dropbox-API-Arg": dropbox_api_arg,
                    "Content-Type": "application/octet-stream",
                },
                content=content,
            )
            resp.raise_for_status()
            data = resp.json()

        file_id = data.get("id", "")
        actual_path = data.get("path_display", dropbox_path)
        return RemoteFile(remote_id=file_id, remote_path=actual_path, share_url="")

    async def get_quota(self, credentials: dict) -> QuotaInfo:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                f"{API}/users/get_space_usage",
                headers={**self._auth_headers(credentials), "Content-Type": "application/json"},
                content=b"null",
            )
            resp.raise_for_status()
            data = resp.json()

        used = int(data.get("used", 0))
        allocation = data.get("allocation", {})
        total = int(allocation.get("allocated", 0))
        free = max(0, total - used)
        return QuotaInfo(total_bytes=total, used_bytes=used, free_bytes=free)

    async def delete_file(self, remote_id: str, credentials: dict) -> bool:
        """Delete using path stored in remote_id (Dropbox IDs start with 'id:...')."""
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                f"{API}/files/delete_v2",
                headers={**self._auth_headers(credentials), "Content-Type": "application/json"},
                json={"path": remote_id},
            )
            return resp.status_code == 200

    async def get_share_link(self, remote_id: str, credentials: dict) -> str:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                f"{API}/sharing/create_shared_link_with_settings",
                headers={**self._auth_headers(credentials), "Content-Type": "application/json"},
                json={"path": remote_id, "settings": {"requested_visibility": "public"}},
            )
            data = resp.json()

        # If already shared, re-fetch existing link
        if resp.status_code == 409 and "shared_link_already_exists" in str(data):
            existing = data.get("error", {}).get("shared_link_already_exists", {}).get("metadata", {})
            return existing.get("url", "")

        if resp.status_code != 200:
            return ""
        return data.get("url", "")
