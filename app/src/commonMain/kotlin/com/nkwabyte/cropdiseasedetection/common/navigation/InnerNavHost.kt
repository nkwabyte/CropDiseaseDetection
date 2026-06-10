package com.nkwabyte.cropdiseasedetection.common.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.*
import com.nkwabyte.cropdiseasedetection.ui.screens.home.RecommendationScreen
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
import com.nkwabyte.cropdiseasedetection.common.model.UserRole
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect

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
    val firebaseUser by Firebase.auth.authStateChanged.collectAsState(initial = Firebase.auth.currentUser)

    LaunchedEffect(firebaseUser) {
        val user = firebaseUser
        if (user != null) {
            val email = user.email ?: ""
            val name = user.displayName?.ifEmpty { null }
                ?: email.substringBefore("@").ifEmpty { "Farmer" }

            profileViewModel.updateProfile(
                userId = user.uid,
                userName = name,
                userEmail = email
            )
            appViewModel.loadUserRole()
        } else {
            profileViewModel.resetProfile()
            appViewModel.setUserRole(UserRole.FARMER)
        }
    }

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
                onNavigateToRecommendations = {
                    navController.navigate(RecommendationScreenRoute)
                }
            )
        }

        composable<RecommendationScreenRoute> {
            RecommendationScreen(
                detectionViewModel = detectionViewModel,
                onBack = { navController.popBackStack() },
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                }
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
                onLoginSuccess = {
                    navController.navigate(SelectCropScreenRoute) {
                        popUpTo(LoginScreenRoute) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(RegisterScreenRoute) {
                        popUpTo(LoginScreenRoute) { inclusive = false }
                    }
                },
                onForgotPasswordClick = {
                    navController.navigate(ForgotPasswordScreenRoute)
                },
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
                authViewModel = authViewModel
            )
        }

        composable<RegisterScreenRoute> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(SelectCropScreenRoute) {
                        popUpTo(RegisterScreenRoute) { inclusive = true }
                    }
                },
                onLoginClick = {
                    navController.navigate(LoginScreenRoute) {
                        popUpTo(RegisterScreenRoute) { inclusive = true }
                    }
                },
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
                authViewModel = authViewModel
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
                onLogoutClick = {
                    scope.launch {
                        try {
                            Firebase.auth.signOut()
                            profileViewModel.resetProfile()
                            navController.navigate(LoginScreenRoute) {
                                popUpTo(SelectCropScreenRoute) { inclusive = false }
                            }
                        } catch (e: Exception) {
                            // Ignore signout error
                        }
                    }
                },
                onSignInClick = {
                    navController.navigate(LoginScreenRoute)
                },
                onPrivacyPolicyClick = {
                    navController.navigate(PrivacyScreenRoute)
                },
                onHelpSupportClick = {
                    navController.navigate(HelpScreenRoute)
                },
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
            )
        }

        composable<HistoryScreenRoute> {
            com.nkwabyte.cropdiseasedetection.ui.screens.history.HistoryScreen(
                onDrawerButtonClick = {
                    scope.launch { drawerState.open() }
                },
                onSignInClick = {
                    navController.navigate(LoginScreenRoute)
                },
                onStartScanClick = {
                    navController.navigate(SelectCropScreenRoute)
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
                },
                appViewModel = appViewModel
            )
        }
    }
}