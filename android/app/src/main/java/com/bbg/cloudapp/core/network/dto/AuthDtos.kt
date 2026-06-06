package com.bbg.cloudapp.core.network.dto

import com.google.gson.annotations.SerializedName

data class GoogleCallbackRequest(
    @SerializedName("auth_code") val authCode: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("token_type") val tokenType: String = "Bearer"
)

data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("display_name") val displayName: String
)
