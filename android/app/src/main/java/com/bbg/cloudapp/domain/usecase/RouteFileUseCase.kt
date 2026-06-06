package com.bbg.cloudapp.domain.usecase

import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.FileCategory
import com.bbg.cloudapp.core.model.ProviderQuota
import javax.inject.Inject

class RouteFileUseCase @Inject constructor() {

    /**
     * Given the current quota state and a file category, returns the best cloud provider.
     *
     * Routing logic:
     * 1. Filter providers that have enough free space (> 0.1 GB remaining).
     * 2. Prefer the default provider for the file category.
     * 3. Among remaining candidates, prefer the provider with the most free GB.
     * 4. Fallback: return Google Drive.
     */
    operator fun invoke(
        quotas: List<ProviderQuota>,
        fileCategory: FileCategory,
        fileSizeMB: Long = 0L
    ): CloudProvider {
        val fileSizeGB = fileSizeMB / 1024.0

        val eligible = quotas.filter { quota ->
            quota.freeGB > fileSizeGB && quota.freeGB > 0.1
        }

        if (eligible.isEmpty()) return CloudProvider.GOOGLE

        // Check if category's preferred provider has space
        val categoryDefault = CloudProvider.values().find { it.defaultCategory == fileCategory }
        if (categoryDefault != null) {
            val preferredQuota = eligible.find { it.provider == categoryDefault }
            if (preferredQuota != null) {
                // Also respect max file size constraint
                val maxMB = categoryDefault.maxFileSizeMB
                if (maxMB == null || fileSizeMB <= maxMB) {
                    return categoryDefault
                }
            }
        }

        // Fall back to provider with most free space
        return eligible.maxByOrNull { it.freeGB }?.provider ?: CloudProvider.GOOGLE
    }
}
