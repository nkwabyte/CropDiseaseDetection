package com.nkwabyte.cropdiseasedetection.common.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import com.nkwabyte.cropdiseasedetection.common.navigation.drawer.AppDrawer
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.AboutScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.HelpScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.HomeScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.LoginScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.PrivacyScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.ProfileScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.SelectCropScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.SplashScreenRoute
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

    val backStack: NavBackStack = rememberNavBackStack(SplashScreenRoute)
    val currentKey = backStack.lastOrNull()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val showDrawer = currentKey !is SplashScreenRoute

    val currentRouteString = when (currentKey) {
        is HomeScreenRoute -> "home"
        is SelectCropScreenRoute -> "select_crop"
        is AboutScreenRoute -> "about"
        is PrivacyScreenRoute -> "privacy"
        is LoginScreenRoute -> "login"
        is ProfileScreenRoute -> "profile"
        is HelpScreenRoute -> "help"
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
                            "home" -> backStack.add(HomeScreenRoute)
                            "select_crop" -> backStack.add(SelectCropScreenRoute)
                            "profile" -> backStack.add(ProfileScreenRoute)
                            "about" -> backStack.add(AboutScreenRoute)
                            "help" -> backStack.add(HelpScreenRoute)
                            "privacy" -> backStack.add(PrivacyScreenRoute)
                            "login" -> backStack.add(LoginScreenRoute)
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
                    backStack = backStack,
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
            backStack = backStack,
            modifier = modifier,
            drawerState = drawerState,
            scope = scope,
            appViewModel = appViewModel,
            detectionViewModel = detectionViewModel,
            profileViewModel = profileViewModel
        )
    }
}
