package com.bbg.cloudapp.domain.usecase

import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.FileCategory
import com.bbg.cloudapp.core.model.FileRecord
import com.bbg.cloudapp.data.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFilesUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    operator fun invoke(
        category: FileCategory? = null,
        provider: CloudProvider? = null
    ): Flow<List<FileRecord>> {
        return fileRepository.getFiles(category, provider)
    }
}
