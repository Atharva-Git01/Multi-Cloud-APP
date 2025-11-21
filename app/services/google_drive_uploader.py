import json

from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build


def create_drive_folder_structure():
    """Create BBG Cloud Storage and subfolders on Google Drive."""
    creds = Credentials.from_authorized_user_file("token.json")
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
        file_metadata = {
            "name": main_folder_name,
            "mimeType": "application/vnd.google-apps.folder",
        }
        folder = service.files().create(body=file_metadata, fields="id").execute()
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
            file_metadata = {
                "name": name,
                "mimeType": "application/vnd.google-apps.folder",
                "parents": [main_folder_id],
            }
            folder = service.files().create(body=file_metadata, fields="id").execute()
            folder_ids[name] = folder.get("id")
            print(f"✅ Created subfolder: {name}")

    # Step 3: Save folder IDs locally
    with open("drive_folders.json", "w") as f:
        json.dump(folder_ids, f, indent=4)
    print("📝 drive_folders.json created successfully!")
    return folder_ids
