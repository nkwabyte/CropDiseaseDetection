package com.nkwabyte.cropdiseasedetection.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.generated.resources.*
import com.nkwabyte.cropdiseasedetection.ui.theme.CropDiseaseDetectionTheme

/**
 * Composable for a single crop selection tile with an image background and text overlay.
 * The text overlay features a dynamic gradient from transparent at the top to a darker shade at the bottom,
 * starting at approximately 25% of the card's height from the bottom, ensuring text readability.
 * A visual indicator is shown when the tile is selected.
 *
 * @param cropNameResId The string resource ID for the crop's name (e.g., Res.string.corn_crop_name).
 * @param cropImageResId The drawable resource ID for the crop's image (e.g., Res.drawable.corn_image).
 * @param cropImageContentDescResId The string resource ID for the image's content description.
 * @param isSelected Boolean indicating if this tile is currently selected.
 * @param onClick Lambda function to be invoked when the card is clicked.
 * @param modifier Modifier to be applied to the outer Box container.
 */
@Composable
fun CropTileButton(
    cropNameResId: StringResource,
    cropImageResId: DrawableResource,
    cropImageContentDescResId: StringResource,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var boxHeightPx by remember { mutableFloatStateOf(0f) }

    // Animations for selection state
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        animationSpec = tween(durationMillis = 150),
        label = "elevation"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "borderWidth"
    )

    Box(
        modifier = modifier
            .height(160.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .onSizeChanged { size ->
                boxHeightPx = size.height.toFloat()
            },
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = if (borderWidth > 0.dp) BorderStroke(borderWidth, MaterialTheme.colorScheme.primary) else null,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize() 
            ) {
                // Background Image
                Image(
                    painter = painterResource(cropImageResId),
                    contentDescription = stringResource(cropImageContentDescResId),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient overlay
                val gradientStartY = if (boxHeightPx > 0) boxHeightPx * 0.5f else 0f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.9f)
                                ),
                                startY = gradientStartY,
                                endY = boxHeightPx
                            )
                        ),
                ) {}

                // Text overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(
                        text = stringResource(cropNameResId),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Selection Indicator Overlay (Checkmark)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .background(Color.White, shape = RoundedCornerShape(50))
                        )
                    }
                }
            }
        }
    }
}