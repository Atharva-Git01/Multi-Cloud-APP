"""Routing rules — view and update per-user provider preferences."""
from __future__ import annotations

import json
import logging

import redis.asyncio as aioredis
from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel

from app.core.config import settings
from app.core.security import get_current_user
from app.models.user import User
from app.services.routing_engine import CATEGORY_ROUTING

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/routing", tags=["routing"])

# Default category→provider map (server-level)
DEFAULT_RULES = dict(CATEGORY_ROUTING)

VALID_CATEGORIES = set(DEFAULT_RULES.keys())
VALID_PROVIDERS = {"google", "onedrive", "mega", "box", "pcloud", "dropbox"}


class RoutingRulesRequest(BaseModel):
    rules: dict  # {category: provider}


def _redis_key(user_id: str) -> str:
    return f"bbg:routing:{user_id}"


async def _get_redis():
    return aioredis.from_url(settings.REDIS_URL, decode_responses=True)


@router.get("/rules", summary="Get current routing rules")
async def get_rules(current_user: User = Depends(get_current_user)):
    """Return the active routing rules for the current user.

    User-specific overrides are stored in Redis.  Falls back to server defaults.
    """
    try:
        r = await _get_redis()
        raw = await r.get(_redis_key(str(current_user.id)))
        await r.aclose()
        if raw:
            user_rules = json.loads(raw)
            merged = {**DEFAULT_RULES, **user_rules}
            return {"rules": merged, "is_custom": True}
    except Exception as exc:
        logger.warning("Redis unavailable for routing rules lookup: %s", exc)

    return {"rules": DEFAULT_RULES, "is_custom": False}


@router.put("/rules", summary="Update routing rules")
async def update_rules(
    body: RoutingRulesRequest,
    current_user: User = Depends(get_current_user),
):
    """Persist per-user routing overrides to Redis."""
    invalid_cats = set(body.rules.keys()) - VALID_CATEGORIES
    if invalid_cats:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Invalid categories: {invalid_cats}. Valid: {VALID_CATEGORIES}",
        )
    invalid_provs = set(body.rules.values()) - VALID_PROVIDERS
    if invalid_provs:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Invalid providers: {invalid_provs}. Valid: {VALID_PROVIDERS}",
        )

    try:
        r = await _get_redis()
        await r.set(_redis_key(str(current_user.id)), json.dumps(body.rules), ex=60 * 60 * 24 * 30)
        await r.aclose()
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"Could not persist rules (Redis unavailable): {exc}",
        )

    merged = {**DEFAULT_RULES, **body.rules}
    return {"rules": merged, "message": "Routing rules updated."}
