"""Intelligent file routing engine.

Routes uploaded files to the best cloud provider based on:
1. File category (primary preference mapping)
2. Available quota (fallback when primary provider is >85% full)
3. Provider-specific file size limits
"""
from __future__ import annotations

import logging
from typing import Dict, Optional

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.models.provider_quota import ProviderQuota
from app.services.providers.base import QuotaInfo

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

CATEGORY_ROUTING: Dict[str, str] = {
    "images": "google",
    "documents": "onedrive",
    "videos": "mega",
    "audio": "pcloud",
    "archives": "box",
    "other": "dropbox",
}

# Providers that have hard per-file size limits (bytes)
PROVIDER_FILE_LIMITS: Dict[str, int] = {
    "box": 250 * 1024 * 1024,  # 250 MB
}

# Fallback order when the primary provider is unavailable
FALLBACK_ORDER = ["google", "onedrive", "dropbox", "pcloud", "box", "mega"]

FULL_THRESHOLD = 0.85  # treat as full when used/total > 85%


class NoStorageAvailableError(Exception):
    """Raised when no provider has sufficient free space for the file."""


# ---------------------------------------------------------------------------
# MIME → category detection
# ---------------------------------------------------------------------------

IMAGE_MIMES = {"image"}
VIDEO_MIMES = {"video"}
AUDIO_MIMES = {"audio"}

DOCUMENT_MIMES = {
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "text/plain",
    "text/csv",
    "application/rtf",
}

ARCHIVE_MIMES = {
    "application/zip",
    "application/x-zip-compressed",
    "application/x-rar-compressed",
    "application/vnd.rar",
    "application/x-7z-compressed",
    "application/x-tar",
    "application/gzip",
    "application/x-bzip2",
}


def detect_category(mime_type: str) -> str:
    """Map a MIME type string to one of the canonical file categories."""
    if not mime_type:
        return "other"

    main_type = mime_type.split("/")[0].lower()
    if main_type in IMAGE_MIMES:
        return "images"
    if main_type in VIDEO_MIMES:
        return "videos"
    if main_type in AUDIO_MIMES:
        return "audio"
    if mime_type in DOCUMENT_MIMES:
        return "documents"
    if mime_type in ARCHIVE_MIMES:
        return "archives"
    return "other"


# ---------------------------------------------------------------------------
# Quota helpers
# ---------------------------------------------------------------------------

async def fetch_all_quotas(user_id: str, db: AsyncSession) -> Dict[str, QuotaInfo]:
    """Load cached provider quotas from the database for a user."""
    result = await db.execute(
        select(ProviderQuota).where(ProviderQuota.user_id == user_id)
    )
    rows = result.scalars().all()
    return {
        row.provider: QuotaInfo(
            total_bytes=row.total_bytes,
            used_bytes=row.used_bytes,
            free_bytes=row.free_bytes,
        )
        for row in rows
    }


def _provider_is_full(quota: QuotaInfo) -> bool:
    if quota.total_bytes == 0:
        return True
    return (quota.used_bytes / quota.total_bytes) > FULL_THRESHOLD


def _provider_can_accept(provider: str, file_size: int, quota: QuotaInfo) -> bool:
    """Return True if the provider can physically store the file."""
    # Check per-file size limit
    limit = PROVIDER_FILE_LIMITS.get(provider)
    if limit is not None and file_size > limit:
        return False
    # Check available space
    if quota.free_bytes < file_size:
        return False
    return True


# ---------------------------------------------------------------------------
# Main routing function
# ---------------------------------------------------------------------------

async def route_file(
    file_size: int,
    mime_type: str,
    user_id: str,
    db: AsyncSession,
) -> str:
    """Determine the best provider for a file upload.

    Algorithm:
    1. Detect category and choose the primary preferred provider.
    2. If the primary provider is >85% full or cannot accept the file, fall back
       to the next provider in FALLBACK_ORDER that has sufficient space.
    3. Raise NoStorageAvailableError if no provider can accept the file.

    Returns the provider name string (e.g. "google", "mega", …).
    """
    category = detect_category(mime_type)
    primary_provider = CATEGORY_ROUTING.get(category, "dropbox")
    quotas = await fetch_all_quotas(user_id, db)

    # Build the ordered list to try: primary first, then fallbacks
    ordered = [primary_provider] + [p for p in FALLBACK_ORDER if p != primary_provider]

    for provider in ordered:
        quota = quotas.get(provider)
        if quota is None:
            # Provider not connected — skip
            logger.debug("Skipping %s: no quota info (not connected?)", provider)
            continue
        if _provider_is_full(quota):
            logger.debug("Skipping %s: quota >85%% full", provider)
            continue
        if not _provider_can_accept(provider, file_size, quota):
            logger.debug("Skipping %s: cannot accept file of size %d", provider, file_size)
            continue
        logger.info("Routing file (category=%s) to provider=%s", category, provider)
        return provider

    raise NoStorageAvailableError(
        "No connected cloud provider has sufficient free space for this file."
    )
