package com.bbg.cloudapp.core.network.dto

import com.google.gson.annotations.SerializedName

data class ProviderDto(
    @SerializedName("name") val name: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("connected") val connected: Boolean,
    @SerializedName("free_storage_gb") val freeStorageGB: Int,
    @SerializedName("oauth_url") val oauthUrl: String?
)
