package com.bbg.cloudapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bbg.cloudapp.core.database.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Query("SELECT * FROM files WHERE is_deleted = 0 ORDER BY uploaded_at DESC")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE provider = :provider AND is_deleted = 0 ORDER BY uploaded_at DESC")
    fun getFilesByProvider(provider: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE category = :category AND is_deleted = 0 ORDER BY uploaded_at DESC")
    fun getFilesByCategory(category: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE category = :category AND provider = :provider AND is_deleted = 0 ORDER BY uploaded_at DESC")
    fun getFilesByCategoryAndProvider(category: String, provider: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id AND is_deleted = 0")
    suspend fun getFileById(id: String): FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    @Query("UPDATE files SET is_deleted = 1 WHERE id = :id")
    suspend fun softDeleteFile(id: String)

    @Query("UPDATE files SET provider = :provider, remote_path = :remotePath WHERE id = :id")
    suspend fun updateFileProvider(id: String, provider: String, remotePath: String)

    @Query("UPDATE files SET share_url = :shareUrl WHERE id = :id")
    suspend fun updateShareUrl(id: String, shareUrl: String)

    @Query("SELECT * FROM files WHERE is_deleted = 0 ORDER BY uploaded_at DESC LIMIT :limit")
    fun getRecentFiles(limit: Int = 5): Flow<List<FileEntity>>
}
