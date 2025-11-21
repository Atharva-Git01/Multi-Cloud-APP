import json
import os

from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build


def ensure_user_drive_structure(user_email: str):
    """
    Creates a personal folder structure for the user inside BBG Cloud Storage.
    Structure:
        My Drive/
        └── BBG Cloud Storage/
            └── user_email/
                ├── images/
                ├── videos/
                ├── text/
                ├── music/
                └── others/
    """

    # Load user token
    token_file = f"tokens/{user_email}.json"
    if not os.path.exists(token_file):
        raise FileNotFoundError(f"Token file for {user_email} not found!")

    creds = Credentials.from_authorized_user_file(token_file)
    service = build("drive", "v3", credentials=creds)

    # 1️⃣ Locate or create the main folder (BBG Cloud Storage)
    main_name = "BBG Cloud Storage"
    res = (
        service.files()
        .list(
            q=f"name='{main_name}' and mimeType='application/vnd.google-apps.folder' and trashed=false",
            fields="files(id)",
        )
        .execute()
    )

    if not res["files"]:
        main_id = (
            service.files()
            .create(
                body={
                    "name": main_name,
                    "mimeType": "application/vnd.google-apps.folder",
                },
                fields="id",
            )
            .execute()["id"]
        )
        print(f"✅ Created main folder: {main_name}")
    else:
        main_id = res["files"][0]["id"]
        print(f"📁 Found existing main folder: {main_name}")

    # 2️⃣ Locate or create the user's personal folder
    res = (
        service.files()
        .list(
            q=f"name='{user_email}' and '{main_id}' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false",
            fields="files(id)",
        )
        .execute()
    )

    if not res["files"]:
        user_id = (
            service.files()
            .create(
                body={
                    "name": user_email,
                    "mimeType": "application/vnd.google-apps.folder",
                    "parents": [main_id],
                },
                fields="id",
            )
            .execute()["id"]
        )
        print(f"✅ Created user folder for {user_email}")
    else:
        user_id = res["files"][0]["id"]
        print(f"📁 Found existing folder for {user_email}")

    # 3️⃣ Create subfolders *inside* the user's folder
    subfolders = ["images", "videos", "text", "music", "others"]
    folder_map = {"user_folder_id": user_id}

    for name in subfolders:
        q = f"name='{name}' and '{user_id}' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false"
        res = service.files().list(q=q, fields="files(id)").execute()
        if not res["files"]:
            folder_id = (
                service.files()
                .create(
                    body={
                        "name": name,
                        "mimeType": "application/vnd.google-apps.folder",
                        "parents": [
                            user_id
                        ],  # ✅ fix: parent is user folder, not main folder
                    },
                    fields="id",
                )
                .execute()["id"]
            )
            print(f"✅ Created subfolder: {name}")
        else:
            folder_id = res["files"][0]["id"]
            print(f"📂 Found subfolder: {name}")
        folder_map[name] = folder_id

    # 4️⃣ Save folder IDs locally
    with open(f"tokens/{user_email}_folders.json", "w") as f:
        json.dump(folder_map, f, indent=4)

    print("📝 Folder map saved successfully!")
    return folder_map
