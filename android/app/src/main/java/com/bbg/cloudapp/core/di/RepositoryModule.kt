package com.bbg.cloudapp.core.di

import com.bbg.cloudapp.data.repository.AuthRepository
import com.bbg.cloudapp.data.repository.AuthRepositoryImpl
import com.bbg.cloudapp.data.repository.FileRepository
import com.bbg.cloudapp.data.repository.FileRepositoryImpl
import com.bbg.cloudapp.data.repository.OnboardingRepository
import com.bbg.cloudapp.data.repository.OnboardingRepositoryImpl
import com.bbg.cloudapp.data.repository.StorageRepository
import com.bbg.cloudapp.data.repository.StorageRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository
}
