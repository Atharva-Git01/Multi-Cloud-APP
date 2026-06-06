"""MEGA cloud provider adapter — uses the mega.py library."""
from __future__ import annotations

import asyncio
import functools
from typing import Any

from app.services.providers.base import (
    BaseCloudProvider,
    ProviderAccount,
    QuotaInfo,
    RemoteFile,
)

FREE_TIER_BYTES = 20 * 1024 * 1024 * 1024  # 20 GB


def _run_sync(fn, *args, **kwargs):
    """Run a synchronous blocking call in the default executor."""
    loop = asyncio.get_event_loop()
    return loop.run_in_executor(None, functools.partial(fn, *args, **kwargs))


class MegaProvider(BaseCloudProvider):
    """Adapter for MEGA using the mega.py library (synchronous, run in executor)."""

    def _login(self, credentials: dict):
        from mega import Mega  # imported lazily so tests can mock

        m = Mega()
        email = credentials.get("account_email") or credentials.get("email")
        password = credentials.get("password") or credentials.get("access_token")
        return m.login(email, password)

    # ---------------------------------------------------------------------------
    # BaseCloudProvider interface
    # ---------------------------------------------------------------------------

    async def create_account(self, email: str, password: str) -> ProviderAccount:
        """Register a new MEGA account (API-level registration)."""

        def _register():
            from mega import Mega

            m = Mega()
            m.register(email, password)
            return m.login(email, password)

        try:
            mega = await _run_sync(_register)
            account_info = mega.get_user()
            return ProviderAccount(
                provider="mega",
                account_email=email,
                access_token=password,  # MEGA uses email+password auth
                refresh_token="",
            )
        except Exception as exc:
            raise RuntimeError(f"MEGA account creation failed: {exc}") from exc

    async def upload_file(
        self,
        file_path: str,
        filename: str,
        remote_folder: str,
        credentials: dict,
    ) -> RemoteFile:
        def _upload():
            mega = self._login(credentials)
            folder = mega.find(remote_folder)
            if folder is None:
                folder = mega.create_folder(remote_folder)
                # create_folder returns a dict; get the folder node
                folder_key = list(folder.keys())[0] if folder else None
            else:
                folder_key = list(folder.keys())[0] if isinstance(folder, dict) else folder

            uploaded = mega.upload(file_path, dest=folder_key, dest_filename=filename)
            link = mega.get_upload_link(uploaded)
            file_node = list(uploaded.keys())[0]
            return file_node, link

        file_node, link = await _run_sync(_upload)
        return RemoteFile(
            remote_id=str(file_node),
            remote_path=f"{remote_folder}/{filename}",
            share_url=link or "",
        )

    async def get_quota(self, credentials: dict) -> QuotaInfo:
        def _quota():
            mega = self._login(credentials)
            return mega.get_quota()

        try:
            quota = await _run_sync(_quota)
            # mega.py returns quota in bytes (total used space)
            used = int(quota) if quota else 0
            total = FREE_TIER_BYTES
            free = max(0, total - used)
            return QuotaInfo(total_bytes=total, used_bytes=used, free_bytes=free)
        except Exception:
            return QuotaInfo(
                total_bytes=FREE_TIER_BYTES,
                used_bytes=0,
                free_bytes=FREE_TIER_BYTES,
            )

    async def delete_file(self, remote_id: str, credentials: dict) -> bool:
        def _delete():
            mega = self._login(credentials)
            mega.delete(remote_id)

        try:
            await _run_sync(_delete)
            return True
        except Exception:
            return False

    async def get_share_link(self, remote_id: str, credentials: dict) -> str:
        def _link():
            mega = self._login(credentials)
            return mega.get_link({"h": remote_id})

        try:
            link = await _run_sync(_link)
            return link or ""
        except Exception:
            return ""
