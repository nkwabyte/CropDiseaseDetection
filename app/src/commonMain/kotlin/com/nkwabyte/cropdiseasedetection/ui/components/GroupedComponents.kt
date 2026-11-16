package com.nkwabyte.cropdiseasedetection.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nkwabyte.cropdiseasedetection.generated.resources.Res
import com.nkwabyte.cropdiseasedetection.generated.resources.result_all_count
import com.nkwabyte.cropdiseasedetection.generated.resources.result_box_confidence
import org.jetbrains.compose.resources.stringResource

// Shared building blocks for the iOS-aligned layout. They are deliberately small: each
// one is a direct counterpart of a SwiftUI idiom the iOS screens repeat, so the two
// platforms are built from the same vocabulary rather than reproduced pixel by pixel.

/** Amber used for "attention" states — the iOS screens' `.orange`. */
val WarningColor = Color(0xFFF59E0B)

/** Blue used for secondary actions — the iOS screens' `.blue`. */
val InfoColor = Color(0xFF2563EB)

/**
 * A rounded, filled container — the counterpart of iOS's
 * `.background(Color(.secondarySystemBackground)).cornerRadius(16)`.
 */
@Composable
fun GroupedCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
    padding: Int = 16,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(color)
            .padding(padding.dp),
        content = content,
    )
}

/** An uppercase-free section caption above a [GroupedCard], like a Form section header. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

/** Small explanatory text below a section, like a Form section footer. */
@Composable
fun SectionFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 6.dp),
    )
}

/** A tinted callout with an icon and title — the iOS result/status banners. */
@Composable
fun StatusBanner(
    icon: ImageVector,
    tint: Color,
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A capsule toggle chip — the iOS filter chips. */
@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = content, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** The full-width filled action button used for each screen's primary action. */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.6f),
            disabledContentColor = contentColor,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp, horizontal = 16.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * A tappable row inside a [GroupedCard]: leading icon, title and subtitle, trailing chevron
 * — the iOS `NavigationLink` / list-button row.
 */
@Composable
fun GroupedRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) trailing()
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Divider between [GroupedRow]s, inset past the leading icon like an iOS list. */
@Composable
fun GroupedDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

/**
 * The disease filter chips and box-confidence slider shown above a scan's detections.
 * Both only change which boxes are DRAWN — never the detections or the diagnosis.
 *
 * @param names distinct disease names, in first-seen order.
 * @param countFor how many detections carry a given name.
 */
@Composable
fun BoxFilterControls(
    names: List<String>,
    totalCount: Int,
    visibleCount: Int,
    countFor: (String) -> Int,
    selectedName: String?,
    onSelectName: (String?) -> Unit,
    confidence: Float,
    onConfidenceChange: (Float) -> Unit,
    floor: Float,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (names.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillChip(
                    text = stringResource(Res.string.result_all_count, totalCount.toString()),
                    selected = selectedName == null,
                    onClick = { onSelectName(null) },
                    leadingIcon = Icons.Default.FilterList,
                )
                names.forEach { name ->
                    val isSelected = selectedName == name
                    PillChip(
                        text = "$name (${countFor(name)})",
                        selected = isSelected,
                        onClick = { onSelectName(if (isSelected) null else name) },
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.result_box_confidence),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "$visibleCount of $totalCount  ·  ${(confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // A degenerate range would crash the Slider, so it is widened if the floor is at 1.
        val start = floor.coerceIn(0f, 0.99f)
        Slider(
            value = confidence.coerceIn(start, 1f),
            onValueChange = onConfidenceChange,
            valueRange = start..1f,
        )
    }
}
