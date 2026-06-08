package com.nkwabyte.cropdiseasedetection.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import com.nkwabyte.cropdiseasedetection.generated.resources.*

/**
 * Composable for displaying a statistic card on the profile screen.
 * It dynamically adjusts its content based on whether a label is provided,
 * allowing it to display either a value/label pair or a centered image icon.
 *
 * @param label The label for the statistic (e.g., "SUCCESS"). If empty, an image icon is displayed.
 * @param value The value of the statistic (e.g., "84%"). Only displayed when label is not empty.
 * @param modifier Modifier to be applied to the card.
 */
@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()

    // Dynamically retrieve visual styles based on the statistic type
    val (emoji, accentColor, brush) = remember(label, isDark) {
        when (label.uppercase()) {
            "SUCCESS" -> {
                val accent = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                val startBg = if (isDark) Color(0xFF1B3822) else Color(0xFFF1F8E9)
                val endBg = if (isDark) Color(0xFF112517) else Color(0xFFDCEDC8)
                Triple("🏆", accent, Brush.verticalGradient(listOf(startBg, endBg)))
            }
            "CROPS" -> {
                val accent = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00695C)
                val startBg = if (isDark) Color(0xFF003830) else Color(0xFFE0F2F1)
                val endBg = if (isDark) Color(0xFF00221E) else Color(0xFFB2DFDB)
                Triple("🌱", accent, Brush.verticalGradient(listOf(startBg, endBg)))
            }
            "DETECTIONS", "TOTAL SCANS", "SCANS", "TOTAL" -> {
                val accent = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)
                val startBg = if (isDark) Color(0xFF102A45) else Color(0xFFE3F2FD)
                val endBg = if (isDark) Color(0xFF0A1C30) else Color(0xFFBBDEFB)
                Triple("🔍", accent, Brush.verticalGradient(listOf(startBg, endBg)))
            }
            "HEALTHY" -> {
                val accent = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                val startBg = if (isDark) Color(0xFF1B3822) else Color(0xFFE8F5E9)
                val endBg = if (isDark) Color(0xFF112517) else Color(0xFFC8E6C9)
                Triple("🌿", accent, Brush.verticalGradient(listOf(startBg, endBg)))
            }
            "DISEASED" -> {
                val accent = if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
                val startBg = if (isDark) Color(0xFF3E1D22) else Color(0xFFFFEBEE)
                val endBg = if (isDark) Color(0xFF2A1216) else Color(0xFFFFCDD2)
                Triple("⚠️", accent, Brush.verticalGradient(listOf(startBg, endBg)))
            }
            else -> {
                val accent = if (isDark) Color(0xFFB0BEC5) else Color(0xFF455A64)
                val startBg = if (isDark) Color(0xFF263238) else Color(0xFFECEFF1)
                val endBg = if (isDark) Color(0xFF1B2428) else Color(0xFFCFD8DC)
                Triple("📊", accent, Brush.verticalGradient(listOf(startBg, endBg)))
            }
        }
    }

    Card(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (label.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.pulse),
                        contentDescription = "Pulse Icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 12.sp)
                    }
                }
                
                Column {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF1E293B),
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = 0.5.sp,
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }
    }
}



