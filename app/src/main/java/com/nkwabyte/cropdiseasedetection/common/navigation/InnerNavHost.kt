package com.nkwabyte.cropdiseasedetection.common.navigation

import com.nkwabyte.cropdiseasedetection.ui.screens.auth.ProfileScreen
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
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
import org.koin.compose.koinInject


@Composable
fun InnerNavHost(
    backStack: NavBackStack<NavKey>,
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
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = { key ->
            when (key) {
                is SplashScreenRoute -> NavEntry(key) {
                    SplashScreen(
                        onGetStartedClick = {
                            backStack.removeLastOrNull()
                            backStack.add(SelectCropScreenRoute)
                        }
                    )
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
                            backStack.removeLastOrNull()
                            backStack.removeLastOrNull()
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
                            backStack.removeLastOrNull()
                            backStack.add(HomeScreenRoute)
                        },
                        onRegisterClick = {
                            backStack.removeLastOrNull()
                            backStack.add(RegisterScreenRoute)
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
                            backStack.removeLastOrNull()
                            backStack.add(LoginScreenRoute)
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

                else -> NavEntry(key) {
                    // Fallback for unknown routes
                }
            }
        }
    )
}