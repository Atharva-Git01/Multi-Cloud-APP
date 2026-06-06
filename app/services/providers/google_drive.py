"""Google Drive cloud provider adapter."""
from __future__ import annotations

import io
import os
from typing import Any

from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload, MediaIoBaseUpload

from app.services.providers.base import (
    BaseCloudProvider,
    ProviderAccount,
    QuotaInfo,
    RemoteFile,
)


class GoogleDriveProvider(BaseCloudProvider):
    """Adapter for Google Drive using the official google-api-python-client."""

    # ---------------------------------------------------------------------------
    # Helpers
    # ---------------------------------------------------------------------------

    def _build_service(self, credentials: dict) -> Any:
        creds = Credentials(
            token=credentials.get("access_token"),
            refresh_token=credentials.get("refresh_token"),
            token_uri="https://oauth2.googleapis.com/token",
            client_id=credentials.get("client_id", ""),
            client_secret=credentials.get("client_secret", ""),
        )
        return build("drive", "v3", credentials=creds, cache_discovery=False)

    def _get_or_create_folder(self, service: Any, folder_name: str) -> str:
        """Return the ID of a top-level Drive folder, creating it if needed."""
        query = (
            f"name='{folder_name}' and mimeType='application/vnd.google-apps.folder'"
            " and trashed=false"
        )
        results = service.files().list(q=query, fields="files(id, name)").execute()
        files = results.get("files", [])
        if files:
            return files[0]["id"]

        folder_meta = {
            "name": folder_name,
            "mimeType": "application/vnd.google-apps.folder",
        }
        folder = service.files().create(body=folder_meta, fields="id").execute()
        return folder["id"]

    # ---------------------------------------------------------------------------
    # BaseCloudProvider interface
    # ---------------------------------------------------------------------------

    async def create_account(self, email: str, password: str) -> ProviderAccount:
        raise NotImplementedError(
            "Google Drive uses an OAuth 2.0 flow — call /api/auth/google/callback instead."
        )

    async def upload_file(
        self,
        file_path: str,
        filename: str,
        remote_folder: str,
        credentials: dict,
    ) -> RemoteFile:
        service = self._build_service(credentials)
        folder_id = self._get_or_create_folder(service, remote_folder)

        file_meta = {"name": filename, "parents": [folder_id]}
        media = MediaFileUpload(file_path, resumable=True)
        uploaded = (
            service.files()
            .create(body=file_meta, media_body=media, fields="id, webViewLink, name")
            .execute()
        )
        file_id = uploaded["id"]
        web_link = uploaded.get("webViewLink", "")
        remote_path = f"{remote_folder}/{filename}"
        return RemoteFile(remote_id=file_id, remote_path=remote_path, share_url=web_link)

    async def get_quota(self, credentials: dict) -> QuotaInfo:
        service = self._build_service(credentials)
        about = (
            service.about()
            .get(fields="storageQuota")
            .execute()
        )
        quota = about.get("storageQuota", {})
        total = int(quota.get("limit", 0))
        used = int(quota.get("usage", 0))
        free = max(0, total - used)
        return QuotaInfo(total_bytes=total, used_bytes=used, free_bytes=free)

    async def delete_file(self, remote_id: str, credentials: dict) -> bool:
        service = self._build_service(credentials)
        try:
            service.files().delete(fileId=remote_id).execute()
            return True
        except Exception:
            return False

    async def get_share_link(self, remote_id: str, credentials: dict) -> str:
        service = self._build_service(credentials)
        # Make the file publicly readable
        service.permissions().create(
            fileId=remote_id,
            body={"role": "reader", "type": "anyone"},
        ).execute()
        file_info = (
            service.files()
            .get(fileId=remote_id, fields="webViewLink")
            .execute()
        )
        return file_info.get("webViewLink", "")
