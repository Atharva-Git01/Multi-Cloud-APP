package com.bbg.cloudapp.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbg.cloudapp.core.model.AccountCreationStatus
import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.CreationState
import com.bbg.cloudapp.domain.usecase.CreateCloudAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OnboardingUiState {
    object Idle : OnboardingUiState()
    object Creating : OnboardingUiState()
    object Complete : OnboardingUiState()
    data class Error(val msg: String) : OnboardingUiState()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val createAccounts: CreateCloudAccountsUseCase
) : ViewModel() {

    private val _selectedProviders = MutableStateFlow<Set<CloudProvider>>(setOf(CloudProvider.GOOGLE))
    val selectedProviders: StateFlow<Set<CloudProvider>> = _selectedProviders.asStateFlow()

    val totalSelectedGB: StateFlow<Int> = selectedProviders.map { set ->
        set.sumOf { it.freeStorageGB }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 15)

    private val _creationStatuses = MutableStateFlow<List<AccountCreationStatus>>(emptyList())
    val creationStatuses: StateFlow<List<AccountCreationStatus>> = _creationStatuses.asStateFlow()

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // Stores credentials temporarily during onboarding flow
    private var pendingEmail: String = ""
    private var pendingPassword: String = ""

    fun toggleProvider(provider: CloudProvider) {
        if (provider == CloudProvider.GOOGLE) return
        _selectedProviders.update { current ->
            if (provider in current) current - provider else current + provider
        }
    }

    fun saveCredentials(email: String, password: String) {
        pendingEmail = email
        pendingPassword = password
    }

    fun startAccountCreation(email: String = pendingEmail, password: String = pendingPassword) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Creating
            createAccounts(email, password, _selectedProviders.value)
                .collect { statuses ->
                    _creationStatuses.value = statuses
                    if (statuses.isNotEmpty() &&
                        statuses.all {
                            it.state == CreationState.DONE || it.state == CreationState.FAILED
                        }
                    ) {
                        _uiState.value = OnboardingUiState.Complete
                    }
                }
        }
    }

    fun retryFailedProviders() {
        val failedProviders = _creationStatuses.value
            .filter { it.state == CreationState.FAILED }
            .map { it.provider }
            .toSet()

        if (failedProviders.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Creating
            createAccounts(pendingEmail, pendingPassword, failedProviders)
                .collect { newStatuses ->
                    val updated = _creationStatuses.value.map { existing ->
                        newStatuses.find { it.provider == existing.provider } ?: existing
                    }
                    _creationStatuses.value = updated
                    if (updated.all {
                            it.state == CreationState.DONE || it.state == CreationState.FAILED
                        }
                    ) {
                        _uiState.value = OnboardingUiState.Complete
                    }
                }
        }
    }
}
