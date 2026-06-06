package com.bbg.cloudapp.core.model

data class FileRecord(
    val id: String,
    val userId: String,
    val originalName: String,
    val mimeType: String,
    val category: FileCategory,
    val sizeBytes: Long,
    val uploadedAt: Long,
    val provider: CloudProvider,
    val remotePath: String,
    val shareUrl: String?
)
