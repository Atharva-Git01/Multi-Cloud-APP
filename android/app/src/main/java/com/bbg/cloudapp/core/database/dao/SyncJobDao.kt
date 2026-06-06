package com.bbg.cloudapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bbg.cloudapp.core.database.entity.SyncJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncJobDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncJob(job: SyncJobEntity): Long

    @Query("SELECT * FROM sync_jobs WHERE status = 'PENDING' ORDER BY created_at ASC")
    fun getPendingJobs(): Flow<List<SyncJobEntity>>

    @Query("UPDATE sync_jobs SET status = :status, error_message = :errorMessage WHERE id = :id")
    suspend fun updateJobStatus(id: Long, status: String, errorMessage: String? = null)

    @Query("UPDATE sync_jobs SET retries = retries + 1 WHERE id = :id")
    suspend fun incrementRetries(id: Long)

    @Query("DELETE FROM sync_jobs WHERE status = 'DONE' AND created_at < :cutoff")
    suspend fun cleanupCompletedJobs(cutoff: Long)
}
