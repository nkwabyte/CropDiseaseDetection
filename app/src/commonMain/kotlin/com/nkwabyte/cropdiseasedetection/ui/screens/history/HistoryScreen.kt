package com.nkwabyte.cropdiseasedetection.ui.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.rememberAsyncImagePainter
import com.nkwabyte.cropdiseasedetection.common.data.DiseaseDatabase
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.data.repository.DetectionRecord
import com.nkwabyte.cropdiseasedetection.data.repository.SyncRepository
import com.nkwabyte.cropdiseasedetection.ui.components.StatCard
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onDrawerButtonClick: () -> Unit,
    onSignInClick: () -> Unit = {},
    onStartScanClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val firebaseUser by Firebase.auth.authStateChanged.collectAsState(initial = Firebase.auth.currentUser)
    val isGuest = firebaseUser == null

    var isLoading by remember { mutableStateOf(false) }
    var historyRecords by remember { mutableStateOf<List<DetectionRecord>>(emptyList()) }
    var selectedRecord by remember { mutableStateOf<DetectionRecord?>(null) }

    val syncRepository = remember { SyncRepository() }
    val scope = rememberCoroutineScope()

    // Reload history records whenever auth state changes (sign-in status updates)
    LaunchedEffect(firebaseUser) {
        if (!isGuest) {
            isLoading = true
            historyRecords = syncRepository.getDetectionRecords()
            isLoading = false
        } else {
            historyRecords = emptyList()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppBar(
                title = { 
                    Text(
                        "Scan History", 
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    ) 
                },
                onDrawerButtonClick = onDrawerButtonClick,
                isHomeScreen = false
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isGuest) {
                // Guest Lock State UI
                GuestLockStateView(
                    onSignInClick = onSignInClick
                )
            } else if (isLoading) {
                // Loading State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (historyRecords.isEmpty()) {
                // Enhanced Empty State View
                HistoryEmptyStateView(
                    onStartScanClick = onStartScanClick
                )
            } else {
                // Compute summary stats for the user
                val totalScans = historyRecords.size
                val healthyCount = remember(historyRecords) {
                    historyRecords.count { record ->
                        val primaryResult = record.matchingResults.maxByOrNull { it.score }
                        primaryResult?.className?.lowercase()?.contains("healthy") == true
                    }
                }
                val diseasedCount = totalScans - healthyCount

                // History Records Scrollable List
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Stats Summary Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatCard(
                                label = "TOTAL",
                                value = totalScans.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = "HEALTHY",
                                value = healthyCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = "DISEASED",
                                value = diseasedCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    items(historyRecords, key = { it.timestamp }) { record ->
                        HistoryRecordCard(
                            record = record,
                            onClick = { selectedRecord = record }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog Overlay
    selectedRecord?.let { record ->
        HistoryDetailDialog(
            record = record,
            onDismiss = { selectedRecord = null }
        )
    }
}

@Composable
fun GuestLockStateView(
    onSignInClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "History Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "History is Locked",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Scans made as a guest are not synced to the cloud. Sign in or register to keep an archive of your diagnostics, sync data, and manage your crops.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Button(
                        onClick = onSignInClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text("Sign In to Unlock History", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryEmptyStateView(
    onStartScanClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Sync",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Your scan shelf is empty.",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "You haven't scanned any crops for diseases yet. Use the disease detector on a leaf to add diagnosed records to your cloud history archive.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Button(
                        onClick = onStartScanClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text("Start Detection Scan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRecordCard(
    record: DetectionRecord,
    onClick: () -> Unit
) {
    val dateString = formatTimestamp(record.timestamp)
    val primaryResult = record.matchingResults.maxByOrNull { it.score }
    val diseaseLabel = primaryResult?.className ?: "No Detection"
    val isHealthy = diseaseLabel.lowercase().contains("healthy")
    val confidence = primaryResult?.let { "${(it.score * 100).toInt()}%" } ?: "0%"

    val cropEmoji = when (record.cropName.lowercase()) {
        "corn" -> "🌽"
        "tomato" -> "🍅"
        "pepper" -> "🫑"
        else -> "🌱"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leaf scan thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                if (record.imageUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(record.imageUrl),
                        contentDescription = "Leaf scan",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$cropEmoji ${record.cropName.uppercase()}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isHealthy) "Healthy" else "Disease",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isHealthy) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Text(
                    text = diseaseLabel,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = "Confidence: $confidence",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryDetailDialog(
    record: DetectionRecord,
    onDismiss: () -> Unit
) {
    val dateString = formatTimestamp(record.timestamp)
    val primaryResult = record.matchingResults.maxByOrNull { it.score }
    val diseaseLabel = primaryResult?.className ?: "No Detection"
    val isHealthy = diseaseLabel.lowercase().contains("healthy")
    val confidence = primaryResult?.let { "${(it.score * 100).toInt()}%" } ?: "0%"

    // Look up contextual remedies from the local DiseaseDatabase
    val diseaseInfo = remember(diseaseLabel) {
        DiseaseDatabase.diseases.find { it.name.equals(diseaseLabel, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = record.cropName.uppercase(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = diseaseLabel,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dialog",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Full Leaf Scan Image
                    if (record.imageUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(record.imageUrl),
                            contentDescription = "Full Scan Leaf",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }

                    // Metadata Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Scan Date", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)))
                                Text(dateString, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Result Type", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)))
                                Text(
                                    text = if (isHealthy) "✓ Healthy" else "⚠️ Disease",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHealthy) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Confidence", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)))
                                Text(confidence, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    // Contextual Remedies details
                    if (diseaseInfo != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Actionable Remedies & Info",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )

                            DetailCardSection(title = "Description", content = diseaseInfo.description)
                            DetailCardSection(title = "Symptoms", content = diseaseInfo.symptoms)
                            DetailCardSection(title = "Prevention & Remedies", content = diseaseInfo.prevention)
                        }
                    } else {
                        // Fallback message if no matching data is found
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "No matching disease info was found in the database. Please check the encyclopedia page for other details.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Close Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Details", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailCardSection(title: String, content: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            )
        }
    }
}

// Pure Kotlin Multiplatform date formatter helper based on Ktor GMTDate
fun formatTimestamp(timestamp: Long): String {
    val date = io.ktor.util.date.GMTDate(timestamp)
    val monthStr = when (date.month) {
        io.ktor.util.date.Month.JANUARY -> "Jan"
        io.ktor.util.date.Month.FEBRUARY -> "Feb"
        io.ktor.util.date.Month.MARCH -> "Mar"
        io.ktor.util.date.Month.APRIL -> "Apr"
        io.ktor.util.date.Month.MAY -> "May"
        io.ktor.util.date.Month.JUNE -> "Jun"
        io.ktor.util.date.Month.JULY -> "Jul"
        io.ktor.util.date.Month.AUGUST -> "Aug"
        io.ktor.util.date.Month.SEPTEMBER -> "Sep"
        io.ktor.util.date.Month.OCTOBER -> "Oct"
        io.ktor.util.date.Month.NOVEMBER -> "Nov"
        io.ktor.util.date.Month.DECEMBER -> "Dec"
    }
    val minutesStr = date.minutes.toString().padStart(2, '0')
    val hoursStr = date.hours.toString().padStart(2, '0')
    return "$monthStr ${date.dayOfMonth}, ${date.year} ${hoursStr}:${minutesStr} GMT"
}
