from datetime import datetime
from typing import Dict, Optional

from pydantic import BaseModel


class ProviderQuotaResponse(BaseModel):
    provider: str
    total_bytes: int
    used_bytes: int
    free_bytes: int
    used_percent: float
    last_checked_at: Optional[datetime]

    model_config = {"from_attributes": True}


class DashboardResponse(BaseModel):
    providers: Dict[str, ProviderQuotaResponse]
    grand_total_bytes: int
    grand_used_bytes: int
    grand_free_bytes: int
    overall_used_percent: float
