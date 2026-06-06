package com.bbg.cloudapp.domain.usecase

import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.data.repository.FileRepository
import javax.inject.Inject

class MoveFileUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(fileId: String, targetProvider: CloudProvider): Result<Unit> {
        return fileRepository.moveFile(fileId, targetProvider)
    }
}
