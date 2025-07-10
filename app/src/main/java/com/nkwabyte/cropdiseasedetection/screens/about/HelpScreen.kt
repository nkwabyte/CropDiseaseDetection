package com.nkwabyte.cropdiseasedetection.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.R
import com.nkwabyte.cropdiseasedetection.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.components.SocialMediaButton
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
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top section for the RAIL logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.rail_logo),
                    contentDescription = stringResource(R.string.rail_logo_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(0.7f)
                )
            }

            // Bottom green section for social media links
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.help_title),
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
                        text = stringResource(R.string.social_facebook_handle),
                        onClick = { /* Handle Facebook click */ }
                    )
                    SocialMediaButton(
                        icon = Icons.Default.Share,
                        text = stringResource(R.string.social_twitter_handle),
                        onClick = { /* Handle Twitter click */ }
                    )
                    SocialMediaButton(
                        icon = Icons.Default.Language,
                        text = stringResource(R.string.social_linkedin_handle),
                        onClick = { /* Handle LinkedIn click */ }
                    )
                    SocialMediaButton(
                        icon = Icons.Default.PlayArrow,
                        text = stringResource(R.string.social_youtube_handle),
                        onClick = { /* Handle YouTube click */ }
                    )
                    SocialMediaButton(
                        icon = Icons.Default.Language,
                        text = stringResource(R.string.social_website_url),
                        onClick = { /* Handle Website click */ }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHelpScreen() {
    CropDiseaseDetectionTheme {
        HelpScreen()
    }
}
