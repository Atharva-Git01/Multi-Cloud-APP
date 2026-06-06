import uuid
from datetime import datetime, timezone

from sqlalchemy import BigInteger, Column, DateTime, ForeignKey, String
from sqlalchemy import Uuid as UUID
from sqlalchemy.orm import relationship

from app.core.database import Base


class ProviderQuota(Base):
    __tablename__ = "provider_quotas"

    id = Column(UUID(native_uuid=False), primary_key=True, default=uuid.uuid4, index=True)
    user_id = Column(UUID(native_uuid=False), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    provider = Column(String, nullable=False)
    total_bytes = Column(BigInteger, default=0)
    used_bytes = Column(BigInteger, default=0)
    free_bytes = Column(BigInteger, default=0)
    last_checked_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))

    user = relationship("User", backref="provider_quotas")
