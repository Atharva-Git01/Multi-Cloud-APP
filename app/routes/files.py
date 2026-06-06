"""File upload, listing, move, delete, and share-link routes."""
from __future__ import annotations

import logging
import math
import mimetypes
import os
import tempfile
import uuid
from datetime import datetime, timezone
from typing import Optional

import aiofiles
from fastapi import (
    APIRouter,
    Depends,
    File,
    HTTPException,
    Query,
    UploadFile,
    status,
)
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.core.database import get_db
from app.core.security import decrypt_token, get_current_user
from app.models.cloud_account import CloudAccount
from app.models.file_record import FileRecord
from app.models.sync_job import SyncJob
from app.models.user import User
from app.schemas.file import FileListResponse, FileResponse, MoveFileRequest
from app.services.providers.base import BaseCloudProvider
from app.services.providers.box_provider import BoxProvider
from app.services.providers.dropbox_provider import DropboxProvider
from app.services.providers.google_drive import GoogleDriveProvider
from app.services.providers.mega_provider import MegaProvider
from app.services.providers.onedrive import OneDriveProvider
from app.services.providers.pcloud_provider import PCloudProvider
from app.services.routing_engine import NoStorageAvailableError, detect_category, route_file

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/files", tags=["files"])

PROVIDER_CLASS_MAP = {
    "google": GoogleDriveProvider,
    "onedrive": OneDriveProvider,
    "mega": MegaProvider,
    "box": BoxProvider,
    "pcloud": PCloudProvider,
    "dropbox": DropboxProvider,
}

BBG_REMOTE_FOLDER = "BBGCloud"


def _get_provider_instance(provider_name: str) -> BaseCloudProvider:
    cls = PROVIDER_CLASS_MAP.get(provider_name)
    if cls is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unknown provider: {provider_name}",
        )
    return cls()


async def _get_credentials(provider_name: str, user_id: str, db: AsyncSession) -> dict:
    result = await db.execute(
        select(CloudAccount).where(
            CloudAccount.user_id == user_id,
            CloudAccount.provider == provider_name,
            CloudAccount.is_active.is_(True),
        )
    )
    account: CloudAccount | None = result.scalar_one_or_none()
    if account is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Provider '{provider_name}' is not connected.",
        )
    creds: dict = {"account_email": account.account_email or ""}
    if account.access_token_enc:
        creds["access_token"] = decrypt_token(account.access_token_enc)
    if account.refresh_token_enc:
        creds["refresh_token"] = decrypt_token(account.refresh_token_enc)
    return creds


# ---------------------------------------------------------------------------
# Upload
# ---------------------------------------------------------------------------

