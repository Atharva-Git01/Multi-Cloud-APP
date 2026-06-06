package com.bbg.cloudapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bbg.cloudapp.core.database.dao.FileDao
import com.bbg.cloudapp.core.database.dao.SyncJobDao
import com.bbg.cloudapp.core.database.entity.FileEntity
import com.bbg.cloudapp.core.database.entity.SyncJobEntity

@Database(
    entities = [FileEntity::class, SyncJobEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BBGDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun syncJobDao(): SyncJobDao
}
