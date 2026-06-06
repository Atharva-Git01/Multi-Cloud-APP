package com.bbg.cloudapp.data.repository

import com.bbg.cloudapp.core.datastore.AuthDataStore
import com.bbg.cloudapp.core.network.ApiService
import com.bbg.cloudapp.core.network.dto.GoogleCallbackRequest
import com.bbg.cloudapp.core.network.dto.RefreshRequest
import com.bbg.cloudapp.core.network.dto.TokenResponse
import com.bbg.cloudapp.core.network.dto.UserDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    suspend fun loginWithGoogle(authCode: String): Result<TokenResponse>
    suspend fun refreshToken(): Result<TokenResponse>
    suspend fun logout()
    suspend fun getCurrentUser(): Result<UserDto>
    val isLoggedIn: Flow<Boolean>
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val authDataStore: AuthDataStore
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = authDataStore.isLoggedIn

    override suspend fun loginWithGoogle(authCode: String): Result<TokenResponse> {
        return try {
            val response = apiService.googleCallback(GoogleCallbackRequest(authCode))
            if (response.isSuccessful) {
                val tokens = response.body()!!
                authDataStore.saveTokens(tokens.accessToken, tokens.refreshToken)
                Result.success(tokens)
            } else {
                Result.failure(Exception("Login failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<TokenResponse> {
        return try {
            val refreshToken = authDataStore.getRefreshToken()
                ?: return Result.failure(Exception("No refresh token available"))
            val response = apiService.refreshToken(RefreshRequest(refreshToken))
            if (response.isSuccessful) {
                val tokens = response.body()!!
                authDataStore.saveTokens(tokens.accessToken, tokens.refreshToken)
                Result.success(tokens)
            } else {
                authDataStore.clearTokens()
                Result.failure(Exception("Token refresh failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        try {
            apiService.logout()
        } catch (_: Exception) { }
        authDataStore.clearTokens()
    }

    override suspend fun getCurrentUser(): Result<UserDto> {
        return try {
            val response = apiService.getMe()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
