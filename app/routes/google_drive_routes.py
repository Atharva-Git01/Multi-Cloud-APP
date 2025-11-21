# google_drive_routes.py
import json
import os

from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import RedirectResponse
from google_auth_oauthlib.flow import Flow

router = APIRouter(tags=["Google Drive Web"])

SCOPES = [
    "openid",
    "https://www.googleapis.com/auth/userinfo.email",
    "https://www.googleapis.com/auth/drive.file",
]

CLIENT_SECRETS_FILE = "credentials_web.json"
REDIRECT_URI = "http://127.0.0.1:8000/api/google/callback"


@router.get("/google/login")
async def google_login():
    try:
        flow = Flow.from_client_secrets_file(
            CLIENT_SECRETS_FILE, scopes=SCOPES, redirect_uri=REDIRECT_URI
        )

        auth_url, _ = flow.authorization_url(
            access_type="offline",
            prompt="consent",
            include_granted_scopes="false",  # 🔹 ensure fresh scopes
        )
        return RedirectResponse(auth_url)

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/google/callback")
async def google_callback(request: Request):
    """
    Callback endpoint Google will redirect to with ?code=...
    Exchanges the code for tokens and saves them to token.json.
    """
    try:
        code = request.query_params.get("code")
        if not code:
            raise HTTPException(status_code=400, detail="Missing code query param")

        flow = Flow.from_client_secrets_file(
            CLIENT_SECRETS_FILE,
            scopes=SCOPES,
            redirect_uri=REDIRECT_URI,
        )
        flow.fetch_token(code=code)
        creds = flow.credentials

        # Save credentials to token.json (simple JSON)
        token_data = {
            "token": creds.token,
            "refresh_token": creds.refresh_token,
            "token_uri": creds.token_uri,
            "client_id": creds.client_id,
            "client_secret": creds.client_secret,
            "scopes": creds.scopes,
        }
        token_path = os.path.join(os.getcwd(), "token.json")
        with open(token_path, "w") as f:
            json.dump(token_data, f, indent=2)

        return {
            "message": "Authentication successful. token.json written.",
            "token_file": token_path,
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
