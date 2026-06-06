package com.bbg.cloudapp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbg.cloudapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun signOut() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            // Logout locally — actual account deletion would require additional API endpoint
            authRepository.logout()
        }
    }

    fun exportCsv() {
        // In a full implementation, this would query all files from Room,
        // generate a CSV string, and write it to external storage via FileProvider.
        // Stub for now — the actual work is platform-level file I/O.
    }
}
