package com.bbg.cloudapp

import app.cash.turbine.test
import com.bbg.cloudapp.core.model.AccountCreationStatus
import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.CreationState
import com.bbg.cloudapp.domain.usecase.CreateCloudAccountsUseCase
import com.bbg.cloudapp.feature.onboarding.OnboardingUiState
import com.bbg.cloudapp.feature.onboarding.OnboardingViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var createAccountsUseCase: CreateCloudAccountsUseCase
    private lateinit var viewModel: OnboardingViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        createAccountsUseCase = mockk()
        viewModel = OnboardingViewModel(createAccountsUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // UT-VM-01: Google Drive always selected and cannot be deselected
    @Test
    fun `UT-VM-01 google drive is always selected and cannot be removed`() = runTest {
        val initial = viewModel.selectedProviders.value
        assertTrue(CloudProvider.GOOGLE in initial)

        viewModel.toggleProvider(CloudProvider.GOOGLE)
        val after = viewModel.selectedProviders.value
        assertTrue(CloudProvider.GOOGLE in after, "Google Drive must remain selected")
    }

    // UT-VM-02: Toggle non-Google provider adds/removes it
    @Test
    fun `UT-VM-02 toggling a non-Google provider adds then removes it`() = runTest {
        assertFalse(CloudProvider.MEGA in viewModel.selectedProviders.value)

        viewModel.toggleProvider(CloudProvider.MEGA)
        assertTrue(CloudProvider.MEGA in viewModel.selectedProviders.value)

        viewModel.toggleProvider(CloudProvider.MEGA)
        assertFalse(CloudProvider.MEGA in viewModel.selectedProviders.value)
    }

    // UT-VM-03: totalSelectedGB is computed correctly
    @Test
    fun `UT-VM-03 totalSelectedGB sums freeStorageGB of selected providers`() = runTest {
        // Default: only Google (15 GB)
        assertEquals(15, viewModel.totalSelectedGB.value)

        viewModel.toggleProvider(CloudProvider.MEGA) // +20 GB
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(35, viewModel.totalSelectedGB.value)

        viewModel.toggleProvider(CloudProvider.ONEDRIVE) // +5 GB
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(40, viewModel.totalSelectedGB.value)
    }

    // UT-VM-04: startAccountCreation emits Creating then Complete
    @Test
    fun `UT-VM-04 startAccountCreation transitions state to Creating then Complete`() = runTest {
        val doneStatuses = listOf(
            AccountCreationStatus(CloudProvider.GOOGLE, CreationState.DONE)
        )
        every { createAccountsUseCase(any(), any(), any()) } returns flowOf(doneStatuses)

        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Idle, awaitItem())
            viewModel.startAccountCreation("user@test.com", "Password1!")
            assertEquals(OnboardingUiState.Creating, awaitItem())
            assertEquals(OnboardingUiState.Complete, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // UT-VM-05: creationStatuses is updated from use case emissions
    @Test
    fun `UT-VM-05 creationStatuses reflects use case flow emissions`() = runTest {
        val statusesA = listOf(AccountCreationStatus(CloudProvider.GOOGLE, CreationState.CREATING))
        val statusesB = listOf(AccountCreationStatus(CloudProvider.GOOGLE, CreationState.DONE))

        every { createAccountsUseCase(any(), any(), any()) } returns flow {
            emit(statusesA)
            emit(statusesB)
        }

        viewModel.creationStatuses.test {
            assertEquals(emptyList<AccountCreationStatus>(), awaitItem())
            viewModel.startAccountCreation("user@test.com", "Password1!")
            testDispatcher.scheduler.advanceUntilIdle()
            val last = awaitItem()
            // After full emission, the last value should be the DONE status
            assertEquals(CreationState.DONE, last.first().state)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
