from typing import Dict, List, Optional

from pydantic import BaseModel, EmailStr


class OnboardingRequest(BaseModel):
    email: EmailStr
    password: str
    providers: List[str]


class AccountCreationStatus(BaseModel):
    provider: str
    status: str           # "pending" | "success" | "failed" | "skipped"
    message: Optional[str] = None
    account_email: Optional[str] = None


class AccountCreationResponse(BaseModel):
    task_id: str
    statuses: Dict[str, AccountCreationStatus]
    overall: str          # "pending" | "completed" | "partial"
