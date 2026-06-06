package com.bbg.cloudapp.feature.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProviderMgmtUiState {
    object Loading : ProviderMgmtUiState()
    data class Success(val connectedProviders: Set<CloudProvider>) : ProviderMgmtUiState()
    data class ActionResult(val message: String, val connectedProviders: Set<CloudProvider>) : ProviderMgmtUiState()
    data class Error(val message: String) : ProviderMgmtUiState()
}

@HiltViewModel
class ProviderManagementViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProviderMgmtUiState>(ProviderMgmtUiState.Loading)
    val uiState: StateFlow<ProviderMgmtUiState> = _uiState.asStateFlow()

    private val connectedSet = mutableSetOf(CloudProvider.GOOGLE)

    init {
        loadProviders()
    }

    private fun loadProviders() {
        viewModelScope.launch {
            try {
                val response = apiService.getProviders()
                if (response.isSuccessful) {
                    val dtos = response.body() ?: emptyList()
                    dtos.forEach { dto ->
                        if (dto.connected) {
                            runCatching { CloudProvider.valueOf(dto.name.uppercase()) }
                                .onSuccess { connectedSet.add(it) }
                        }
                    }
                }
                _uiState.value = ProviderMgmtUiState.Success(connectedSet.toSet())
            } catch (e: Exception) {
                // Use local defaults on network failure
                _uiState.value = ProviderMgmtUiState.Success(connectedSet.toSet())
            }
        }
    }

    fun connectProvider(provider: CloudProvider) {
        viewModelScope.launch {
            try {
                val response = apiService.connectProvider(mapOf("provider" to provider.name))
                if (response.isSuccessful) {
                    connectedSet.add(provider)
                    _uiState.value = ProviderMgmtUiState.ActionResult(
                        "${provider.displayName} connected",
                        connectedSet.toSet()
                    )
                } else {
                    _uiState.value = ProviderMgmtUiState.ActionResult(
                        "Failed to connect ${provider.displayName}",
                        connectedSet.toSet()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProviderMgmtUiState.ActionResult(
                    e.message ?: "Connection failed",
                    connectedSet.toSet()
                )
            }
        }
    }

    fun disconnectProvider(provider: CloudProvider) {
        viewModelScope.launch {
            try {
                val response = apiService.disconnectProvider(provider.name)
                if (response.isSuccessful) {
                    connectedSet.remove(provider)
                    _uiState.value = ProviderMgmtUiState.ActionResult(
                        "${provider.displayName} disconnected",
                        connectedSet.toSet()
                    )
                } else {
                    _uiState.value = ProviderMgmtUiState.ActionResult(
                        "Failed to disconnect ${provider.displayName}",
                        connectedSet.toSet()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProviderMgmtUiState.ActionResult(
                    e.message ?: "Disconnect failed",
                    connectedSet.toSet()
                )
            }
        }
    }

    fun clearActionResult() {
        _uiState.value = ProviderMgmtUiState.Success(connectedSet.toSet())
    }
}
