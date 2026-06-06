package com.bbg.cloudapp.core.network.dto

import com.google.gson.annotations.SerializedName

data class CreateAccountsRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("providers") val providers: List<String>
)

data class AccountStatusDto(
    @SerializedName("provider") val provider: String,
    @SerializedName("state") val state: String,
    @SerializedName("error_message") val errorMessage: String?
)

data class AccountStatusResponse(
    @SerializedName("statuses") val statuses: List<AccountStatusDto>
)
