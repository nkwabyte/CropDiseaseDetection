package com.nkwabyte.cropdiseasedetection.ui.screens.splash
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieConstants
import com.nkwabyte.cropdiseasedetection.R
import com.nkwabyte.cropdiseasedetection.ui.theme.CropDiseaseDetectionTheme
import kotlinx.coroutines.delay

/**
 * A splash screen composable function for the Crop Disease Detector app.
 * Displays a full-width image at the top and a Lottie animation with a call-to-action button at the bottom.
 *
 * @param onGetStartedClick Lambda function to be invoked when the "GET STARTED" button is clicked.
 */
@Composable
fun SplashScreen(
    onGetStartedClick: () -> Unit,
) {
    val composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animation))
    var hasNavigated by remember { mutableStateOf(false) }


    // Auto-navigate after 30 seconds if user doesn't act
    LaunchedEffect(Unit) {
        delay(30_000)
        if (!hasNavigated) {
            hasNavigated = true
            onGetStartedClick()
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(0.0.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.corn_on_the_cob),
                contentDescription = stringResource(R.string.corn_corb_image_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize(),
            )

            // Bottom half: Lottie animation and button
            Box(
                modifier = Modifier.padding(0.0.dp),
                contentAlignment = Alignment.BottomCenter
            ){
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // empty space at the top
                    Box(modifier = Modifier.weight(0.40f)){}

                    // content at the bottom
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .weight(0.60f)
                            .background(MaterialTheme.colorScheme.surface),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Title text
                        Text(
                            text = stringResource(R.string.app_name_full).uppercase(),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LottieAnimation(
                            composition = composition.value,
                            modifier = Modifier
                                .size(200.dp)
                                .padding(bottom = 16.dp),
                            iterations = LottieConstants.IterateForever,
                            speed = 1f
                        )
                        // spacer
                        Spacer(modifier = Modifier.height(16.dp))

                        // "GET STARTED" button
                        Button(
                            onClick = onGetStartedClick,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(14.0.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.get_started),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.W500
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview function for the SplashScreen composable.
 */
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    CropDiseaseDetectionTheme {
        SplashScreen(onGetStartedClick = { /* Handle click in preview */ })
    }
}