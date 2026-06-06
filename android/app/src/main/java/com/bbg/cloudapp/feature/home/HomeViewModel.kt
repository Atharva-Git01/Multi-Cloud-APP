package com.bbg.cloudapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbg.cloudapp.core.model.DashboardData
import com.bbg.cloudapp.core.model.FileRecord
import com.bbg.cloudapp.data.repository.FileRepository
import com.bbg.cloudapp.domain.usecase.GetStorageDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val dashboardData: DashboardData,
        val recentFiles: List<FileRecord>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getStorageDashboard: GetStorageDashboardUseCase,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getStorageDashboard(),
                fileRepository.getRecentFiles(5)
            ) { dashboard, recentFiles ->
                HomeUiState.Success(dashboard, recentFiles)
            }
            .catch { e ->
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
            .collect { state ->
                _uiState.value = state
            }
        }
    }

    fun refresh() {
        _uiState.value = HomeUiState.Loading
        loadData()
    }
}
