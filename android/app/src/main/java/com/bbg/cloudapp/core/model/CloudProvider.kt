package com.bbg.cloudapp.core.model

enum class CloudProvider(
    val displayName: String,
    val freeStorageGB: Int,
    val defaultCategory: FileCategory?,
    val maxFileSizeMB: Int?,
    val iconRes: String,
    val emoji: String
) {
    GOOGLE("Google Drive", 15, FileCategory.IMAGES, null, "ic_google_drive", "📁"),
    ONEDRIVE("OneDrive", 5, FileCategory.DOCUMENTS, null, "ic_onedrive", "📂"),
    MEGA("MEGA", 20, FileCategory.VIDEOS, null, "ic_mega", "⚡"),
    BOX("Box", 10, FileCategory.ARCHIVES, 250, "ic_box", "📦"),
    PCLOUD("pCloud", 10, FileCategory.AUDIO, null, "ic_pcloud", "☁️"),
    DROPBOX("Dropbox", 2, null, null, "ic_dropbox", "💧"),
}

enum class FileCategory(val displayName: String) {
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    ARCHIVES("Archives"),
    OTHER("Other")
}
