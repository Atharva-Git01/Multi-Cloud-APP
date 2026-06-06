package com.bbg.cloudapp.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.bbg.cloudapp.core.database.BBGDatabase
import com.bbg.cloudapp.core.database.dao.FileDao
import com.bbg.cloudapp.core.database.dao.SyncJobDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bbg_auth_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideBBGDatabase(@ApplicationContext context: Context): BBGDatabase {
        return Room.databaseBuilder(
            context,
            BBGDatabase::class.java,
            "bbg_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideFileDao(database: BBGDatabase): FileDao {
        return database.fileDao()
    }

    @Provides
    @Singleton
    fun provideSyncJobDao(database: BBGDatabase): SyncJobDao {
        return database.syncJobDao()
    }
}
