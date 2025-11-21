import json
import os

from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build


def create_drive_folder_structure():
    """
    Admin setup: creates the BBG Cloud Storage root and main subfolders (shared).
    Uses 'credentials_web.json' instead of 'credentials.json'.
    """
    TOKEN_FILE = "token.json"
    CREDENTIALS_FILE = "credentials_web.json"
    SCOPES = ["https://www.googleapis.com/auth/drive.file"]

    # ✅ Check for token.json (admin auth)
    if not os.path.exists(TOKEN_FILE):
        if not os.path.exists(CREDENTIALS_FILE):
            raise FileNotFoundError(
                "No credentials_web.json found. Please place it in your cloud_app directory."
            )

        # Run OAuth flow for admin once if token.json doesn't exist
        flow = InstalledAppFlow.from_client_secrets_file(CREDENTIALS_FILE, SCOPES)
        creds = flow.run_local_server(port=0)
        with open(TOKEN_FILE, "w") as token:
            token.write(creds.to_json())
        print("✅ Admin token.json created successfully!")
    else:
        creds = Credentials.from_authorized_user_file(TOKEN_FILE, SCOPES)

    # Build Drive service
    service = build("drive", "v3", credentials=creds)

    main_folder_name = "BBG Cloud Storage"

    # Step 1: Check if main folder exists
    query = f"name='{main_folder_name}' and mimeType='application/vnd.google-apps.folder' and trashed=false"
    results = service.files().list(q=query, fields="files(id, name)").execute()
    items = results.get("files", [])

    if items:
        main_folder_id = items[0]["id"]
        print(f"📁 Found existing main folder: {main_folder_name}")
    else:
        folder = (
            service.files()
            .create(
                body={
                    "name": main_folder_name,
                    "mimeType": "application/vnd.google-apps.folder",
                },
                fields="id",
            )
            .execute()
        )
        main_folder_id = folder.get("id")
        print(f"✅ Created main folder: {main_folder_name}")

    # Step 2: Create subfolders
    subfolders = ["images", "videos", "text", "music", "others"]
    folder_ids = {"main": main_folder_id}

    for name in subfolders:
        query = f"name='{name}' and '{main_folder_id}' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false"
        results = service.files().list(q=query, fields="files(id, name)").execute()
        items = results.get("files", [])
        if items:
            folder_ids[name] = items[0]["id"]
            print(f"📂 Found existing subfolder: {name}")
        else:
            folder = (
                service.files()
                .create(
                    body={
                        "name": name,
                        "mimeType": "application/vnd.google-apps.folder",
                        "parents": [main_folder_id],
                    },
                    fields="id",
                )
                .execute()
            )
            folder_ids[name] = folder.get("id")
            print(f"✅ Created subfolder: {name}")

    # Step 3: Save folder IDs locally
    with open("drive_folders.json", "w") as f:
        json.dump(folder_ids, f, indent=4)
    print("📝 drive_folders.json created successfully!")

    return folder_ids
