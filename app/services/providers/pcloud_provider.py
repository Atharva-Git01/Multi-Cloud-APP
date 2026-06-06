"""pCloud cloud provider adapter — REST API via httpx."""
from __future__ import annotations

import mimetypes
import os

import httpx

from app.services.providers.base import (
    BaseCloudProvider,
    ProviderAccount,
    QuotaInfo,
    RemoteFile,
)

BASE_URL = "https://api.pcloud.com"


class PCloudProvider(BaseCloudProvider):
    """Adapter for pCloud using their REST API."""

    def _auth_params(self, credentials: dict) -> dict:
        return {"access_token": credentials["access_token"]}

    # ---------------------------------------------------------------------------
    # BaseCloudProvider interface
    # ---------------------------------------------------------------------------

    async def create_account(self, email: str, password: str) -> ProviderAccount:
        """Register a new pCloud account — this API is publicly documented and works."""
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                f"{BASE_URL}/userregister",
                data={
                    "mail": email,
                    "password": password,
                    "os": 4,  # 4 = other/web
                    "invite": "",
                    "tos": "yes",
                    "agreed": "yes",
                },
            )
            resp.raise_for_status()
            data = resp.json()

        if data.get("result", 1) != 0:
            raise RuntimeError(f"pCloud registration error: {data.get('error', 'unknown')}")

        # Immediately get an access token via digest auth
        token = data.get("token") or data.get("auth", "")
        return ProviderAccount(
            provider="pcloud",
            account_email=email,
            access_token=token,
            refresh_token="",
        )

    async def upload_file(
        self,
        file_path: str,
        filename: str,
        remote_folder: str,
        credentials: dict,
    ) -> RemoteFile:
        mime_type, _ = mimetypes.guess_type(filename)
        mime_type = mime_type or "application/octet-stream"

        params = {**self._auth_params(credentials), "path": f"/{remote_folder}", "nopartial": 1}

        with open(file_path, "rb") as f:
            content = f.read()

        async with httpx.AsyncClient(timeout=300) as client:
            resp = await client.post(
                f"{BASE_URL}/uploadfile",
                params=params,
                files={"file": (filename, content, mime_type)},
            )
            resp.raise_for_status()
            data = resp.json()

        if data.get("result", 1) != 0:
            raise RuntimeError(f"pCloud upload error: {data.get('error', 'unknown')}")

        metadata = data.get("metadata", [{}])[0]
        file_id = str(metadata.get("fileid", ""))
        return RemoteFile(
            remote_id=file_id,
            remote_path=f"/{remote_folder}/{filename}",
            share_url="",
        )

    async def get_quota(self, credentials: dict) -> QuotaInfo:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.get(
                f"{BASE_URL}/userinfo",
                params=self._auth_params(credentials),
            )
            resp.raise_for_status()
            data = resp.json()

        total = int(data.get("quota", 0))
        used = int(data.get("usedquota", 0))
        free = max(0, total - used)
        return QuotaInfo(total_bytes=total, used_bytes=used, free_bytes=free)

    async def delete_file(self, remote_id: str, credentials: dict) -> bool:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.get(
                f"{BASE_URL}/deletefile",
                params={**self._auth_params(credentials), "fileid": remote_id},
            )
            data = resp.json()
        return data.get("result", 1) == 0

    async def get_share_link(self, remote_id: str, credentials: dict) -> str:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.get(
                f"{BASE_URL}/getfilepublink",
                params={**self._auth_params(credentials), "fileid": remote_id},
            )
            data = resp.json()

        if data.get("result", 1) != 0:
            return ""
        code = data.get("code", "")
        return f"https://u.pcloud.link/publink/show?code={code}" if code else ""
