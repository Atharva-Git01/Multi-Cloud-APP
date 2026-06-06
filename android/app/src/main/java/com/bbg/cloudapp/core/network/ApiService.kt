package com.bbg.cloudapp.core.network

import com.bbg.cloudapp.core.network.dto.AccountStatusResponse
import com.bbg.cloudapp.core.network.dto.CreateAccountsRequest
import com.bbg.cloudapp.core.network.dto.DashboardDto
import com.bbg.cloudapp.core.network.dto.FileDto
import com.bbg.cloudapp.core.network.dto.FileListResponse
import com.bbg.cloudapp.core.network.dto.GoogleCallbackRequest
import com.bbg.cloudapp.core.network.dto.MoveFileRequest
import com.bbg.cloudapp.core.network.dto.ProviderDto
import com.bbg.cloudapp.core.network.dto.RefreshRequest
import com.bbg.cloudapp.core.network.dto.ShareLinkResponse
import com.bbg.cloudapp.core.network.dto.TokenResponse
import com.bbg.cloudapp.core.network.dto.UploadResponse
import com.bbg.cloudapp.core.network.dto.UserDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ── Auth ─────────────────────────────────────────────────────────────
    @POST("auth/google/callback")
    suspend fun googleCallback(@Body request: GoogleCallbackRequest): Response<TokenResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<TokenResponse>

    @DELETE("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("auth/me")
    suspend fun getMe(): Response<UserDto>

    // ── Onboarding ───────────────────────────────────────────────────────
    @POST("onboarding/create-accounts")
    suspend fun createAccounts(@Body request: CreateAccountsRequest): Response<AccountStatusResponse>

    @GET("onboarding/status")
    suspend fun getOnboardingStatus(): Response<AccountStatusResponse>

    // ── Files ────────────────────────────────────────────────────────────
    @Multipart
    @POST("files/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("category") category: okhttp3.RequestBody? = null
    ): Response<UploadResponse>

    @GET("files")
    suspend fun getFiles(
        @Query("category") category: String? = null,
        @Query("provider") provider: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<FileListResponse>

    @PATCH("files/{id}/move")
    suspend fun moveFile(
        @Path("id") fileId: String,
        @Body request: MoveFileRequest
    ): Response<FileDto>

    @DELETE("files/{id}")
    suspend fun deleteFile(@Path("id") fileId: String): Response<Unit>

    @POST("files/{id}/share")
    suspend fun shareFile(@Path("id") fileId: String): Response<ShareLinkResponse>

    // ── Storage ──────────────────────────────────────────────────────────
    @GET("storage/dashboard")
    suspend fun getDashboard(): Response<DashboardDto>

    @POST("storage/refresh-quotas")
    suspend fun refreshQuotas(): Response<Unit>

    // ── Providers ────────────────────────────────────────────────────────
    @GET("providers")
    suspend fun getProviders(): Response<List<ProviderDto>>

    @POST("providers/connect")
    suspend fun connectProvider(@Body request: Map<String, String>): Response<ProviderDto>

    @DELETE("providers/{name}")
    suspend fun disconnectProvider(@Path("name") providerName: String): Response<Unit>

    @GET("providers/{name}/files")
    suspend fun getProviderFiles(@Path("name") providerName: String): Response<FileListResponse>

    // ── Routing rules ────────────────────────────────────────────────────
    @GET("routing/rules")
    suspend fun getRoutingRules(): Response<Map<String, String>>

    @PUT("routing/rules")
    suspend fun updateRoutingRules(@Body rules: Map<String, String>): Response<Map<String, String>>
}
