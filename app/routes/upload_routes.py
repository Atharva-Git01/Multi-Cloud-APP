import os

from fastapi import APIRouter, File, UploadFile

from app.services.file_manager import save_uploaded_file
from app.services.google_drive_auto_upload import upload_sorted_file

router = APIRouter(tags=["Uploads"])

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)


@router.post("/upload")
async def upload_file(file: UploadFile = File(...)):
    """Upload a single file → save locally → auto-upload to Google Drive"""
    try:
        # Step 1: Save locally
        file_path = await save_uploaded_file(file, UPLOAD_DIR)

        # Step 2: Upload same file to Google Drive (auto-detects correct folder)
        drive_result = upload_sorted_file(file_path)

        return {
            "message": f"✅ {file.filename} uploaded and synced to Google Drive.",
            "local_path": file_path,
            "drive_info": drive_result,
        }

    except Exception as e:
        return {"error": str(e)}
