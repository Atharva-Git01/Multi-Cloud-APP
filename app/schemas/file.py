from datetime import datetime
from typing import List, Optional
from uuid import UUID

from pydantic import BaseModel


class SyncJobResponse(BaseModel):
    id: UUID
    provider: str
    status: str
    remote_path: Optional[str]
    remote_id: Optional[str]
    share_url: Optional[str]
    synced_at: Optional[datetime]
    retry_count: int
    error_msg: Optional[str]

    model_config = {"from_attributes": True}


class FileResponse(BaseModel):
    id: UUID
    original_name: str
    mime_type: Optional[str]
    category: str
    size_bytes: int
    uploaded_at: datetime
    is_deleted: bool
    sync_jobs: List[SyncJobResponse] = []

    model_config = {"from_attributes": True}


class FileListResponse(BaseModel):
    items: List[FileResponse]
    total: int
    page: int
    size: int
    pages: int


class MoveFileRequest(BaseModel):
    target_provider: str
