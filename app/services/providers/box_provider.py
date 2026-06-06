"""Box cloud provider adapter."""
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

BOX_API = "https://api.box.com/2.0"
BOX_UPLOAD_API = "https://upload.box.com/api/2.0"
MAX_FILE_SIZE_BYTES = 250 * 1024 * 1024  # 250 MB


class BoxProvider(BaseCloudProvider):
    """Adapter for Box using the Box Content API v2 via httpx."""

    def _headers(self, credentials: dict) -> dict:
        return {"Authorization": f"Bearer {credentials['access_token']}"}

    # ---------------------------------------------------------------------------
    # BaseCloudProvider interface
    # ---------------------------------------------------------------------------

    async def create_account(self, email: str, password: str) -> ProviderAccount:
        raise NotImplementedError(
            "Box requires an OAuth 2.0 flow — use /api/providers/connect?provider=box"
        )

    async def _get_or_create_folder(self, client: httpx.AsyncClient, folder_name: str, headers: dict) -> str:
        """Find or create a top-level folder by name; return its ID."""
        # List root folder items
        resp = await client.get(
            f"{BOX_API}/folders/0/items",
            headers=headers,
            params={"fields": "id,name,type", "limit": 1000},
        )
        resp.raise_for_status()
        items = resp.json().get("entries", [])
        for item in items:
            if item["type"] == "folder" and item["name"] == folder_name:
                return item["id"]

        # Create folder
        resp = await client.post(
            f"{BOX_API}/folders",
            headers={**headers, "Content-Type": "application/json"},
            json={"name": folder_name, "parent": {"id": "0"}},
        )
        resp.raise_for_status()
        return resp.json()["id"]

    async def upload_file(
        self,
        file_path: str,
        filename: str,
        remote_folder: str,
        credentials: dict,
    ) -> RemoteFile:
        file_size = os.path.getsize(file_path)
        if file_size > MAX_FILE_SIZE_BYTES:
            raise ValueError(
                f"File size {file_size} bytes exceeds Box limit of {MAX_FILE_SIZE_BYTES} bytes."
            )

        headers = self._headers(credentials)
        mime_type, _ = mimetypes.guess_type(filename)
        mime_type = mime_type or "application/octet-stream"

        async with httpx.AsyncClient(timeout=120) as client:
            folder_id = await self._get_or_create_folder(client, remote_folder, headers)

            with open(file_path, "rb") as f:
                content = f.read()

            resp = await client.post(
                f"{BOX_UPLOAD_API}/files/content",
                headers=headers,
                files={"file": (filename, content, mime_type)},
                data={"attributes": f'{{"name":"{filename}","parent":{{"id":"{folder_id}"}}}}'},
            )
            resp.raise_for_status()
            data = resp.json()

        entry = data.get("entries", [{}])[0]
        file_id = entry.get("id", "")
        return RemoteFile(
            remote_id=file_id,
            remote_path=f"{remote_folder}/{filename}",
            share_url="",
        )

    async def get_quota(self, credentials: dict) -> QuotaInfo:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.get(
                f"{BOX_API}/users/me",
                headers=self._headers(credentials),
                params={"fields": "space_amount,space_used"},
            )
            resp.raise_for_status()
            data = resp.json()

        total = int(data.get("space_amount", 0))
        used = int(data.get("space_used", 0))
        free = max(0, total - used)
        return QuotaInfo(total_bytes=total, used_bytes=used, free_bytes=free)

    async def delete_file(self, remote_id: str, credentials: dict) -> bool:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.delete(
                f"{BOX_API}/files/{remote_id}",
                headers=self._headers(credentials),
            )
            return resp.status_code in (200, 204)

    async def get_share_link(self, remote_id: str, credentials: dict) -> str:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.put(
                f"{BOX_API}/files/{remote_id}",
                headers={**self._headers(credentials), "Content-Type": "application/json"},
                json={"shared_link": {"access": "open"}},
            )
            resp.raise_for_status()
            data = resp.json()
        return data.get("shared_link", {}).get("url", "")
