from datetime import datetime
from typing import List, Optional

from pydantic import BaseModel, field_serializer


class ProviderQuotaResponse(BaseModel):
    provider: str
    used_gb: float
    total_gb: float
    free_gb: float
    percent_used: float
    last_checked_at: Optional[int] = None  # Unix timestamp millis for Android

    model_config = {"from_attributes": True}


class DashboardResponse(BaseModel):
    quotas: List[ProviderQuotaResponse]
    total_free_gb: float
    total_used_gb: float
    total_gb: float
