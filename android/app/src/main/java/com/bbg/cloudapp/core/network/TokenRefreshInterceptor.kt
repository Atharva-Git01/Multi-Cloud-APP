package com.bbg.cloudapp.core.network

import com.bbg.cloudapp.core.datastore.AuthDataStore
import com.bbg.cloudapp.core.network.dto.RefreshRequest
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject

class TokenRefreshInterceptor @Inject constructor(
    private val authDataStore: AuthDataStore,
    private val gson: Gson,
    private val baseUrl: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalResponse = chain.proceed(originalRequest)

        if (originalResponse.code != 401) return originalResponse

        originalResponse.close()

        val refreshToken = runBlocking { authDataStore.getRefreshToken() }
            ?: return originalResponse

        val newTokens = tryRefresh(chain, refreshToken) ?: run {
            runBlocking { authDataStore.clearTokens() }
            return originalResponse
        }

        runBlocking { authDataStore.saveTokens(newTokens.first, newTokens.second) }

        val retryRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer ${newTokens.first}")
            .build()
        return chain.proceed(retryRequest)
    }

    private fun tryRefresh(chain: Interceptor.Chain, refreshToken: String): Pair<String, String>? {
        return try {
            val body = gson.toJson(RefreshRequest(refreshToken))
                .toRequestBody("application/json".toMediaType())
            val refreshRequest = Request.Builder()
                .url("${baseUrl}auth/refresh")
                .post(body)
                .build()
            val response = chain.proceed(refreshRequest)
            if (!response.isSuccessful) return null
            val responseBody = response.body?.string() ?: return null
            val tokenMap = gson.fromJson(responseBody, Map::class.java)
            val access = tokenMap["access_token"] as? String ?: return null
            val refresh = tokenMap["refresh_token"] as? String ?: refreshToken
            Pair(access, refresh)
        } catch (e: Exception) {
            null
        }
    }
}
