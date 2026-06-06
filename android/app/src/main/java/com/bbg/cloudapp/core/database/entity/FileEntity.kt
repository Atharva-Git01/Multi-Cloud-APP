package com.bbg.cloudapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "original_name") val originalName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "uploaded_at") val uploadedAt: Long,
    @ColumnInfo(name = "provider") val provider: String,
    @ColumnInfo(name = "remote_path") val remotePath: String,
    @ColumnInfo(name = "share_url") val shareUrl: String?,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)
