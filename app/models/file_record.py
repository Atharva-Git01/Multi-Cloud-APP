import uuid
from datetime import datetime, timezone

from sqlalchemy import BigInteger, Boolean, Column, DateTime, ForeignKey, String
from sqlalchemy import Uuid as UUID
from sqlalchemy.orm import relationship

from app.core.database import Base

VALID_CATEGORIES = ("images", "videos", "audio", "documents", "archives", "other")


class FileRecord(Base):
    __tablename__ = "file_records"

    id = Column(UUID(native_uuid=False), primary_key=True, default=uuid.uuid4, index=True)
    user_id = Column(UUID(native_uuid=False), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    original_name = Column(String, nullable=False)
    mime_type = Column(String, nullable=True)
    category = Column(String, nullable=False, default="other")  # one of VALID_CATEGORIES
    size_bytes = Column(BigInteger, default=0)
    uploaded_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    is_deleted = Column(Boolean, default=False)

    user = relationship("User", backref="file_records")
    sync_jobs = relationship("SyncJob", back_populates="file_record", cascade="all, delete-orphan")
