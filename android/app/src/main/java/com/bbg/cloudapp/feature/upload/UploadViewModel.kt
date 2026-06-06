package com.bbg.cloudapp.feature.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.FileRecord
import com.bbg.cloudapp.data.repository.UploadState
import com.bbg.cloudapp.domain.usecase.UploadFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingFile(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String
)

data class UploadItem(
    val file: PendingFile,
    val state: UploadState = UploadState.Idle
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val uploadFileUseCase: UploadFileUseCase
) : ViewModel() {

    private val _pendingFiles = MutableStateFlow<List<PendingFile>>(emptyList())
    val pendingFiles: StateFlow<List<PendingFile>> = _pendingFiles.asStateFlow()

    private val _uploadItems = MutableStateFlow<List<UploadItem>>(emptyList())
    val uploadItems: StateFlow<List<UploadItem>> = _uploadItems.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    fun addFile(uri: Uri, displayName: String, sizeBytes: Long, mimeType: String) {
        val file = PendingFile(uri, displayName, sizeBytes, mimeType)
        _pendingFiles.value = _pendingFiles.value + file
    }

    fun removeFile(file: PendingFile) {
        _pendingFiles.value = _pendingFiles.value - file
    }

    fun clearFiles() {
        _pendingFiles.value = emptyList()
        _uploadItems.value = emptyList()
    }

    fun startUpload() {
        if (_pendingFiles.value.isEmpty()) return
        _isUploading.value = true
        _uploadItems.value = _pendingFiles.value.map { UploadItem(it) }

        viewModelScope.launch {
            val files = _pendingFiles.value.toList()
            files.forEach { file ->
                uploadFileUseCase(file.uri).collect { state ->
                    _uploadItems.value = _uploadItems.value.map { item ->
                        if (item.file == file) item.copy(state = state) else item
                    }
                }
            }
            _isUploading.value = false
        }
    }
}
