package com.nkwabyte.cropdiseasedetection.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Step 1: Define the GradientSnackBar composable and its parameters
@Composable
fun GradientSnackBar(
    modifier: Modifier = Modifier,
    message: String = "Action completed successfully!",
    actionLabel: String? = "Dismiss",
    onAction: () -> Unit = {},
//    duration: Long = 3000L, // Default 3-second duration
) {
    val (alphaAnim, scaleAnim) = animateFadeAndScaleSnackBar()

    // Step 2: Set up the Card container with styling and animations
    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight()
            .alpha(alphaAnim)
            .scale(scaleAnim)
            .padding(SnackBarConstants.PADDING),
        shape = RoundedCornerShape(SnackBarConstants.CORNER_RADIUS),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        // Step 3: Build the Row layout with gradient background and content
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
                    )
                )
                .padding(SnackBarConstants.CONTENT_PADDING)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Nested Row for icon and message
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Info",
                    tint = Color.White,
                    modifier = Modifier
                        .size(SnackBarConstants.ICON_SIZE)
                        .padding(end = SnackBarConstants.ICON_PADDING)
                )
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = SnackBarConstants.MESSAGE_FONT_SIZE,
                    textAlign = TextAlign.Start,
                    lineHeight = SnackBarConstants.MESSAGE_FONT_SIZE * 1.2f,
                    modifier = Modifier.padding(end = SnackBarConstants.SPACING)
                )
            }
            // Optional action button
            actionLabel?.let {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.height(SnackBarConstants.BUTTON_HEIGHT)
                ) {
                    Text(
                        text = it,
                        color = Color.White,
                        fontSize = SnackBarConstants.ACTION_FONT_SIZE,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Step 4: Implement fade and scale animations
@Composable
private fun animateFadeAndScaleSnackBar(): Pair<Float, Float> {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = SnackBarConstants.ALPHA_DURATION,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = SnackBarConstants.SCALE_DURATION,
            easing = FastOutSlowInEasing
        ),
        label = "scale"
    )
    return alpha to scale
}

// Step 5: Define styling constants and demo composable
private object SnackBarConstants {
    val PADDING = 8.dp
    val CONTENT_PADDING = 12.dp
    val CORNER_RADIUS = 12.dp
    val ICON_SIZE = 24.dp
    val ICON_PADDING = 8.dp // Icon padding
    val SPACING = 8.dp
    val BUTTON_HEIGHT = 36.dp
    val MESSAGE_FONT_SIZE = 14.sp
    val ACTION_FONT_SIZE = 14.sp
    const val ALPHA_DURATION = 250
    const val SCALE_DURATION = 200
}

//@Composable
//fun GradientSnackBarDemo() {
//    var showSnackBar by remember { mutableStateOf(false) } // Visibility state
//    Box(
//        modifier = Modifier
//            .fillMaxSize() // Fill screen
//            .padding(16.dp) // Outer padding
//    ) {
//        Button(
//            onClick = { showSnackBar = true }, // Show SnackBar
//            modifier = Modifier
//                .align(Alignment.Center) // Center button
//                .wrapContentWidth() // Adjust width
//        ) {
//            Text("Show SnackBar") // Button text
//        }
//        if (showSnackBar) {
//            Box(
//                modifier = Modifier
//                    .align(Alignment.BottomCenter) // Position at bottom
//                    .padding(bottom = 16.dp) // Padding
//            ) {
//                GradientSnackBar(
//                    message = "Your settings have been saved successfully!", // Demo message
//                    actionLabel = "OK", // Action label
//                    onAction = { showSnackBar = false }, // Dismiss on action
////                    duration = 3000L // Duration
//                )
//                LaunchedEffect(Unit) {
//                    delay(3000L) // Wait 3 seconds
//                    showSnackBar = false // Auto-dismiss
//                }
//            }
//        }
//    }
//}