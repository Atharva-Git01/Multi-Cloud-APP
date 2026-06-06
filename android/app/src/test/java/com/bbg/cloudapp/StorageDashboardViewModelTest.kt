package com.bbg.cloudapp

import app.cash.turbine.test
import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.DashboardData
import com.bbg.cloudapp.core.model.ProviderQuota
import com.bbg.cloudapp.data.repository.StorageRepository
import com.bbg.cloudapp.domain.usecase.GetStorageDashboardUseCase
import com.bbg.cloudapp.feature.dashboard.DashboardUiState
import com.bbg.cloudapp.feature.dashboard.StorageDashboardViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StorageDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getStorageDashboard: GetStorageDashboardUseCase
    private lateinit var storageRepository: StorageRepository
    private lateinit var viewModel: StorageDashboardViewModel

    private val fakeDashboard = DashboardData(
        quotas = listOf(
            ProviderQuota(
                provider = CloudProvider.GOOGLE,
                usedGB = 5.0,
                totalGB = 15.0,
                freeGB = 10.0,
                percentUsed = 33.3,
                lastCheckedAt = System.currentTimeMillis()
            ),
            ProviderQuota(
                provider = CloudProvider.MEGA,
                usedGB = 10.0,
                totalGB = 20.0,
                freeGB = 10.0,
                percentUsed = 50.0,
                lastCheckedAt = System.currentTimeMillis()
            )
        ),
        totalFreeGB = 20.0,
        totalUsedGB = 15.0,
        totalGB = 35.0
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getStorageDashboard = mockk()
        storageRepository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // UT-VM-06: ViewModel emits Success when dashboard loads
    @Test
    fun `UT-VM-06 dashboard loads successfully and emits Success state`() = runTest {
        every { getStorageDashboard() } returns flowOf(fakeDashboard)
        coEvery { storageRepository.refreshQuotas() } returns Result.success(Unit)

        viewModel = StorageDashboardViewModel(getStorageDashboard, storageRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success
        assertEquals(35.0, success.data.totalGB)
        assertEquals(2, success.data.quotas.size)
    }

    // UT-VM-07: ViewModel emits Error when dashboard throws
    @Test
    fun `UT-VM-07 dashboard emits Error state when use case throws`() = runTest {
        every { getStorageDashboard() } returns flow {
            throw RuntimeException("Network failure")
        }
        coEvery { storageRepository.refreshQuotas() } returns Result.failure(Exception())

        viewModel = StorageDashboardViewModel(getStorageDashboard, storageRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Error)
        assertEquals("Network failure", (state as DashboardUiState.Error).message)
    }
}
