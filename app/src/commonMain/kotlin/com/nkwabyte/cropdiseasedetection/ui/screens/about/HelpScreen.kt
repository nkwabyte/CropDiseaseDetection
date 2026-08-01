package com.nkwabyte.cropdiseasedetection.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.generated.resources.*
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.ui.components.SocialMediaButton
import com.nkwabyte.cropdiseasedetection.ui.theme.CropDiseaseDetectionTheme

/**
 * A composable function that displays the Help screen, providing information
 * on how to follow RAIL KNUST on social media platforms.
 *
 * @param modifier Modifier to be applied to the layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    modifier: Modifier = Modifier,
    onDrawerButtonClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            AppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.app_name),
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
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding()
                )
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top section for the RAIL logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.2f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.rail_logo),
                    contentDescription = stringResource(Res.string.rail_logo_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }

            // Bottom green section for social media links
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Contact Us Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Contact Us",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Contact Us",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            Text(
                                text = "Responsible Artificial Intelligence Lab (RAIL)",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Text(
                                text = "Kwame Nkrumah University of Science and Technology (KNUST)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            ContactItem(
                                icon = Icons.Default.Phone,
                                label = "Phone",
                                value = "+233 20 753 4396"
                            )
                            ContactItem(
                                icon = Icons.Default.Email,
                                label = "Email",
                                value = "rail@knust.edu.gh"
                            )
                            ContactItem(
                                icon = Icons.Default.LocationOn,
                                label = "Location",
                                value = "College of Engineering, Research Hill, KNUST, Kumasi, Ghana"
                            )
                            ContactItem(
                                icon = Icons.Default.Language,
                                label = "Website",
                                value = "rail.knust.edu.gh"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(Res.string.help_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Social Media Buttons
                    SocialMediaButton(
                        icon = Icons.Default.Facebook,
                        text = stringResource(Res.string.social_facebook_handle),
                        onClick = { /* Handle Facebook click */ }
                    )
                    SocialMediaButton(
                        icon = Icons.Default.Share,
                        text = stringResource(Res.string.social_twitter_handle),
                        onClick = { /* Handle Twitter click */ }
                    )
                    SocialMediaButton(
                        icon = Icons.Default.Language,
                        text = stringResource(Res.string.social_linkedin_handle),
                        onClick = { /* Handle LinkedIn click */ }
                    )
                    SocialMediaButton(
                        icon = Icons.Default.PlayArrow,
                        text = stringResource(Res.string.social_youtube_handle),
                        onClick = { /* Handle YouTube click */ }
                    )
                    SocialMediaButton(
                        icon = Icons.Default.Language,
                        text = stringResource(Res.string.social_website_url),
                        onClick = { /* Handle Website click */ }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            )
        }
    }
}

@Composable
private fun ContactItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Preview
@Composable
fun PreviewHelpScreen() {
    CropDiseaseDetectionTheme {
        HelpScreen()
    }
}
