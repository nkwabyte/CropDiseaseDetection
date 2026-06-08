package com.nkwabyte.cropdiseasedetection.common.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nkwabyte.cropdiseasedetection.common.navigation.drawer.AppDrawer
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.AboutScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.HelpScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.HomeScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.LoginScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.PrivacyScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.ProfileScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.SelectCropScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.SplashScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.HistoryScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.EncyclopediaScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.SettingsScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    val appViewModel: AppViewModel = koinInject()
    val detectionViewModel: DetectionViewModel = koinInject()
    val profileViewModel: ProfileViewModel = koinInject<ProfileViewModel>()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val showDrawer = currentRoute != "com.nkwabyte.cropdiseasedetection.common.navigation.routes.SplashScreenRoute"

    val currentRouteString = when (currentRoute) {
        "com.nkwabyte.cropdiseasedetection.common.navigation.routes.HomeScreenRoute" -> "home"
        "com.nkwabyte.cropdiseasedetection.common.navigation.routes.SelectCropScreenRoute" -> "select_crop"
        "com.nkwabyte.cropdiseasedetection.common.navigation.routes.AboutScreenRoute" -> "about"
        "com.nkwabyte.cropdiseasedetection.common.navigation.routes.PrivacyScreenRoute" -> "privacy"
        "com.nkwabyte.cropdiseasedetection.common.navigation.routes.LoginScreenRoute" -> "login"
        "com.nkwabyte.cropdiseasedetection.common.navigation.routes.ProfileScreenRoute" -> "profile"
        "com.nkwabyte.cropdiseasedetection.common.navigation.routes.HelpScreenRoute" -> "help"
        "com.nkwabyte.cropdiseasedetection.common.navigation.routes.HistoryScreenRoute" -> "history"
        "com.nkwabyte.cropdiseasedetection.common.navigation.routes.EncyclopediaScreenRoute" -> "encyclopedia"
        "com.nkwabyte.cropdiseasedetection.common.navigation.routes.SettingsScreenRoute" -> "settings"
        else -> ""
    }

    if (showDrawer) {
        ModalNavigationDrawer(
            drawerContent = {
                AppDrawer(
                    currentRoute = currentRouteString,
                    onNavigate = { destination ->
                        scope.launch { drawerState.close() }
                        when (destination) {
                            "home" -> navController.navigate(HomeScreenRoute)
                            "select_crop" -> navController.navigate(SelectCropScreenRoute)
                            "profile" -> navController.navigate(ProfileScreenRoute)
                            "about" -> navController.navigate(AboutScreenRoute)
                            "help" -> navController.navigate(HelpScreenRoute)
                            "privacy" -> navController.navigate(PrivacyScreenRoute)
                            "login" -> navController.navigate(LoginScreenRoute)
                            "history" -> navController.navigate(HistoryScreenRoute)
                            "encyclopedia" -> navController.navigate(EncyclopediaScreenRoute)
                            "settings" -> navController.navigate(SettingsScreenRoute)
                        }
                    },
                    onCloseDrawer = { scope.launch { drawerState.close() } },
                    appViewModel = appViewModel,
                    detectionViewModel = detectionViewModel,
                    profileViewModel = profileViewModel,
                )
            },
            drawerState = drawerState,
            content = {
                InnerNavHost(
                    navController = navController,
                    modifier = modifier,
                    drawerState = drawerState,
                    scope = scope,
                    appViewModel = appViewModel,
                    detectionViewModel = detectionViewModel,
                    profileViewModel = profileViewModel,
                )
            }
        )
    } else {
        InnerNavHost(
            navController = navController,
            modifier = modifier,
            drawerState = drawerState,
            scope = scope,
            appViewModel = appViewModel,
            detectionViewModel = detectionViewModel,
            profileViewModel = profileViewModel
        )
    }
}
