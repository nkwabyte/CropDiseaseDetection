package com.nkwabyte.cropdiseasedetection.common.navigation.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.R
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.ProfileViewModel
import com.nkwabyte.cropdiseasedetection.ui.theme.CropDiseaseDetectionTheme
import org.koin.compose.koinInject


/**
 * Custom App Drawer composable designed to resemble the provided image.
 *
 * @param currentRoute The currently selected navigation route, used for highlighting the active item.
 * @param onNavigate Lambda function to be invoked when a navigation item is clicked,
 * providing the destination string.
 * @param onCloseDrawer Lambda function to be invoked when the close button is clicked.
 */
@Composable
fun AppDrawer(
    modifier: Modifier = Modifier,
    currentRoute: String,
    onNavigate: (destination: String) -> Unit,
    onCloseDrawer: () -> Unit,
    appViewModel: AppViewModel,
    detectionViewModel: DetectionViewModel,
    profileViewModel: ProfileViewModel,
) {
//    val appState by appViewModel.appState.collectAsState()
//    val detectionState by detectionViewModel.detectionState.collectAsState()
    val profileState by profileViewModel.userProfileState.collectAsState()

    ModalDrawerSheet(
        modifier = modifier.fillMaxWidth(0.9f),
        drawerContainerColor = MaterialTheme.colorScheme.primary,
        drawerShape = RoundedCornerShape(
            topEnd = 24.0.dp,
            bottomEnd = 24.0.dp,
        )
    ) {
        Box(
            modifier = Modifier.height(45.0.dp)
        ){}
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            // Close Button
            IconButton(
                onClick = onCloseDrawer,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.drawer_close_button_description),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // RAIL Logo/User Info
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp, vertical = 8.0.dp)
                    .align(Alignment.CenterStart)
                    .clickable {
                        onNavigate("profile")
                    },
            ) {
                Column (
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.profile_icon),
                        contentDescription = stringResource(R.string.profile_icon_description),
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = profileState.userName.ifEmpty { stringResource(R.string.default_user_name) },
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = profileState.userBio ?: stringResource(R.string.default_user_bio),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Divider after header
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f), thickness = 1.dp
        )

        // Main Navigation Items
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            NavigationDrawerItem(
                label = {
                    Text(
                        stringResource(R.string.select_crop_drawer_item_text_alt),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                selected = currentRoute == "select_crop",
                onClick = {
                    // reset the detection view model and and app view model
                    appViewModel.reset()
                    detectionViewModel.reset()
                    onNavigate("select_crop")
                },
                icon = {
                    Icon(
                        Icons.Outlined.SelectAll,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
               },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    unselectedContainerColor = Color.Transparent,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            /// Home Item (Commented out as per the original code)
//            NavigationDrawerItem(
//                label = {
//                    Text(
//                        stringResource(R.string.home_drawer_item),
//                        color = MaterialTheme.colorScheme.onPrimaryContainer
//                    )
//                },
//                selected = currentRoute == "home",
//                onClick = { onNavigate("home") },
//                icon = {
//                    Icon(
//                        Icons.Outlined.Home,
//                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.onPrimaryContainer
//                    )
//               },
//                colors = NavigationDrawerItemDefaults.colors(
//                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
//                    unselectedContainerColor = Color.Transparent,
//                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
//                    unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
//                )
//            )
            NavigationDrawerItem(
                label = {
                    Text(
                        stringResource(R.string.about_drawer_item),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                selected = currentRoute == "about",
                onClick = { onNavigate("about") },
                icon = {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
               },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    unselectedContainerColor = Color.Transparent,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.privacy_policy_drawer_item), color = MaterialTheme.colorScheme.onPrimaryContainer) },
                selected = currentRoute == "privacy", // Assuming "privacy" is the route for Privacy Policy
                onClick = { onNavigate("privacy") },
                icon = {
                    Icon(
                        Icons.Outlined.Policy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
               },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    unselectedContainerColor = Color.Transparent,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        // Divider before bottom section
        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f), thickness = 1.dp)

        // Spacer to push the remaining content to the bottom
        Spacer(Modifier.weight(1f))

        // Bottom Section (Sign In and Footer Text)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sign In Item
            NavigationDrawerItem(
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Text(
                            stringResource(R.string.sign_in_drawer_item),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        // right angle icon
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = stringResource(R.string.sign_in_icon_description),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                selected = currentRoute == "login",
                onClick = { onNavigate("login") },
                icon = {
                    Image(
                        painter = painterResource(id = R.drawable.profile_icon),
                        contentDescription = stringResource(R.string.profile_icon_description),
                        modifier = Modifier.size(24.dp)
                    )
               },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color.Transparent,
                    unselectedContainerColor = Color.Transparent,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedTextColor = MaterialTheme.colorScheme.tertiary
                )
            )
            Spacer(Modifier.height(16.dp))

            // Website URL and Copyright
            Text(
                modifier = Modifier
                    .padding(vertical = 6.0.dp)
                    .clickable {
                    // open the website URL
                },
                text = stringResource(R.string.social_website_url),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.copy_right_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Preview function for the custom AppDrawer composable.
 */
@Preview(showBackground = true)
@Composable
fun AppDrawerPreview() {
    CropDiseaseDetectionTheme {
        // Provide placeholder strings and a current route for the preview
        AppDrawer(
            currentRoute = "home", // Simulate "Home" being selected
            onNavigate = { destination -> println("Navigating to: $destination") },
            onCloseDrawer = { println("Drawer closed") },
            appViewModel = koinInject<AppViewModel>(),
            detectionViewModel = koinInject<DetectionViewModel>(),
            profileViewModel = koinInject<ProfileViewModel>(),
        )
    }
}