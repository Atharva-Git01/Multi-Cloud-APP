package com.bbg.cloudapp.data.repository

import com.bbg.cloudapp.core.model.AccountCreationStatus
import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.CreationState
import com.bbg.cloudapp.core.network.ApiService
import com.bbg.cloudapp.core.network.dto.CreateAccountsRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface OnboardingRepository {
    fun createAccounts(
        email: String,
        password: String,
        providers: Set<CloudProvider>
    ): Flow<List<AccountCreationStatus>>
}

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : OnboardingRepository {

    override fun createAccounts(
        email: String,
        password: String,
        providers: Set<CloudProvider>
    ): Flow<List<AccountCreationStatus>> = flow {
        val providerList = providers.toList()

        // Emit initial PENDING state for all providers
        val statuses = providerList.map { provider ->
            AccountCreationStatus(provider = provider, state = CreationState.PENDING)
        }.toMutableList()
        emit(statuses.toList())

        // Start the creation request
        val creatingStatuses = providerList.map { provider ->
            AccountCreationStatus(provider = provider, state = CreationState.CREATING)
        }.toMutableList()
        emit(creatingStatuses.toList())

        try {
            val request = CreateAccountsRequest(
                email = email,
                password = password,
                providers = providerList.map { it.name }
            )
            val response = apiService.createAccounts(request)

            if (response.isSuccessful) {
                val statusDtos = response.body()?.statuses ?: emptyList()
                val finalStatuses = providerList.map { provider ->
                    val dto = statusDtos.find {
                        it.provider.equals(provider.name, ignoreCase = true)
                    }
                    val state = when (dto?.state?.uppercase()) {
                        "DONE" -> CreationState.DONE
                        "FAILED" -> CreationState.FAILED
                        else -> CreationState.DONE
                    }
                    AccountCreationStatus(
                        provider = provider,
                        state = state,
                        errorMessage = dto?.errorMessage
                    )
                }
                emit(finalStatuses)
            } else {
                // Mark all as failed
                val failedStatuses = providerList.map { provider ->
                    AccountCreationStatus(
                        provider = provider,
                        state = CreationState.FAILED,
                        errorMessage = "Server error: ${response.code()}"
                    )
                }
                emit(failedStatuses)
            }
        } catch (e: Exception) {
            // Network error — poll for status
            delay(2000)
            try {
                val statusResponse = apiService.getOnboardingStatus()
                if (statusResponse.isSuccessful) {
                    val statusDtos = statusResponse.body()?.statuses ?: emptyList()
                    val polledStatuses = providerList.map { provider ->
                        val dto = statusDtos.find {
                            it.provider.equals(provider.name, ignoreCase = true)
                        }
                        val state = when (dto?.state?.uppercase()) {
                            "DONE" -> CreationState.DONE
                            "FAILED" -> CreationState.FAILED
                            else -> CreationState.FAILED
                        }
                        AccountCreationStatus(
                            provider = provider,
                            state = state,
                            errorMessage = dto?.errorMessage ?: e.message
                        )
                    }
                    emit(polledStatuses)
                } else {
                    val failedStatuses = providerList.map { provider ->
                        AccountCreationStatus(
                            provider = provider,
                            state = CreationState.FAILED,
                            errorMessage = e.message
                        )
                    }
                    emit(failedStatuses)
                }
            } catch (_: Exception) {
                val failedStatuses = providerList.map { provider ->
                    AccountCreationStatus(
                        provider = provider,
                        state = CreationState.FAILED,
                        errorMessage = e.message
                    )
                }
                emit(failedStatuses)
            }
        }
    }
}
