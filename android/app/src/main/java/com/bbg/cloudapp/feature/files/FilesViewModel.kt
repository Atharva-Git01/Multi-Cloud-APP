package com.bbg.cloudapp.feature.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.FileCategory
import com.bbg.cloudapp.core.model.FileRecord
import com.bbg.cloudapp.data.repository.FileRepository
import com.bbg.cloudapp.domain.usecase.GetFilesUseCase
import com.bbg.cloudapp.domain.usecase.MoveFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FilesUiState {
    object Loading : FilesUiState()
    data class Success(val files: List<FileRecord>) : FilesUiState()
    data class Error(val message: String) : FilesUiState()
}

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val getFilesUseCase: GetFilesUseCase,
    private val moveFileUseCase: MoveFileUseCase,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory.asStateFlow()

    private val _selectedProvider = MutableStateFlow<CloudProvider?>(null)
    val selectedProvider: StateFlow<CloudProvider?> = _selectedProvider.asStateFlow()

    private val _uiState = MutableStateFlow<FilesUiState>(FilesUiState.Loading)
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    init {
        observeFiles()
    }

    private fun observeFiles() {
        viewModelScope.launch {
            combine(_selectedCategory, _selectedProvider) { cat, prov -> Pair(cat, prov) }
                .collect { (cat, prov) ->
                    getFilesUseCase(cat, prov)
                        .catch { e -> _uiState.value = FilesUiState.Error(e.message ?: "Error") }
                        .collect { files -> _uiState.value = FilesUiState.Success(files) }
                }
        }
    }

    fun setInitialProvider(providerName: String) {
        val provider = runCatching { CloudProvider.valueOf(providerName.uppercase()) }.getOrNull()
        _selectedProvider.value = provider
    }

    fun setCategory(category: FileCategory?) {
        _selectedCategory.value = category
    }

    fun setProvider(provider: CloudProvider?) {
        _selectedProvider.value = provider
    }

    fun moveFile(fileId: String, targetProvider: CloudProvider) {
        viewModelScope.launch {
            val result = moveFileUseCase(fileId, targetProvider)
            _actionResult.value = if (result.isSuccess) "File moved successfully"
            else result.exceptionOrNull()?.message ?: "Move failed"
        }
    }

    fun deleteFile(fileId: String) {
        viewModelScope.launch {
            val result = fileRepository.deleteFile(fileId)
            _actionResult.value = if (result.isSuccess) "File deleted"
            else result.exceptionOrNull()?.message ?: "Delete failed"
        }
    }

    fun shareFile(fileId: String) {
        viewModelScope.launch {
            val result = fileRepository.getShareLink(fileId)
            _actionResult.value = if (result.isSuccess) result.getOrNull()
            else result.exceptionOrNull()?.message ?: "Share failed"
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }
}
