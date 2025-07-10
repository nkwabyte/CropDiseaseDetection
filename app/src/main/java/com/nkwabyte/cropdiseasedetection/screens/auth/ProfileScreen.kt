import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.R
import com.nkwabyte.cropdiseasedetection.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.components.ProfileMenuItem
import com.nkwabyte.cropdiseasedetection.components.StatCard
import com.nkwabyte.cropdiseasedetection.navigation.viewmodel.ProfileViewModel
import com.nkwabyte.cropdiseasedetection.ui.theme.CropDiseaseDetectionTheme


/**
 * A composable function that displays a user profile screen.
 * It features a profile image, user name, statistics cards,
 * navigation buttons, and a sign-out option.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param onLogoutClick Lambda function to be invoked when the Sign Out button is clicked.
 * @param onEditProfileClick Lambda function to be invoked when the profile picture is clicked (optional).
 * @param onDeleteAccountClick Lambda function to be invoked for account deletion (not directly in UI, but in signature).
 * @param viewModel The ViewModel providing data for the profile screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onLogoutClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {},
    onDrawerButtonClick: () -> Unit = { /* Default no-op */ },
    viewModel: ProfileViewModel = ProfileViewModel(),
) {
    Scaffold(
        topBar = {
            AppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onDrawerButtonClick = onDrawerButtonClick,
                isHomeScreen = false,
            )
        },
    ){ paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Green Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .align(Alignment.Center)
                ) {
                    // Placeholder for actual profile image or icon
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.profile_icon_description),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }

                // Actual Profile Picture (Circular)
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(4.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .align(Alignment.Center)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.profile_icon),
                        contentDescription = stringResource(R.string.profile_picture_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // White Content Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Name
                Text(
                    text = viewModel.userName.uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatCard(
                        label = stringResource(R.string.profile_success_label),
                        value = viewModel.successRate,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    StatCard(
                        label = "",
                        value = viewModel.detections,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    StatCard(
                        label = stringResource(R.string.profile_detections_label),
                        value = viewModel.detections,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation Buttons
                ProfileMenuItem(
                    text = stringResource(R.string.profile_home_button),
                    onClick = { /* Navigate Home */ }
                )
                ProfileMenuItem(
                    text = stringResource(R.string.profile_additional_help_button),
                    onClick = { /* Go to Help */ }
                )
                ProfileMenuItem(
                    text = stringResource(R.string.profile_about_button),
                    onClick = { /* Go to About */ }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Sign Out Button
                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_sign_out_button),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewProfileScreen() {
    CropDiseaseDetectionTheme {
        ProfileScreen(
            onLogoutClick = { println("Logout clicked") },
            onEditProfileClick = { println("Edit Profile clicked") },
            onDeleteAccountClick = { println("Delete Account clicked") }
        )
    }
}
