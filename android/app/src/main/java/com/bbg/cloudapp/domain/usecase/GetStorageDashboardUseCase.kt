package com.bbg.cloudapp.domain.usecase

import com.bbg.cloudapp.core.model.DashboardData
import com.bbg.cloudapp.data.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStorageDashboardUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    operator fun invoke(): Flow<DashboardData> {
        return storageRepository.getDashboard()
    }
}
