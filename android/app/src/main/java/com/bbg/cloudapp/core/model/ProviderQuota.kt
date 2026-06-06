package com.bbg.cloudapp.core.model

data class ProviderQuota(
    val provider: CloudProvider,
    val usedGB: Double,
    val totalGB: Double,
    val freeGB: Double,
    val percentUsed: Double,
    val lastCheckedAt: Long
)

data class DashboardData(
    val quotas: List<ProviderQuota>,
    val totalFreeGB: Double,
    val totalUsedGB: Double,
    val totalGB: Double
)
