import uuid
from datetime import datetime, timezone

from sqlalchemy import Column, DateTime, ForeignKey, Integer, String
from sqlalchemy import Uuid as UUID
from sqlalchemy.orm import relationship

from app.core.database import Base

VALID_STATUSES = ("pending", "uploading", "done", "failed")


class SyncJob(Base):
    __tablename__ = "sync_jobs"

    id = Column(UUID(native_uuid=False), primary_key=True, default=uuid.uuid4, index=True)
    file_id = Column(UUID(native_uuid=False), ForeignKey("file_records.id", ondelete="CASCADE"), nullable=False, index=True)
    provider = Column(String, nullable=False)
    status = Column(String, nullable=False, default="pending")   # one of VALID_STATUSES
    remote_path = Column(String, nullable=True)
    remote_id = Column(String, nullable=True)
    share_url = Column(String, nullable=True)
    synced_at = Column(DateTime(timezone=True), nullable=True)
    retry_count = Column(Integer, default=0)
    error_msg = Column(String, nullable=True)

    file_record = relationship("FileRecord", back_populates="sync_jobs")
