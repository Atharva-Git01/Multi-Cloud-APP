package com.bbg.cloudapp.navigation

object Routes {
    // Auth graph
    const val AUTH_GRAPH = "auth_graph"
    const val SPLASH = "splash"
    const val ONBOARDING_CARDS = "onboarding_cards"
    const val CREDENTIAL_SETUP = "credential_setup"
    const val PLATFORM_SELECTOR = "platform_selector"
    const val ACCOUNT_CREATION = "account_creation"
    const val ONBOARDING_COMPLETE = "onboarding_complete"

    // Main graph
    const val MAIN_GRAPH = "main_graph"
    const val HOME = "home"
    const val STORAGE_DASHBOARD = "storage_dashboard"
    const val UPLOAD = "upload"
    const val FILES_ALL = "files_all"
    const val FILES_BY_PROVIDER = "files_by_provider/{providerName}"
    const val FILE_DETAIL = "file_detail/{fileId}"
    const val PROFILE = "profile"
    const val PROVIDER_MANAGEMENT = "provider_management"
    const val ROUTING_RULES = "routing_rules"
    const val SETTINGS = "settings"

    // Route builders
    fun filesByProvider(providerName: String) = "files_by_provider/$providerName"
    fun fileDetail(fileId: String) = "file_detail/$fileId"
}
