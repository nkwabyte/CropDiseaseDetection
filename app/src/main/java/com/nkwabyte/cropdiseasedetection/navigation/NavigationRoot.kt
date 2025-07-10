package com.nkwabyte.cropdiseasedetection.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import com.nkwabyte.cropdiseasedetection.navigation.drawer.AppDrawer
import com.nkwabyte.cropdiseasedetection.navigation.routes.AboutScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.HelpScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.HomeScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.LoginScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.PrivacyScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.ProfileScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.SelectCropScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.SplashScreenRoute
import com.nkwabyte.cropdiseasedetection.screens.home.SelectCropScreen
import kotlinx.coroutines.launch



@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    val backStack: NavBackStack = rememberNavBackStack(SplashScreenRoute)
    val currentKey = backStack.lastOrNull()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val showDrawer = currentKey !is SplashScreenRoute

    // Determine the current route string for the AppDrawer
    val currentRouteString = when (currentKey) {
        is HomeScreenRoute -> "home"
        is SelectCropScreenRoute -> "select_crop"
        is AboutScreenRoute -> "about"
        is PrivacyScreenRoute -> "privacy"
        is LoginScreenRoute -> "login"
        is ProfileScreenRoute -> "profile"
        is HelpScreenRoute -> "help"
        // Add other routes as needed
        else -> ""
    }

    if (showDrawer) {
        ModalNavigationDrawer(
            drawerContent = {
                AppDrawer(
                    currentRoute = currentRouteString,
                    onNavigate = { destination ->
                        scope.launch { drawerState.close() }
                        // Handle navigation based on destination string
                        when (destination) {
                            "home" -> { backStack.add(HomeScreenRoute) }
                            "select_crop" -> { backStack.add(SelectCropScreenRoute) }
                            "profile" -> { backStack.add(ProfileScreenRoute("user123")) }
                            "about" -> { backStack.add(AboutScreenRoute) }
                            "help" -> { backStack.add(HelpScreenRoute) }
                            "privacy" -> { backStack.add(PrivacyScreenRoute) }
                            "login" -> { backStack.add(LoginScreenRoute) }
                            // Add other navigation cases here if you expand your drawer
                        }
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            },
            drawerState = drawerState
        ) {
            InnerNavHost(backStack, modifier, drawerState, scope)
        }
    } else {
        InnerNavHost(backStack, modifier, drawerState, scope)
    }
}