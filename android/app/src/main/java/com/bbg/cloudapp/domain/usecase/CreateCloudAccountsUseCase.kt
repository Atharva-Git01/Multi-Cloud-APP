package com.bbg.cloudapp.domain.usecase

import com.bbg.cloudapp.core.model.AccountCreationStatus
import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.data.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CreateCloudAccountsUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) {
    operator fun invoke(
        email: String,
        password: String,
        providers: Set<CloudProvider>
    ): Flow<List<AccountCreationStatus>> {
        return onboardingRepository.createAccounts(email, password, providers)
    }
}
