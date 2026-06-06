"""Onboarding routes — batch account creation for new users."""
from __future__ import annotations

import logging
from typing import List

from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.core.database import get_db
from app.core.security import encrypt_token, get_password_hash
from app.models.cloud_account import CloudAccount
from app.models.user import User
from app.schemas.onboarding import (
    AccountCreationResponse,
    AccountCreationStatus,
    OnboardingRequest,
)
from app.services.providers.mega_provider import MegaProvider
from app.services.providers.pcloud_provider import PCloudProvider

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/onboarding", tags=["onboarding"])

SELF_REGISTER_PROVIDERS = {"mega", "pcloud"}
OAUTH_PROVIDERS = {"google", "onedrive", "box", "dropbox"}

PROVIDER_MAP = {
    "mega": MegaProvider,
    "pcloud": PCloudProvider,
}


async def _create_single_account(
    provider: str,
    email: str,
    password: str,
    user_id: str,
    db: AsyncSession,
) -> AccountCreationStatus:
    if provider in OAUTH_PROVIDERS:
        return AccountCreationStatus(
            provider=provider,
            state="skipped",
            error_message=f"{provider} requires OAuth — connect via /api/providers/connect",
        )

    provider_cls = PROVIDER_MAP.get(provider)
    if provider_cls is None:
        return AccountCreationStatus(
            provider=provider,
            state="failed",
            error_message=f"Unknown provider: {provider}",
        )

    try:
        account = await provider_cls().create_account(email, password)
        enc_token = encrypt_token(account.access_token) if account.access_token else None
        enc_refresh = encrypt_token(account.refresh_token) if account.refresh_token else None

        cloud_acc = CloudAccount(
            user_id=user_id,
            provider=provider,
            account_email=account.account_email,
            access_token_enc=enc_token,
            refresh_token_enc=enc_refresh,
            is_active=True,
        )
        db.add(cloud_acc)
        await db.flush()

        return AccountCreationStatus(
            provider=provider,
            state="success",
            account_email=account.account_email,
        )
    except Exception as exc:
        logger.error("Account creation failed for %s: %s", provider, exc)
        return AccountCreationStatus(
            provider=provider,
            state="failed",
            error_message=str(exc),
        )


@router.post("/create-accounts", response_model=AccountCreationResponse, status_code=status.HTTP_202_ACCEPTED)
async def create_accounts(
    body: OnboardingRequest,
    db: AsyncSession = Depends(get_db),
):
    """Kick off account creation for supported providers.

    For providers that support programmatic registration (MEGA, pCloud) we
    create accounts immediately.  OAuth providers (Google, OneDrive, Box,
    Dropbox) are marked as `skipped` with redirect instructions.
    """
    # Get or create the BBG user
    result = await db.execute(select(User).where(User.email == body.email))
    user: User | None = result.scalar_one_or_none()

    if user is None:
        user = User(
            email=body.email,
            hashed_password=get_password_hash(body.password),
            display_name=body.email.split("@")[0],
        )
        db.add(user)
        await db.flush()
        await db.refresh(user)

    statuses: List[AccountCreationStatus] = []

    for provider in body.providers:
        s = await _create_single_account(
            provider=provider,
            email=body.email,
            password=body.password,
            user_id=str(user.id),
            db=db,
        )
        statuses.append(s)

    return AccountCreationResponse(statuses=statuses)
