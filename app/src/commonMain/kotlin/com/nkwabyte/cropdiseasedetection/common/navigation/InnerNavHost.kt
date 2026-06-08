package com.nkwabyte.cropdiseasedetection.common.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.*
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.ProfileViewModel
import com.nkwabyte.cropdiseasedetection.ui.screens.about.AboutScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.about.HelpScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.auth.LoginScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.auth.ProfileScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.auth.RegisterScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.home.DetectionResultScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.home.HomeScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.home.SelectCropScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.privacy.PrivacyScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.splash.SplashScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AuthViewModel

@Composable
fun InnerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    drawerState: DrawerState,
    scope: CoroutineScope,
    appViewModel: AppViewModel = koinInject(),
    detectionViewModel: DetectionViewModel = koinInject(),
    profileViewModel: ProfileViewModel = koinInject(),
    authViewModel: AuthViewModel = koinInject()
) {
    NavHost(
        navController = navController,
        startDestination = SplashScreenRoute,
        modifier = modifier
    ) {
        composable<SplashScreenRoute> {
            SplashScreen(
                onGetStartedClick = {
                    navController.popBackStack()
                    navController.navigate(SelectCropScreenRoute)
                }
            )
        }

        composable<SelectCropScreenRoute> {
            SelectCropScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
                onContinueClick = {
                    navController.navigate(HomeScreenRoute)
                },
                appViewModel = appViewModel
            )
        }

        composable<HomeScreenRoute> {
            HomeScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
                navigateToResult = {
                    navController.navigate(DetectionResultScreenRoute)
                },
                appViewModel = appViewModel,
                detectionViewModel = detectionViewModel,
            )
        }

        composable<DetectionResultScreenRoute> {
            DetectionResultScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
                detectionViewModel = detectionViewModel,
                appViewModel = appViewModel,
                onCloseDetection = {
                    navController.popBackStack(route = HomeScreenRoute, inclusive = false)
                },
            )
        }

        composable<AboutScreenRoute> {
            AboutScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
            )
        }

        composable<HelpScreenRoute> {
            HelpScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
            )
        }

        composable<PrivacyScreenRoute> {
            PrivacyScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
            )
        }

        composable<LoginScreenRoute> {
            LoginScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
                onLoginClick = { _, _ ->
                    navController.popBackStack()
                    navController.navigate(HomeScreenRoute)
                },
                onRegisterClick = {
                    navController.popBackStack()
                    navController.navigate(RegisterScreenRoute)
                },
                onForgotPasswordClick = {
                    navController.navigate(ForgotPasswordScreenRoute)
                }
            )
        }

        composable<RegisterScreenRoute> {
            RegisterScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
                onLoginClick = { _, _ ->
                    navController.popBackStack()
                    navController.navigate(LoginScreenRoute)
                }
            )
        }

        composable<ForgotPasswordScreenRoute> {
            com.nkwabyte.cropdiseasedetection.ui.screens.auth.ForgotPasswordScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
                onBackToLoginClick = {
                    navController.popBackStack(route = LoginScreenRoute, inclusive = false)
                },
                authViewModel = authViewModel
            )
        }

        composable<ProfileScreenRoute> {
            ProfileScreen(
                modifier = modifier,
                profileViewModel = profileViewModel,
                onLogoutClick = {},
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
            )
        }

        composable<HistoryScreenRoute> {
            com.nkwabyte.cropdiseasedetection.ui.screens.history.HistoryScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                }
            )
        }

        composable<EncyclopediaScreenRoute> {
            com.nkwabyte.cropdiseasedetection.ui.screens.encyclopedia.EncyclopediaScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                }
            )
        }

        composable<SettingsScreenRoute> {
            com.nkwabyte.cropdiseasedetection.ui.screens.settings.SettingsScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                }
            )
        }
    }
}