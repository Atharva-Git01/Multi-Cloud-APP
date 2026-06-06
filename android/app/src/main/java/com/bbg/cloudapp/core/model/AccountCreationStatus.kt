package com.bbg.cloudapp.core.model

enum class CreationState { PENDING, CREATING, DONE, FAILED }

data class AccountCreationStatus(
    val provider: CloudProvider,
    val state: CreationState,
    val errorMessage: String? = null
)
