package com.bbg.cloudapp.data.repository

import android.content.Context
import android.net.Uri
import com.bbg.cloudapp.core.database.dao.FileDao
import com.bbg.cloudapp.core.database.entity.FileEntity
import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.FileCategory
import com.bbg.cloudapp.core.model.FileRecord
import com.bbg.cloudapp.core.network.ApiService
import com.bbg.cloudapp.core.network.dto.MoveFileRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class UploadState {
    object Idle : UploadState()
    data class Uploading(val progress: Int) : UploadState()
    data class Success(val provider: CloudProvider, val fileRecord: FileRecord) : UploadState()
    data class Error(val message: String) : UploadState()
}

interface FileRepository {
    fun getFiles(category: FileCategory? = null, provider: CloudProvider? = null): Flow<List<FileRecord>>
    fun getRecentFiles(limit: Int = 5): Flow<List<FileRecord>>
    suspend fun uploadFile(uri: Uri): UploadState
    suspend fun moveFile(fileId: String, targetProvider: CloudProvider): Result<Unit>
    suspend fun deleteFile(fileId: String): Result<Unit>
    suspend fun getShareLink(fileId: String): Result<String>
    suspend fun syncFilesFromNetwork()
}

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val fileDao: FileDao,
    @ApplicationContext private val context: Context
) : FileRepository {

    override fun getFiles(category: FileCategory?, provider: CloudProvider?): Flow<List<FileRecord>> {
        return when {
            category != null && provider != null ->
                fileDao.getFilesByCategoryAndProvider(category.name, provider.name).map { it.map(::toRecord) }
            category != null ->
                fileDao.getFilesByCategory(category.name).map { it.map(::toRecord) }
            provider != null ->
                fileDao.getFilesByProvider(provider.name).map { it.map(::toRecord) }
            else ->
                fileDao.getAllFiles().map { it.map(::toRecord) }
        }
    }

    override fun getRecentFiles(limit: Int): Flow<List<FileRecord>> {
        return fileDao.getRecentFiles(limit).map { it.map(::toRecord) }
    }

    override suspend fun uploadFile(uri: Uri): UploadState {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val inputStream = contentResolver.openInputStream(uri)
                ?: return UploadState.Error("Cannot open file")

            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}")
            tempFile.outputStream().use { output -> inputStream.copyTo(output) }

            val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

            val response = apiService.uploadFile(part)
            tempFile.delete()

            if (response.isSuccessful) {
                val dto = response.body()!!
                val provider = CloudProvider.valueOf(dto.provider.uppercase())
                val entity = FileEntity(
                    id = dto.file.id,
                    userId = dto.file.userId,
                    originalName = dto.file.originalName,
                    mimeType = dto.file.mimeType,
                    category = dto.file.category,
                    sizeBytes = dto.file.sizeBytes,
                    uploadedAt = dto.file.uploadedAt,
                    provider = dto.file.provider,
                    remotePath = dto.file.remotePath,
                    shareUrl = dto.file.shareUrl
                )
                fileDao.insertFile(entity)
                UploadState.Success(provider, toRecord(entity))
            } else {
                UploadState.Error("Upload failed: ${response.code()}")
            }
        } catch (e: Exception) {
            UploadState.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun moveFile(fileId: String, targetProvider: CloudProvider): Result<Unit> {
        return try {
            val response = apiService.moveFile(fileId, MoveFileRequest(targetProvider.name))
            if (response.isSuccessful) {
                val dto = response.body()!!
                fileDao.updateFileProvider(fileId, dto.provider, dto.remotePath)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Move failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(fileId: String): Result<Unit> {
        return try {
            val response = apiService.deleteFile(fileId)
            if (response.isSuccessful) {
                fileDao.softDeleteFile(fileId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getShareLink(fileId: String): Result<String> {
        return try {
            val response = apiService.shareFile(fileId)
            if (response.isSuccessful) {
                val shareUrl = response.body()!!.shareUrl
                fileDao.updateShareUrl(fileId, shareUrl)
                Result.success(shareUrl)
            } else {
                Result.failure(Exception("Share failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncFilesFromNetwork() {
        try {
            val response = apiService.getFiles()
            if (response.isSuccessful) {
                val dtos = response.body()?.files ?: emptyList()
                val entities = dtos.map { dto ->
                    FileEntity(
                        id = dto.id,
                        userId = dto.userId,
                        originalName = dto.originalName,
                        mimeType = dto.mimeType,
                        category = dto.category,
                        sizeBytes = dto.sizeBytes,
                        uploadedAt = dto.uploadedAt,
                        provider = dto.provider,
                        remotePath = dto.remotePath,
                        shareUrl = dto.shareUrl
                    )
                }
                fileDao.insertFiles(entities)
            }
        } catch (_: Exception) { }
    }

    private fun toRecord(entity: FileEntity): FileRecord {
        val category = runCatching { FileCategory.valueOf(entity.category) }.getOrDefault(FileCategory.OTHER)
        val provider = runCatching { CloudProvider.valueOf(entity.provider.uppercase()) }.getOrDefault(CloudProvider.GOOGLE)
        return FileRecord(
            id = entity.id,
            userId = entity.userId,
            originalName = entity.originalName,
            mimeType = entity.mimeType,
            category = category,
            sizeBytes = entity.sizeBytes,
            uploadedAt = entity.uploadedAt,
            provider = provider,
            remotePath = entity.remotePath,
            shareUrl = entity.shareUrl
        )
    }
}