@router.post("/upload", response_model=FileResponse, status_code=status.HTTP_201_CREATED)
async def upload_file(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Upload a file, auto-route to the best provider, and record in DB."""
    # Detect MIME type
    mime_type = file.content_type or mimetypes.guess_type(file.filename or "")[0] or "application/octet-stream"
    category = detect_category(mime_type)

    # Write to a temp file so we can pass a file path to provider SDKs
    suffix = os.path.splitext(file.filename or "upload")[1]
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp_path = tmp.name

    try:
        async with aiofiles.open(tmp_path, "wb") as f:
            while chunk := await file.read(1024 * 1024):  # 1 MB chunks
                await f.write(chunk)

        file_size = os.path.getsize(tmp_path)

        # Route
        try:
            provider_name = await route_file(
                file_size=file_size,
                mime_type=mime_type,
                user_id=str(current_user.id),
                db=db,
            )
        except NoStorageAvailableError as exc:
            raise HTTPException(
                status_code=status.HTTP_507_INSUFFICIENT_STORAGE,
                detail=str(exc),
            )

        # Get credentials and upload
        creds = await _get_credentials(provider_name, str(current_user.id), db)
        provider = _get_provider_instance(provider_name)

        remote_file = await provider.upload_file(
            file_path=tmp_path,
            filename=file.filename or "upload",
            remote_folder=BBG_REMOTE_FOLDER,
            credentials=creds,
        )

        # Persist file record
        file_record = FileRecord(
            user_id=current_user.id,
            original_name=file.filename or "upload",
            mime_type=mime_type,
            category=category,
            size_bytes=file_size,
        )
        db.add(file_record)
        await db.flush()
        await db.refresh(file_record)

        sync_job = SyncJob(
            file_id=file_record.id,
            provider=provider_name,
            status="done",
            remote_path=remote_file.remote_path,
            remote_id=remote_file.remote_id,
            share_url=remote_file.share_url,
            synced_at=datetime.now(timezone.utc),
        )
        db.add(sync_job)
        await db.flush()
        await db.refresh(file_record)

        return FileResponse.model_validate(file_record)

    finally:
        try:
            os.unlink(tmp_path)
        except OSError:
            pass


# ---------------------------------------------------------------------------
# List
# ---------------------------------------------------------------------------

@router.get("", response_model=FileListResponse)
async def list_files(
    category: Optional[str] = Query(None),
    provider: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Return a paginated list of the current user's files."""
    from sqlalchemy import func

    query = (
        select(FileRecord)
        .where(
            FileRecord.user_id == current_user.id,
            FileRecord.is_deleted.is_(False),
        )
    )
    if category:
        query = query.where(FileRecord.category == category)
    if provider:
        # Filter files that have at least one sync_job for this provider
        from sqlalchemy import exists
        query = query.where(
            exists().where(
                SyncJob.file_id == FileRecord.id,
                SyncJob.provider == provider,
                SyncJob.status == "done",
            )
        )

    count_query = select(func.count()).select_from(query.subquery())
    total_result = await db.execute(count_query)
    total: int = total_result.scalar_one()

    offset = (page - 1) * size
    result = await db.execute(
        query.order_by(FileRecord.uploaded_at.desc()).offset(offset).limit(size)
    )
    records = result.scalars().all()

    # Eagerly load sync_jobs for each record
    items = []
    for rec in records:
        sj_result = await db.execute(select(SyncJob).where(SyncJob.file_id == rec.id))
        rec.sync_jobs = sj_result.scalars().all()
        items.append(FileResponse.model_validate(rec))

    return FileListResponse(
        items=items,
        total=total,
        page=page,
        size=size,
        pages=math.ceil(total / size) if total else 1,
    )


# ---------------------------------------------------------------------------
# Move
# ---------------------------------------------------------------------------

@router.patch("/{file_id}/move", response_model=FileResponse)
async def move_file(
    file_id: str,
    body: MoveFileRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Move (re-upload) a file to a different provider."""
    result = await db.execute(
        select(FileRecord).where(
            FileRecord.id == file_id,
            FileRecord.user_id == current_user.id,
            FileRecord.is_deleted.is_(False),
        )
    )
    file_record: FileRecord | None = result.scalar_one_or_none()
    if file_record is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="File not found.")

    target = body.target_provider
    if target not in PROVIDER_CLASS_MAP:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unknown provider: {target}",
        )

    # Find existing sync job to get source remote info
    sj_result = await db.execute(
        select(SyncJob).where(SyncJob.file_id == file_record.id, SyncJob.status == "done")
    )
    existing_job: SyncJob | None = sj_result.scalars().first()

    # We can't re-download from the remote easily without the binary.
    # In a full implementation this would pull from source provider.
    # Here we create a new pending sync job on the target provider.
    new_job = SyncJob(
        file_id=file_record.id,
        provider=target,
        status="pending",
        retry_count=0,
    )
    db.add(new_job)
    await db.flush()

    sj_all = await db.execute(select(SyncJob).where(SyncJob.file_id == file_record.id))
    file_record.sync_jobs = sj_all.scalars().all()
    return FileResponse.model_validate(file_record)


# ---------------------------------------------------------------------------
# Delete (soft)
# ---------------------------------------------------------------------------

@router.delete("/{file_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_file(
    file_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(FileRecord).where(
            FileRecord.id == file_id,
            FileRecord.user_id == current_user.id,
        )
    )
    file_record: FileRecord | None = result.scalar_one_or_none()
    if file_record is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="File not found.")

    file_record.is_deleted = True
    return None


# ---------------------------------------------------------------------------
# Share link
# ---------------------------------------------------------------------------

@router.post("/{file_id}/share")
async def share_file(
    file_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Generate and return a public share link for the file."""
    result = await db.execute(
        select(FileRecord).where(
            FileRecord.id == file_id,
            FileRecord.user_id == current_user.id,
            FileRecord.is_deleted.is_(False),
        )
    )
    file_record: FileRecord | None = result.scalar_one_or_none()
    if file_record is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="File not found.")

    sj_result = await db.execute(
        select(SyncJob).where(SyncJob.file_id == file_record.id, SyncJob.status == "done")
    )
    job: SyncJob | None = sj_result.scalars().first()
    if job is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="File has not been synced to any provider yet.",
        )

    # Return cached share URL if available
    if job.share_url:
        return {"share_url": job.share_url}

    creds = await _get_credentials(job.provider, str(current_user.id), db)
    provider = _get_provider_instance(job.provider)
    share_url = await provider.get_share_link(job.remote_id, creds)

    job.share_url = share_url
    return {"share_url": share_url}
