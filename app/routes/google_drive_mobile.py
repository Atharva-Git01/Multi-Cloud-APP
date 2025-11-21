# google_drive_mobile.py
import os

import requests as http_requests
from fastapi import APIRouter, HTTPException
from google.auth.transport import requests as google_requests
from google.oauth2 import id_token
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import Flow
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from pydantic import BaseModel

from app.services.google_drive_auto_upload import upload_sorted_file
from app.services.google_drive_initializer import create_drive_folder_structure
from app.services.google_drive_user_folders import ensure_user_drive_structure

router = APIRouter(tags=["Google Drive Mobile"])


class CodeRequest(BaseModel):
    code: str
    platform: str  # "web" or "mobile"


# ✅ Unified scopes (same everywhere)
SCOPES = [
    "openid",
    "https://www.googleapis.com/auth/userinfo.email",
    "https://www.googleapis.com/auth/drive.file",
]


@router.post("/google/oauth_callback")
async def google_oauth_callback(data: CodeRequest):
    """Handles OAuth callback for both web and mobile and saves user tokens."""
    try:
        # ✅ Dynamic credentials & redirect URIs
        if data.platform == "mobile":
            CLIENT_SECRETS_FILE = "credentials_desktop.json"
            REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"
        else:
            CLIENT_SECRETS_FILE = "credentials_web.json"
            REDIRECT_URI = "http://127.0.0.1:8000/api/google/callback"

        flow = Flow.from_client_secrets_file(
            CLIENT_SECRETS_FILE,
            scopes=SCOPES,
            redirect_uri=REDIRECT_URI,
        )

        # ✅ Manual token exchange (bypasses scope check bug)
        token_request = http_requests.post(
            "https://oauth2.googleapis.com/token",
            data={
                "code": data.code,
                "client_id": flow.client_config["client_id"],
                "client_secret": flow.client_config["client_secret"],
                "redirect_uri": flow.redirect_uri,
                "grant_type": "authorization_code",
            },
        )
        token_data = token_request.json()
        if "error" in token_data:
            raise HTTPException(status_code=400, detail=token_data["error_description"])

        creds = Credentials(
            token=token_data["access_token"],
            refresh_token=token_data.get("refresh_token"),
            token_uri="https://oauth2.googleapis.com/token",
            client_id=flow.client_config["client_id"],
            client_secret=flow.client_config["client_secret"],
            scopes=SCOPES,
        )

        # ✅ Extract user email
        info = id_token.verify_oauth2_token(
            token_data["id_token"],
            google_requests.Request(),
            flow.client_config["client_id"],
        )
        email = info.get("email")

        # ✅ Save user credentials
        os.makedirs("tokens", exist_ok=True)
        user_token_file = f"tokens/{email}.json"
        with open(user_token_file, "w") as f:
            f.write(creds.to_json())

        # ✅ Auto-create personal folder structure after login
        try:
            ensure_user_drive_structure(email)
            print(f"✅ User Drive folders created successfully for {email}")
        except Exception as e:
            print(f"⚠️ Failed to create Drive folders for {email}: {e}")

        return {
            "message": "✅ Authentication successful",
            "platform": data.platform,
            "email": email,
            "token_file": user_token_file,
        }

    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/google/upload_local_file")
async def upload_local_file(file_path: str):
    """Uploads a local file to Google Drive automatically."""
    try:
        result = upload_sorted_file(file_path)
        return result
    except Exception as e:
        return {"error": str(e)}


@router.get("/google/get_auth_url")
async def get_google_auth_url(platform: str = "web"):
    """Generate Google OAuth2 URL (web or mobile)."""
    try:
        if platform == "mobile":
            CLIENT_SECRETS_FILE = "credentials_desktop.json"
            REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"
        else:
            CLIENT_SECRETS_FILE = "credentials_web.json"
            REDIRECT_URI = "http://127.0.0.1:8000/api/google/callback"

        print(f"🚨 DEBUG: Using redirect URI → {REDIRECT_URI}")

        flow = Flow.from_client_secrets_file(
            CLIENT_SECRETS_FILE,
            scopes=SCOPES,
            redirect_uri=REDIRECT_URI,
        )

        auth_url, _ = flow.authorization_url(
            access_type="offline",
            prompt="consent",
            include_granted_scopes="false",
        )

        return {"auth_url": auth_url, "platform": platform}

    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/google/create_user_folders")
async def create_user_folders(email: str):
    """Creates per-user folders inside Google Drive."""
    try:
        result = ensure_user_drive_structure(email)
        return {"message": "User Drive folders ready ✅", "data": result}
    except Exception as e:
        return {"error": str(e)}


@router.get("/google/setup_admin_drive")
async def setup_admin_drive():
    """One-time setup for global 'BBG Cloud Storage' folder."""
    try:
        folder_map = create_drive_folder_structure()
        return {"message": "✅ Admin Drive structure initialized", "data": folder_map}
    except Exception as e:
        return {"error": str(e)}
