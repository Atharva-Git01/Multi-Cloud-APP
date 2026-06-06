package com.bbg.cloudapp.core.network.dto

import com.google.gson.annotations.SerializedName

data class ProviderQuotaDto(
    @SerializedName("provider") val provider: String,
    @SerializedName("used_gb") val usedGB: Double,
    @SerializedName("total_gb") val totalGB: Double,
    @SerializedName("free_gb") val freeGB: Double,
    @SerializedName("percent_used") val percentUsed: Double,
    @SerializedName("last_checked_at") val lastCheckedAt: Long
)

data class DashboardDto(
    @SerializedName("quotas") val quotas: List<ProviderQuotaDto>,
    @SerializedName("total_free_gb") val totalFreeGB: Double,
    @SerializedName("total_used_gb") val totalUsedGB: Double,
    @SerializedName("total_gb") val totalGB: Double
)
