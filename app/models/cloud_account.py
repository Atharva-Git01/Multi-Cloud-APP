import uuid
from datetime import datetime, timezone

from sqlalchemy import Boolean, Column, DateTime, ForeignKey, String
from sqlalchemy import Uuid as UUID
from sqlalchemy.orm import relationship

from app.core.database import Base

VALID_PROVIDERS = ("google", "onedrive", "mega", "box", "pcloud", "dropbox")


class CloudAccount(Base):
    __tablename__ = "cloud_accounts"

    id = Column(UUID(native_uuid=False), primary_key=True, default=uuid.uuid4, index=True)
    user_id = Column(UUID(native_uuid=False), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    provider = Column(String, nullable=False)           # one of VALID_PROVIDERS
    account_email = Column(String, nullable=True)
    access_token_enc = Column(String, nullable=True)   # AES-256 encrypted
    refresh_token_enc = Column(String, nullable=True)  # AES-256 encrypted
    token_expiry = Column(DateTime(timezone=True), nullable=True)
    connected_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    is_active = Column(Boolean, default=True)

    user = relationship("User", backref="cloud_accounts")
