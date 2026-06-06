package com.bbg.cloudapp.core.network.dto

import com.google.gson.annotations.SerializedName

data class FileDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("original_name") val originalName: String,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("category") val category: String,
    @SerializedName("size_bytes") val sizeBytes: Long,
    @SerializedName("uploaded_at") val uploadedAt: Long,
    @SerializedName("provider") val provider: String,
    @SerializedName("remote_path") val remotePath: String,
    @SerializedName("share_url") val shareUrl: String?
)

data class FileListResponse(
    @SerializedName("files") val files: List<FileDto>,
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("limit") val limit: Int
)

data class MoveFileRequest(
    @SerializedName("target_provider") val targetProvider: String
)

data class ShareLinkResponse(
    @SerializedName("share_url") val shareUrl: String,
    @SerializedName("expires_at") val expiresAt: Long?
)

data class UploadResponse(
    @SerializedName("file") val file: FileDto,
    @SerializedName("provider") val provider: String,
    @SerializedName("routing_reason") val routingReason: String?
)
