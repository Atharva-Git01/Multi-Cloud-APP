package com.bbg.cloudapp

import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.FileCategory
import com.bbg.cloudapp.core.model.ProviderQuota
import com.bbg.cloudapp.domain.usecase.RouteFileUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RouteFileUseCaseTest {

    private lateinit var useCase: RouteFileUseCase

    private fun quota(
        provider: CloudProvider,
        usedGB: Double,
        totalGB: Double
    ) = ProviderQuota(
        provider = provider,
        usedGB = usedGB,
        totalGB = totalGB,
        freeGB = totalGB - usedGB,
        percentUsed = (usedGB / totalGB) * 100,
        lastCheckedAt = System.currentTimeMillis()
    )

    @BeforeEach
    fun setUp() {
        useCase = RouteFileUseCase()
    }

    // UT-UC-03: Routes images to Google Drive by default
    @Test
    fun `UT-UC-03 images route to Google Drive when it has space`() {
        val quotas = listOf(
            quota(CloudProvider.GOOGLE, usedGB = 1.0, totalGB = 15.0),
            quota(CloudProvider.MEGA, usedGB = 1.0, totalGB = 20.0)
        )
        val result = useCase(quotas, FileCategory.IMAGES)
        assertEquals(CloudProvider.GOOGLE, result)
    }

    // UT-UC-04: Routes videos to MEGA by default
    @Test
    fun `UT-UC-04 videos route to MEGA when it has space`() {
        val quotas = listOf(
            quota(CloudProvider.GOOGLE, usedGB = 1.0, totalGB = 15.0),
            quota(CloudProvider.MEGA, usedGB = 1.0, totalGB = 20.0)
        )
        val result = useCase(quotas, FileCategory.VIDEOS)
        assertEquals(CloudProvider.MEGA, result)
    }

    // UT-UC-05: Falls back to provider with most free space when preferred is full
    @Test
    fun `UT-UC-05 falls back to most-free provider when category default is full`() {
        val quotas = listOf(
            // Google (images default) is almost full
            quota(CloudProvider.GOOGLE, usedGB = 14.9, totalGB = 15.0),
            // MEGA has lots of space
            quota(CloudProvider.MEGA, usedGB = 1.0, totalGB = 20.0),
            quota(CloudProvider.ONEDRIVE, usedGB = 1.0, totalGB = 5.0)
        )
        val result = useCase(quotas, FileCategory.IMAGES)
        // Should fall back to MEGA as it has the most free space (19 GB)
        assertEquals(CloudProvider.MEGA, result)
    }

    // UT-UC-06: Returns Google Drive when all providers are full
    @Test
    fun `UT-UC-06 returns Google Drive when all providers are completely full`() {
        val quotas = listOf(
            quota(CloudProvider.GOOGLE, usedGB = 15.0, totalGB = 15.0),
            quota(CloudProvider.MEGA, usedGB = 20.0, totalGB = 20.0),
            quota(CloudProvider.ONEDRIVE, usedGB = 5.0, totalGB = 5.0)
        )
        val result = useCase(quotas, FileCategory.DOCUMENTS)
        assertEquals(CloudProvider.GOOGLE, result)
    }

    // Bonus: Box max file size is respected
    @Test
    fun `large files skip Box when file exceeds maxFileSizeMB limit`() {
        val quotas = listOf(
            quota(CloudProvider.BOX, usedGB = 1.0, totalGB = 10.0),    // 9 GB free, but 250 MB limit
            quota(CloudProvider.GOOGLE, usedGB = 1.0, totalGB = 15.0), // 14 GB free, no limit
        )
        // 500 MB file should not go to Box
        val result = useCase(quotas, FileCategory.ARCHIVES, fileSizeMB = 500L)
        // Box's max is 250 MB, 500 MB file should be routed elsewhere
        // Since ARCHIVES default is Box but it violates the limit, falls back to most-free = Google
        assertEquals(CloudProvider.GOOGLE, result)
    }
}
