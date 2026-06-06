from typing import List, Optional

from pydantic import BaseModel, EmailStr


class OnboardingRequest(BaseModel):
    email: EmailStr
    password: str
    providers: List[str]


class AccountCreationStatus(BaseModel):
    provider: str
    state: str            # "pending" | "success" | "failed" | "skipped"
    error_message: Optional[str] = None
    account_email: Optional[str] = None


class AccountCreationResponse(BaseModel):
    statuses: List[AccountCreationStatus]
