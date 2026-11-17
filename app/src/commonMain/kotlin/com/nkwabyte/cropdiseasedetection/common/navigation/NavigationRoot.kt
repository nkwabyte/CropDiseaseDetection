package com.nkwabyte.cropdiseasedetection.common.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nkwabyte.cropdiseasedetection.common.model.UserRole
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.*
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AuthViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.HistoryViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.ProfileViewModel
import com.nkwabyte.cropdiseasedetection.generated.resources.*
import com.nkwabyte.cropdiseasedetection.ui.screens.about.AboutScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.about.HelpScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.auth.ForgotPasswordScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.auth.ProfileScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.encyclopedia.DiseaseDetailScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.encyclopedia.EncyclopediaScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.history.HistoryDetailScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.history.HistoryScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.history.historyKey
import com.nkwabyte.cropdiseasedetection.ui.screens.home.DetectionResultScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.home.HomeScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.home.RecommendationScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.privacy.PrivacyScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.settings.SettingsScreen
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.reflect.KClass

private data class Tab(
    val graph: Any,
    val graphClass: KClass<*>,
    val label: StringResource,
    val icon: ImageVector,
)

private val tabs = listOf(
    Tab(DiagnosticsTabRoute, DiagnosticsTabRoute::class, Res.string.tab_diagnostics, Icons.Default.MonitorHeart),
    Tab(EncyclopediaTabRoute, EncyclopediaTabRoute::class, Res.string.tab_encyclopedia, Icons.AutoMirrored.Filled.MenuBook),
    Tab(HistoryTabRoute, HistoryTabRoute::class, Res.string.tab_history, Icons.Default.History),
    Tab(SettingsTabRoute, SettingsTabRoute::class, Res.string.tab_settings, Icons.Default.Settings),
    Tab(AccountTabRoute, AccountTabRoute::class, Res.string.tab_account, Icons.Default.AccountCircle),
)

/** Switches to [graph], keeping every tab's own back stack (like iOS TabView). */
private fun NavHostController.selectTab(graph: Any) {
    navigate(graph) {
        popUpTo(this@selectTab.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The app shell — a bottom tab bar with one nested navigation graph per tab, mirroring
 * the iOS `TabView` in `ContentView.swift`. Details are pushed pages inside each tab.
 */
@Composable
fun NavigationRoot(modifier: Modifier = Modifier) {
    val appViewModel: AppViewModel = koinInject()
    val detectionViewModel: DetectionViewModel = koinInject()
    val profileViewModel: ProfileViewModel = koinInject()
    val historyViewModel: HistoryViewModel = koinInject()
    val authViewModel: AuthViewModel = koinInject()

    // Keep the profile and role in step with the Firebase session.
    val firebaseUser by Firebase.auth.authStateChanged.collectAsState(initial = Firebase.auth.currentUser)
    LaunchedEffect(firebaseUser) {
        val user = firebaseUser
        if (user != null) {
            val email = user.email ?: ""
            val name = user.displayName?.ifEmpty { null }
                ?: email.substringBefore("@").ifEmpty { "Farmer" }
            profileViewModel.updateProfile(userId = user.uid, userName = name, userEmail = email)
            appViewModel.loadUserRole()
        } else {
            profileViewModel.resetProfile()
            appViewModel.setUserRole(UserRole.FARMER)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = destination?.hierarchy?.any { it.hasRoute(tab.graphClass) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.selectTab(tab.graph) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = {
                            Text(stringResource(tab.label), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = DiagnosticsTabRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            val back: () -> Unit = { navController.popBackStack() }
            val openDisease: (Int) -> Unit = { navController.navigate(DiseaseDetailScreenRoute(it)) }

            // Shared pages that any tab may push.
            composable<DiseaseDetailScreenRoute> { entry ->
                DiseaseDetailScreen(diseaseId = entry.toRoute<DiseaseDetailScreenRoute>().diseaseId, onBack = back)
            }
            composable<PrivacyScreenRoute> { PrivacyScreen(onBack = back) }

            navigation<DiagnosticsTabRoute>(startDestination = HomeScreenRoute) {
                composable<HomeScreenRoute> {
                    HomeScreen(
                        appViewModel = appViewModel,
                        detectionViewModel = detectionViewModel,
                        navigateToResult = { navController.navigate(DetectionResultScreenRoute) },
                    )
                }
                composable<DetectionResultScreenRoute> {
                    DetectionResultScreen(
                        detectionViewModel = detectionViewModel,
                        appViewModel = appViewModel,
                        onBack = back,
                        onDone = {
                            appViewModel.reset()
                            detectionViewModel.reset()
                            navController.popBackStack(HomeScreenRoute, inclusive = false)
                        },
                        onOpenDisease = openDisease,
                        onViewTreatment = { navController.navigate(RecommendationScreenRoute) },
                    )
                }
                composable<RecommendationScreenRoute> {
                    val state by detectionViewModel.detectionState.collectAsState()
                    RecommendationScreen(results = state.results, appViewModel = appViewModel, onBack = back)
                }
            }

            navigation<EncyclopediaTabRoute>(startDestination = EncyclopediaScreenRoute) {
                composable<EncyclopediaScreenRoute> { EncyclopediaScreen(onOpenDisease = openDisease) }
            }

            navigation<HistoryTabRoute>(startDestination = HistoryScreenRoute) {
                composable<HistoryScreenRoute> {
                    HistoryScreen(
                        onOpenRecord = { navController.navigate(HistoryDetailScreenRoute(it)) },
                        onSignInClick = { navController.selectTab(AccountTabRoute) },
                        historyViewModel = historyViewModel,
                    )
                }
                composable<HistoryDetailScreenRoute> { entry ->
                    val key = entry.toRoute<HistoryDetailScreenRoute>().recordKey
                    HistoryDetailScreen(
                        recordKey = key,
                        onBack = back,
                        onOpenDisease = openDisease,
                        onViewTreatment = { navController.navigate(HistoryRecommendationScreenRoute(key)) },
                        historyViewModel = historyViewModel,
                    )
                }
                composable<HistoryRecommendationScreenRoute> { entry ->
                    val key = entry.toRoute<HistoryRecommendationScreenRoute>().recordKey
                    val state by historyViewModel.historyState.collectAsState()
                    val record = state.records.firstOrNull { it.historyKey() == key }
                    RecommendationScreen(
                        results = record?.matchingResults.orEmpty(),
                        appViewModel = appViewModel,
                        onBack = back,
                    )
                }
            }

            navigation<SettingsTabRoute>(startDestination = SettingsScreenRoute) {
                composable<SettingsScreenRoute> {
                    SettingsScreen(
                        onOpenAbout = { navController.navigate(AboutScreenRoute) },
                        onOpenHelp = { navController.navigate(HelpScreenRoute) },
                        appViewModel = appViewModel,
                    )
                }
                composable<AboutScreenRoute> { AboutScreen(onBack = back) }
                composable<HelpScreenRoute> { HelpScreen(onBack = back) }
            }

            navigation<AccountTabRoute>(startDestination = ProfileScreenRoute) {
                composable<ProfileScreenRoute> {
                    ProfileScreen(
                        profileViewModel = profileViewModel,
                        authViewModel = authViewModel,
                        onOpenPrivacy = { navController.navigate(PrivacyScreenRoute) },
                        onForgotPassword = { navController.navigate(ForgotPasswordScreenRoute) },
                    )
                }
                composable<ForgotPasswordScreenRoute> {
                    ForgotPasswordScreen(onBack = back, onBackToLoginClick = back, authViewModel = authViewModel)
                }
            }
        }
    }
}
