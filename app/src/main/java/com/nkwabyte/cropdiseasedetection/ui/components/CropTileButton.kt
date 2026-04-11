package com.nkwabyte.cropdiseasedetection.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.R
import com.nkwabyte.cropdiseasedetection.ui.theme.CropDiseaseDetectionTheme

/**
 * Composable for a single crop selection tile with an image background and text overlay.
 * The text overlay features a dynamic gradient from transparent at the top to a darker shade at the bottom,
 * starting at approximately 25% of the card's height from the bottom, ensuring text readability.
 * A visual indicator is shown when the tile is selected.
 *
 * @param cropNameResId The string resource ID for the crop's name (e.g., R.string.corn_crop_name).
 * @param cropImageResId The drawable resource ID for the crop's image (e.g., R.drawable.corn_image).
 * @param cropImageContentDescResId The string resource ID for the image's content description.
 * @param isSelected Boolean indicating if this tile is currently selected.
 * @param onClick Lambda function to be invoked when the card is clicked.
 * @param modifier Modifier to be applied to the outer Box container.
 */
@Composable
fun CropTileButton(
    cropNameResId: Int,
    cropImageResId: Int,
    cropImageContentDescResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var boxHeightPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .height(150.dp)
            .clickable { onClick() }
            .onSizeChanged { size ->
                boxHeightPx = size.height.toFloat()
            },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize() // This Box contains the image and overlays, fills the Card
            ) {
                // Background Image
                Image(
                    painter = painterResource(id = cropImageResId),
                    contentDescription = stringResource(cropImageContentDescResId),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient overlay
                // The startY for the gradient will be calculated dynamically
                val gradientStartY = if (boxHeightPx > 0) {
                    // Start the gradient at 75% of the height from the top (which is 25% from the bottom)
                    boxHeightPx * 0.75f
                } else {
                    // Fallback or initial value if height is not yet measured
                    0f
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
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
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(
                        text = stringResource(cropNameResId),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Selection Indicator Overlay (on top of everything)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 150) // Specify dimensions for preview
@Composable
private fun PreviewCropTileButtonSelected() {
    CropDiseaseDetectionTheme {
        // Using sample resources for a more realistic preview
        CropTileButton(
            cropNameResId = R.string.crop_corn, // Assuming you have this string in your resources
            cropImageResId = R.drawable.corn_on_the_cob, // Assuming you have this image in your drawables
            cropImageContentDescResId = R.string.corn_corb_image_description,
            isSelected = true,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 150)
@Composable
private fun PreviewCropTileButtonUnselected() {
    CropDiseaseDetectionTheme {
        CropTileButton(
            cropNameResId = R.string.crop_corn,
            cropImageResId = R.drawable.corn_on_the_cob,
            cropImageContentDescResId = R.string.corn_corb_image_description,
            isSelected = false,
            onClick = {}
        )
    }
}