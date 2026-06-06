package com.bbg.cloudapp.data.repository

import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.DashboardData
import com.bbg.cloudapp.core.model.ProviderQuota
import com.bbg.cloudapp.core.network.ApiService
import com.bbg.cloudapp.core.network.dto.ProviderDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface StorageRepository {
    fun getDashboard(): Flow<DashboardData>
    suspend fun refreshQuotas(): Result<Unit>
    suspend fun getProviders(): Result<List<ProviderDto>>
}

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : StorageRepository {

    override fun getDashboard(): Flow<DashboardData> = flow {
        val response = apiService.getDashboard()
        if (response.isSuccessful) {
            val dto = response.body()!!
            val quotas = dto.quotas.mapNotNull { q ->
                val provider = runCatching { CloudProvider.valueOf(q.provider.uppercase()) }.getOrNull()
                    ?: return@mapNotNull null
                ProviderQuota(
                    provider = provider,
                    usedGB = q.usedGB,
                    totalGB = q.totalGB,
                    freeGB = q.freeGB,
                    percentUsed = q.percentUsed,
                    lastCheckedAt = q.lastCheckedAt
                )
            }
            emit(
                DashboardData(
                    quotas = quotas,
                    totalFreeGB = dto.totalFreeGB,
                    totalUsedGB = dto.totalUsedGB,
                    totalGB = dto.totalGB
                )
            )
        } else {
            throw Exception("Dashboard fetch failed: ${response.code()}")
        }
    }

    override suspend fun refreshQuotas(): Result<Unit> {
        return try {
            val response = apiService.refreshQuotas()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Refresh failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProviders(): Result<List<ProviderDto>> {
        return try {
            val response = apiService.getProviders()
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Providers fetch failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
