package com.bbg.cloudapp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.FileCategory
import com.bbg.cloudapp.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RoutingRulesUiState {
    object Loading : RoutingRulesUiState()
    data class Loaded(val rules: Map<String, String>) : RoutingRulesUiState()
    data class Saved(val rules: Map<String, String>) : RoutingRulesUiState()
    data class Error(val message: String) : RoutingRulesUiState()
}

@HiltViewModel
class RoutingRulesViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<RoutingRulesUiState>(RoutingRulesUiState.Loading)
    val uiState: StateFlow<RoutingRulesUiState> = _uiState.asStateFlow()

    private val currentRules = mutableMapOf<String, String>()

    init {
        loadRules()
    }

    private fun loadRules() {
        viewModelScope.launch {
            try {
                val response = apiService.getRoutingRules()
                if (response.isSuccessful) {
                    val rules = response.body() ?: emptyMap()
                    currentRules.clear()
                    currentRules.putAll(rules)
                } else {
                    // Populate defaults
                    FileCategory.values().forEach { cat ->
                        val defaultProvider = CloudProvider.values().find { it.defaultCategory == cat }
                        if (defaultProvider != null) {
                            currentRules[cat.name] = defaultProvider.name
                        }
                    }
                }
                _uiState.value = RoutingRulesUiState.Loaded(currentRules.toMap())
            } catch (e: Exception) {
                FileCategory.values().forEach { cat ->
                    val defaultProvider = CloudProvider.values().find { it.defaultCategory == cat }
                    if (defaultProvider != null) currentRules[cat.name] = defaultProvider.name
                }
                _uiState.value = RoutingRulesUiState.Loaded(currentRules.toMap())
            }
        }
    }

    fun updateRule(category: FileCategory, provider: CloudProvider) {
        currentRules[category.name] = provider.name
        _uiState.value = RoutingRulesUiState.Loaded(currentRules.toMap())
    }

    fun saveRules() {
        viewModelScope.launch {
            try {
                val response = apiService.updateRoutingRules(currentRules.toMap())
                if (response.isSuccessful) {
                    val updated = response.body() ?: currentRules
                    currentRules.clear()
                    currentRules.putAll(updated)
                }
                _uiState.value = RoutingRulesUiState.Saved(currentRules.toMap())
            } catch (e: Exception) {
                _uiState.value = RoutingRulesUiState.Saved(currentRules.toMap())
            }
        }
    }

    fun resetState() {
        _uiState.value = RoutingRulesUiState.Loaded(currentRules.toMap())
    }
}
