package com.bbg.cloudapp.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbg.cloudapp.core.model.DashboardData
import com.bbg.cloudapp.data.repository.StorageRepository
import com.bbg.cloudapp.domain.usecase.GetStorageDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val data: DashboardData) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class StorageDashboardViewModel @Inject constructor(
    private val getStorageDashboard: GetStorageDashboardUseCase,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            getStorageDashboard()
                .catch { e -> _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error") }
                .collect { data -> _uiState.value = DashboardUiState.Success(data) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            storageRepository.refreshQuotas()
            _isRefreshing.value = false
            loadDashboard()
        }
    }
}
