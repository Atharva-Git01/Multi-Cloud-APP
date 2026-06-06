package com.bbg.cloudapp.domain.usecase

import android.net.Uri
import com.bbg.cloudapp.data.repository.FileRepository
import com.bbg.cloudapp.data.repository.UploadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UploadFileUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    operator fun invoke(uri: Uri): Flow<UploadState> = flow {
        emit(UploadState.Uploading(0))
        val result = fileRepository.uploadFile(uri)
        emit(result)
    }
}
