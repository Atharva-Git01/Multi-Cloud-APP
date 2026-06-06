"""Quota fetcher — refreshes quota info from all connected providers for a user."""
from __future__ import annotations

import logging
from datetime import datetime, timezone
from typing import Dict

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.core.security import decrypt_token
from app.models.cloud_account import CloudAccount
from app.models.provider_quota import ProviderQuota
from app.services.providers.base import QuotaInfo
from app.services.providers.box_provider import BoxProvider
from app.services.providers.dropbox_provider import DropboxProvider
from app.services.providers.google_drive import GoogleDriveProvider
from app.services.providers.mega_provider import MegaProvider
from app.services.providers.onedrive import OneDriveProvider
from app.services.providers.pcloud_provider import PCloudProvider

logger = logging.getLogger(__name__)

PROVIDER_CLASSES = {
    "google": GoogleDriveProvider,
    "onedrive": OneDriveProvider,
    "mega": MegaProvider,
    "box": BoxProvider,
    "pcloud": PCloudProvider,
    "dropbox": DropboxProvider,
}


def _build_credentials(account: CloudAccount) -> dict:
    creds: dict = {
        "account_email": account.account_email or "",
    }
    if account.access_token_enc:
        try:
            creds["access_token"] = decrypt_token(account.access_token_enc)
        except Exception:
            creds["access_token"] = ""
    if account.refresh_token_enc:
        try:
            creds["refresh_token"] = decrypt_token(account.refresh_token_enc)
        except Exception:
            creds["refresh_token"] = ""
    return creds


async def refresh_quotas_for_user(
    user_id: str, db: AsyncSession
) -> Dict[str, QuotaInfo]:
    """Fetch live quota from every connected provider and persist to DB.

    Returns a mapping of provider name → QuotaInfo.
    """
    result = await db.execute(
        select(CloudAccount).where(
            CloudAccount.user_id == user_id,
            CloudAccount.is_active.is_(True),
        )
    )
    accounts = result.scalars().all()

    quota_map: Dict[str, QuotaInfo] = {}

    for account in accounts:
        provider_name = account.provider
        provider_cls = PROVIDER_CLASSES.get(provider_name)
        if provider_cls is None:
            logger.warning("Unknown provider '%s' — skipping quota fetch", provider_name)
            continue

        credentials = _build_credentials(account)
        provider = provider_cls()

        try:
            quota = await provider.get_quota(credentials)
        except Exception as exc:
            logger.error(
                "Failed to fetch quota for provider=%s user=%s: %s",
                provider_name, user_id, exc,
            )
            continue

        quota_map[provider_name] = quota

        # Upsert into provider_quotas table
        existing = await db.execute(
            select(ProviderQuota).where(
                ProviderQuota.user_id == user_id,
                ProviderQuota.provider == provider_name,
            )
        )
        row: ProviderQuota | None = existing.scalar_one_or_none()

        now = datetime.now(timezone.utc)
        if row is None:
            row = ProviderQuota(
                user_id=user_id,
                provider=provider_name,
                total_bytes=quota.total_bytes,
                used_bytes=quota.used_bytes,
                free_bytes=quota.free_bytes,
                last_checked_at=now,
            )
            db.add(row)
        else:
            row.total_bytes = quota.total_bytes
            row.used_bytes = quota.used_bytes
            row.free_bytes = quota.free_bytes
            row.last_checked_at = now

    await db.flush()
    return quota_map
