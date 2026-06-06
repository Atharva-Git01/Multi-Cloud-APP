"""Provider management routes — connect, disconnect, list, per-provider files."""
from __future__ import annotations

import logging
from datetime import datetime, timezone
from typing import List

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.core.config import settings
from app.core.database import get_db
from app.core.security import encrypt_token, get_current_user
from app.models.cloud_account import CloudAccount
from app.models.sync_job import SyncJob
from app.models.user import User

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/providers", tags=["providers"])

VALID_PROVIDERS = {"google", "onedrive", "mega", "box", "pcloud", "dropbox"}

# OAuth authorization URLs per provider
OAUTH_URLS = {
    "google": (
        "https://accounts.google.com/o/oauth2/v2/auth"
        "?response_type=code&scope=openid%20email%20profile%20https://www.googleapis.com/auth/drive"
        "&access_type=offline&prompt=consent"
    ),
    "onedrive": (
        "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"
        "?response_type=code&scope=Files.ReadWrite.All%20offline_access"
    ),
    "box": "https://account.box.com/api/oauth2/authorize?response_type=code",
    "dropbox": "https://www.dropbox.com/oauth2/authorize?response_type=code&token_access_type=offline",
}


@router.get("", summary="List connected providers")
async def list_providers(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(CloudAccount).where(
            CloudAccount.user_id == current_user.id,
            CloudAccount.is_active.is_(True),
        )
    )
    accounts = result.scalars().all()
    return [
        {
            "provider": acc.provider,
            "account_email": acc.account_email,
            "connected_at": acc.connected_at,
            "is_active": acc.is_active,
        }
        for acc in accounts
    ]


@router.post("/connect", summary="Start OAuth for additional provider")
async def connect_provider(
    provider: str = Query(..., description="Provider name (google|onedrive|box|dropbox)"),
    current_user: User = Depends(get_current_user),
):
    if provider not in VALID_PROVIDERS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unknown provider: {provider}. Valid: {sorted(VALID_PROVIDERS)}",
        )

    oauth_base = OAUTH_URLS.get(provider)
    if oauth_base is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Provider '{provider}' does not support OAuth from this endpoint.",
        )

    # Append client_id dynamically based on provider
    client_id_map = {
        "google": settings.GOOGLE_CLIENT_ID,
        "onedrive": settings.ONEDRIVE_CLIENT_ID,
        "box": settings.BOX_CLIENT_ID,
        "dropbox": settings.DROPBOX_APP_KEY,
    }
    client_id = client_id_map.get(provider, "")
    redirect_map = {
        "google": settings.GOOGLE_REDIRECT_URI,
        "onedrive": settings.ONEDRIVE_REDIRECT_URI,
        "box": "",
        "dropbox": "",
    }
    redirect_uri = redirect_map.get(provider, "")

    auth_url = f"{oauth_base}&client_id={client_id}"
    if redirect_uri:
        auth_url += f"&redirect_uri={redirect_uri}"

    return {
        "provider": provider,
        "auth_url": auth_url,
        "message": "Redirect the user to auth_url to complete the OAuth flow.",
    }


@router.delete("/{provider_name}", status_code=status.HTTP_204_NO_CONTENT)
async def disconnect_provider(
    provider_name: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(CloudAccount).where(
            CloudAccount.user_id == current_user.id,
            CloudAccount.provider == provider_name,
            CloudAccount.is_active.is_(True),
        )
    )
    account: CloudAccount | None = result.scalar_one_or_none()
    if account is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Provider '{provider_name}' is not connected.",
        )

    account.is_active = False
    return None


@router.get("/{provider_name}/files", summary="Files on a specific provider")
async def provider_files(
    provider_name: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Return all sync jobs (i.e. files) stored on the given provider for this user."""
    from app.models.file_record import FileRecord

    result = await db.execute(
        select(SyncJob, FileRecord)
        .join(FileRecord, SyncJob.file_id == FileRecord.id)
        .where(
            FileRecord.user_id == current_user.id,
            SyncJob.provider == provider_name,
            SyncJob.status == "done",
            FileRecord.is_deleted.is_(False),
        )
    )
    rows = result.all()

    return [
        {
            "file_id": str(fr.id),
            "original_name": fr.original_name,
            "mime_type": fr.mime_type,
            "category": fr.category,
            "size_bytes": fr.size_bytes,
            "uploaded_at": fr.uploaded_at,
            "remote_id": sj.remote_id,
            "remote_path": sj.remote_path,
            "share_url": sj.share_url,
        }
        for sj, fr in rows
    ]
