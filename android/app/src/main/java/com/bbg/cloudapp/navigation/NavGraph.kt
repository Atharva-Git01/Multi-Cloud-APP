package com.bbg.cloudapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bbg.cloudapp.feature.dashboard.StorageDashboardScreen
import com.bbg.cloudapp.feature.files.FileDetailScreen
import com.bbg.cloudapp.feature.files.FilesScreen
import com.bbg.cloudapp.feature.home.HomeScreen
import com.bbg.cloudapp.feature.onboarding.AccountCreationScreen
import com.bbg.cloudapp.feature.onboarding.CredentialSetupScreen
import com.bbg.cloudapp.feature.onboarding.OnboardingCardsScreen
import com.bbg.cloudapp.feature.onboarding.OnboardingCompleteScreen
import com.bbg.cloudapp.feature.onboarding.PlatformSelectorScreen
import com.bbg.cloudapp.feature.profile.ProfileScreen
import com.bbg.cloudapp.feature.providers.ProviderManagementScreen
import com.bbg.cloudapp.feature.settings.RoutingRulesScreen
import com.bbg.cloudapp.feature.settings.SettingsScreen
import com.bbg.cloudapp.feature.splash.SplashScreen
import com.bbg.cloudapp.feature.upload.UploadBottomSheet

@Composable
fun BBGNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.AUTH_GRAPH
    ) {
        // Auth graph
        navigation(
            startDestination = Routes.SPLASH,
            route = Routes.AUTH_GRAPH
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(navController = navController)
            }
            composable(Routes.ONBOARDING_CARDS) {
                OnboardingCardsScreen(navController = navController)
            }
            composable(Routes.CREDENTIAL_SETUP) {
                CredentialSetupScreen(navController = navController)
            }
            composable(Routes.PLATFORM_SELECTOR) {
                PlatformSelectorScreen(navController = navController)
            }
            composable(Routes.ACCOUNT_CREATION) {
                AccountCreationScreen(navController = navController)
            }
            composable(Routes.ONBOARDING_COMPLETE) {
                OnboardingCompleteScreen(navController = navController)
            }
        }

        // Main graph
        navigation(
            startDestination = Routes.HOME,
            route = Routes.MAIN_GRAPH
        ) {
            composable(Routes.HOME) {
                HomeScreen(navController = navController)
            }
            composable(Routes.STORAGE_DASHBOARD) {
                StorageDashboardScreen(navController = navController)
            }
            composable(Routes.UPLOAD) {
                UploadBottomSheet(navController = navController)
            }
            composable(Routes.FILES_ALL) {
                FilesScreen(navController = navController)
            }
            composable(
                route = Routes.FILES_BY_PROVIDER,
                arguments = listOf(navArgument("providerName") { type = NavType.StringType })
            ) { backStackEntry ->
                val providerName = backStackEntry.arguments?.getString("providerName") ?: ""
                FilesScreen(navController = navController, filterProvider = providerName)
            }
            composable(
                route = Routes.FILE_DETAIL,
                arguments = listOf(navArgument("fileId") { type = NavType.StringType })
            ) { backStackEntry ->
                val fileId = backStackEntry.arguments?.getString("fileId") ?: ""
                FileDetailScreen(navController = navController, fileId = fileId)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(navController = navController)
            }
            composable(Routes.PROVIDER_MANAGEMENT) {
                ProviderManagementScreen(navController = navController)
            }
            composable(Routes.ROUTING_RULES) {
                RoutingRulesScreen(navController = navController)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(navController = navController)
            }
        }
    }
}
