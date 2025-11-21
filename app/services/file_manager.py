import mimetypes
import os

from fastapi import UploadFile


def detect_file_category(filename: str) -> str:
    """Detect general category: image, video, text, music, or other"""
    mime_type, _ = mimetypes.guess_type(filename)
    if mime_type:
        if mime_type.startswith("image"):
            return "images"
        elif mime_type.startswith("video"):
            return "videos"
        elif mime_type.startswith("text"):
            return "text"
        elif mime_type.startswith("audio"):
            return "music"
    return "others"


async def save_uploaded_file(file: UploadFile, upload_dir: str) -> str:
    """Save uploaded file in subfolder based on file type"""
    category = detect_file_category(file.filename)
    category_path = os.path.join(upload_dir, category)
    os.makedirs(category_path, exist_ok=True)

    file_path = os.path.join(category_path, file.filename)
    with open(file_path, "wb") as f:
        f.write(await file.read())
    return file_path
