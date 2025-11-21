import json
import mimetypes

from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload


def upload_sorted_file(local_path: str):
    """Upload a file to the correct Google Drive subfolder based on type."""
    creds = Credentials.from_authorized_user_file("token.json")
    service = build("drive", "v3", credentials=creds)

    # Load your Drive folder IDs
    with open("drive_folders.json", "r") as f:
        folder_ids = json.load(f)

    mime_type, _ = mimetypes.guess_type(local_path)
    file_type = "others"

    if mime_type:
        if mime_type.startswith("image/"):
            file_type = "images"
        elif mime_type.startswith("video/"):
            file_type = "videos"
        elif mime_type.startswith("text/"):
            file_type = "text"
        elif mime_type.startswith("audio/"):
            file_type = "music"

    folder_id = folder_ids.get(file_type, folder_ids["others"])
    file_name = local_path.split("\\")[-1].split("/")[-1]

    file_metadata = {"name": file_name, "parents": [folder_id]}
    media = MediaFileUpload(
        local_path, mimetype=mime_type or "application/octet-stream"
    )

    uploaded = (
        service.files()
        .create(body=file_metadata, media_body=media, fields="id, name")
        .execute()
    )
    return {"message": f"{file_name} uploaded to {file_type} folder.", "file": uploaded}
