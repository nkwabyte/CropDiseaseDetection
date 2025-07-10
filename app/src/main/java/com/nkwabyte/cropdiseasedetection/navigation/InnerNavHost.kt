package com.nkwabyte.cropdiseasedetection.navigation

import ProfileScreen
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.nkwabyte.cropdiseasedetection.navigation.routes.AboutScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.DetectionResultScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.HelpScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.HomeScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.LoginScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.PrivacyScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.ProfileScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.RegisterScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.SelectCropScreenRoute
import com.nkwabyte.cropdiseasedetection.navigation.routes.SplashScreenRoute
import com.nkwabyte.cropdiseasedetection.screens.about.AboutScreen
import com.nkwabyte.cropdiseasedetection.screens.about.HelpScreen
import com.nkwabyte.cropdiseasedetection.screens.auth.LoginScreen
import com.nkwabyte.cropdiseasedetection.screens.auth.RegisterScreen
import com.nkwabyte.cropdiseasedetection.screens.home.DetectionResultScreen
import com.nkwabyte.cropdiseasedetection.screens.home.HomeScreen
import com.nkwabyte.cropdiseasedetection.screens.home.SelectCropScreen
import com.nkwabyte.cropdiseasedetection.screens.privacy.PrivacyScreen
import com.nkwabyte.cropdiseasedetection.screens.splash.SplashScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun InnerNavHost(
    backStack: NavBackStack,
    modifier: Modifier = Modifier,
    drawerState: DrawerState,
    scope: CoroutineScope,
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
                        onCropSelected = {},
                        onContinueClick = {
                            backStack.add(HomeScreenRoute)
                        }
                    )
                }
                is HomeScreenRoute -> NavEntry(key) {
                    HomeScreen(
                        onDrawerButtonClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        navigateToResult = { results ->
                            backStack.add(DetectionResultScreenRoute(results))
                        }
                    )
                }
                is DetectionResultScreenRoute -> NavEntry(key) {
                    DetectionResultScreen(
                        onDrawerButtonClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        viewModel = koinViewModel {
                            parametersOf(key.detectionResult)
                        }
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
                        viewModel = koinViewModel { parametersOf(key.id) },
                        onLogoutClick = {},
                        onEditProfileClick = {},
                        onDeleteAccountClick = {},
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