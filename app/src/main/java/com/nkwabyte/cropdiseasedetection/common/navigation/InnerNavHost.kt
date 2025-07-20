package com.nkwabyte.cropdiseasedetection.common.navigation

import com.nkwabyte.cropdiseasedetection.ui.screens.auth.ProfileScreen
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.AboutScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.DetectionResultScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.HelpScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.HomeScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.LoginScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.PrivacyScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.ProfileScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.RegisterScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.SelectCropScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.routes.SplashScreenRoute
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.ProfileViewModel
import com.nkwabyte.cropdiseasedetection.ui.screens.about.AboutScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.about.HelpScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.auth.LoginScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.auth.RegisterScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.home.DetectionResultScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.home.HomeScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.home.SelectCropScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.privacy.PrivacyScreen
import com.nkwabyte.cropdiseasedetection.ui.screens.splash.SplashScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


@Composable
fun InnerNavHost(
    backStack: NavBackStack,
    modifier: Modifier = Modifier,
    drawerState: DrawerState,
    scope: CoroutineScope,
    appViewModel: AppViewModel = koinInject<AppViewModel>(),
    detectionViewModel: DetectionViewModel = koinInject<DetectionViewModel>(),
    profileViewModel: ProfileViewModel = koinInject<ProfileViewModel>()
) {
    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        entryDecorators = listOf(
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberSceneSetupNavEntryDecorator()
        ),
        entryProvider = { key ->
            when (key) {
                is SplashScreenRoute -> {
                    NavEntry(key) {
                        SplashScreen(
                            onGetStartedClick = {
                                backStack.add(SelectCropScreenRoute)
                                backStack.remove(SplashScreenRoute)
                            }
                        )
                    }
                }
                is SelectCropScreenRoute -> NavEntry(key) {
                    SelectCropScreen(
                        onDrawerButtonClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onContinueClick = {
                            backStack.add(HomeScreenRoute)
                        },
                        appViewModel = appViewModel
                    )
                }
                is HomeScreenRoute -> NavEntry(key) {
                    HomeScreen(
                        onDrawerButtonClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        navigateToResult = {
                            backStack.add(DetectionResultScreenRoute)
                        },
                        appViewModel = appViewModel,
                        detectionViewModel = detectionViewModel,
                    )
                }
                is DetectionResultScreenRoute -> NavEntry(key) {
                    DetectionResultScreen(
                        onDrawerButtonClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        detectionViewModel = detectionViewModel,
                        appViewModel = appViewModel,
                        onCloseDetection = {
                            backStack.remove(DetectionResultScreenRoute)
                            backStack.remove(HomeScreenRoute)
                        },
                    )
                }
                is AboutScreenRoute -> NavEntry(key) {
                    AboutScreen(
                        onDrawerButtonClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                    )
                }
                is HelpScreenRoute -> NavEntry(key) {
                    HelpScreen(
                        onDrawerButtonClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                    )
                }
                is PrivacyScreenRoute -> NavEntry(key) {
                    PrivacyScreen(
                        onDrawerButtonClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                    )
                }
                is LoginScreenRoute -> NavEntry(key) {
                    LoginScreen(
                        onDrawerButtonClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onLoginClick = { _, _ ->
                            backStack.add(HomeScreenRoute)
                            backStack.remove(LoginScreenRoute)
                        },
                        onRegisterClick = {
                            backStack.add(RegisterScreenRoute)
                            backStack.remove(LoginScreenRoute)
                        }
                    )
                }
                is RegisterScreenRoute -> NavEntry(key) {
                    RegisterScreen(
                        onDrawerButtonClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onLoginClick = { _, _ ->
                            backStack.add(LoginScreenRoute)
                            backStack.remove(RegisterScreenRoute)
                        }
                    )
                }
                is ProfileScreenRoute -> NavEntry(key) {
                    ProfileScreen(
                        modifier = modifier,
                        profileViewModel = profileViewModel,
                        onLogoutClick = {},
                        onDrawerButtonClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                    )
                }
                else -> throw RuntimeException("Invalid NavKey.")
            }
        }
    )
}