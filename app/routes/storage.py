"""Storage dashboard and quota refresh routes."""
from __future__ import annotations

import logging
from typing import Dict

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.core.database import get_db
from app.core.security import get_current_user
from app.models.provider_quota import ProviderQuota
from app.models.user import User
from app.schemas.storage import DashboardResponse, ProviderQuotaResponse
from app.services.quota_fetcher import refresh_quotas_for_user

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/storage", tags=["storage"])


@router.get("/dashboard", response_model=DashboardResponse)
async def dashboard(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Return quota stats for all connected providers + aggregate totals."""
    result = await db.execute(
        select(ProviderQuota).where(ProviderQuota.user_id == current_user.id)
    )
    rows = result.scalars().all()

    providers: Dict[str, ProviderQuotaResponse] = {}
    grand_total = grand_used = grand_free = 0

    for row in rows:
        used_pct = (row.used_bytes / row.total_bytes * 100) if row.total_bytes else 0.0
        providers[row.provider] = ProviderQuotaResponse(
            provider=row.provider,
            total_bytes=row.total_bytes,
            used_bytes=row.used_bytes,
            free_bytes=row.free_bytes,
            used_percent=round(used_pct, 2),
            last_checked_at=row.last_checked_at,
        )
        grand_total += row.total_bytes
        grand_used += row.used_bytes
        grand_free += row.free_bytes

    overall_pct = (grand_used / grand_total * 100) if grand_total else 0.0

    return DashboardResponse(
        providers=providers,
        grand_total_bytes=grand_total,
        grand_used_bytes=grand_used,
        grand_free_bytes=grand_free,
        overall_used_percent=round(overall_pct, 2),
    )


@router.post("/refresh-quotas", status_code=status.HTTP_200_OK)
async def refresh_quotas(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Force a live quota refresh from all connected providers."""
    quota_map = await refresh_quotas_for_user(str(current_user.id), db)
    return {
        "refreshed": list(quota_map.keys()),
        "message": f"Quota refreshed for {len(quota_map)} provider(s).",
    }
