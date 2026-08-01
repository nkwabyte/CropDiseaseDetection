package com.nkwabyte.cropdiseasedetection.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.common.data.DiseaseDatabase
import com.nkwabyte.cropdiseasedetection.common.data.DiseaseInfo
import com.nkwabyte.cropdiseasedetection.common.model.RecommendationLanguage
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.ui.screens.encyclopedia.getCropTheme
import com.nkwabyte.cropdiseasedetection.ui.screens.encyclopedia.getDiseaseDrawable
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    detectionViewModel: DetectionViewModel,
    appViewModel: AppViewModel,
    onBack: () -> Unit,
    onDrawerButtonClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val detectionState by detectionViewModel.detectionState.collectAsState()
    val appState by appViewModel.appState.collectAsState()
    val selectedLanguage = appState.recommendationLanguage

    val detectedDiseases: List<Pair<DiseaseInfo, Int>> = remember(detectionState.results) {
        detectionState.results
            .groupBy { it.className ?: it.classIndex.toString() }
            .map { (_, results) ->
                val sample = results.first()
                val disease = DiseaseDatabase.diseases.find { info ->
                    sample.className?.let { name -> info.name.contains(name, ignoreCase = true) } == true
                } ?: DiseaseDatabase.diseases.getOrNull(sample.classIndex)
                Pair(disease, results.size)
            }
            .filter { it.first != null }
            .map { Pair(it.first!!, it.second) }
            .sortedByDescending { it.second }
    }

    var selectedDisease by remember(detectedDiseases) {
        mutableStateOf(if (detectedDiseases.size == 1) detectedDiseases.first().first else null)
    }

    Scaffold(
        topBar = {
            AppBar(
                title = {
                    Text(
                        "Recommendations",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                onDrawerButtonClick = onDrawerButtonClick,
                isHomeScreen = false,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            // ── Sticky language selector ────────────────────────────────────────
            LanguageSelectorBar(
                selectedLanguage = selectedLanguage,
                onSelect = { appViewModel.setRecommendationLanguage(it) }
            )

            if (detectedDiseases.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("No detection data available.", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Run a detection first, then come back for recommendations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        OutlinedButton(onClick = onBack) { Text("Go Back") }
                    }
                }
            } else {
                AnimatedContent(
                    targetState = selectedDisease,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) { disease ->
                    if (disease == null) {
                        DiseasePicker(
                            detectedDiseases = detectedDiseases,
                            selectedLanguage = selectedLanguage,
                            onSelect = { selectedDisease = it }
                        )
                    } else {
                        DiseaseRecommendationDetail(
                            disease = disease,
                            selectedLanguage = selectedLanguage,
                            detectionCount = detectedDiseases.find { it.first.id == disease.id }?.second ?: 1,
                            totalDetections = detectionState.results.size,
                            showBackToList = detectedDiseases.size > 1,
                            onBackToList = { selectedDisease = null }
                        )
                    }
                }
            }
        }
    }
}

// ── Language selector ──────────────────────────────────────────────────────────

@Composable
private fun LanguageSelectorBar(
    selectedLanguage: RecommendationLanguage,
    onSelect: (RecommendationLanguage) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                RecommendationLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = selectedLanguage == language,
                        onClick = { onSelect(language) },
                        label = {
                            Text(
                                text = "${language.flag} ${language.displayName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}

// ── Translation pending banner ─────────────────────────────────────────────────

@Composable
private fun TranslationPendingBanner(language: RecommendationLanguage) {
    AnimatedVisibility(
        visible = language != RecommendationLanguage.ENGLISH,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "${language.flag} ${language.nativeName} — Translation coming soon",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBF360C)
                        )
                    )
                    Text(
                        text = "Content is currently displayed in English.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFE65100)
                        )
                    )
                }
            }
        }
    }
}

// ── Disease picker (multi-disease) ─────────────────────────────────────────────

@Composable
private fun DiseasePicker(
    detectedDiseases: List<Pair<DiseaseInfo, Int>>,
    selectedLanguage: RecommendationLanguage,
    onSelect: (DiseaseInfo) -> Unit
) {
    val total = detectedDiseases.sumOf { it.second }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TranslationPendingBanner(selectedLanguage)

        Text(
            text = "Multiple conditions detected",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Select a disease to view treatment recommendations:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        detectedDiseases.forEach { (disease, count) ->
            DiseasePickerCard(
                disease = disease,
                count = count,
                total = total,
                onClick = { onSelect(disease) }
            )
        }
    }
}

@Composable
private fun DiseasePickerCard(
    disease: DiseaseInfo,
    count: Int,
    total: Int,
    onClick: () -> Unit
) {
    val cropTheme = getCropTheme(disease.crop)
    val percentage = if (total > 0) (count.toFloat() / total * 100).toInt() else 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(getDiseaseDrawable(disease.id)),
                contentDescription = disease.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = disease.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = disease.localName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { count.toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                    color = cropTheme.accentColor,
                    trackColor = cropTheme.backgroundColor
                )
                Text(
                    text = "Detected $count time${if (count != 1) "s" else ""} ($percentage% of results)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ── Detail view ────────────────────────────────────────────────────────────────

@Composable
private fun DiseaseRecommendationDetail(
    disease: DiseaseInfo,
    selectedLanguage: RecommendationLanguage,
    detectionCount: Int,
    totalDetections: Int,
    showBackToList: Boolean,
    onBackToList: () -> Unit
) {
    val cropTheme = getCropTheme(disease.crop)
    val percentage = if (totalDetections > 0) (detectionCount.toFloat() / totalDetections * 100).toInt() else 100

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Translation banner (only when non-English is selected)
        item {
            TranslationPendingBanner(selectedLanguage)
        }

        // Back to list button
        if (showBackToList) {
            item {
                TextButton(
                    onClick = onBackToList,
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("All detected conditions")
                }
            }
        }

        // Disease header card with image
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cropTheme.backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cropTheme.emoji, fontSize = 28.sp)
                        Column {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(cropTheme.accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = disease.crop,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = cropTheme.textColor
                                    )
                                )
                            }
                            if (!disease.isHealthy) {
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFEBEE))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "⚠ Disease",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC62828)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = disease.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = cropTheme.textColor
                        )
                    )
                    Text(
                        text = disease.localName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = cropTheme.textColor.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { detectionCount.toFloat() / totalDetections.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = cropTheme.accentColor,
                        trackColor = cropTheme.accentColor.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "Detected $detectionCount time${if (detectionCount != 1) "s" else ""} on this leaf ($percentage% of all detections)",
                        style = MaterialTheme.typography.bodySmall.copy(color = cropTheme.textColor.copy(alpha = 0.7f)),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Image(
                        painter = painterResource(getDiseaseDrawable(disease.id)),
                        contentDescription = disease.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = disease.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = cropTheme.textColor.copy(alpha = 0.85f),
                            lineHeight = 22.sp
                        )
                    )
                }
            }
        }

        if (!disease.isHealthy) {
            item {
                RecommendationSection(
                    icon = Icons.Default.WarningAmber,
                    iconTint = Color(0xFFE65100),
                    title = "Cause",
                    content = disease.causes,
                    containerColor = Color(0xFFFFF3E0)
                )
            }
            item {
                RecommendationSection(
                    icon = Icons.Default.WarningAmber,
                    iconTint = Color(0xFFC62828),
                    title = "Effects on Crop & Yield",
                    content = disease.effects,
                    containerColor = Color(0xFFFFEBEE)
                )
            }
            item {
                RecommendationSection(
                    icon = Icons.Default.Eco,
                    iconTint = Color(0xFF2E7D32),
                    title = "Organic / Biological Control",
                    content = disease.organicMitigation,
                    containerColor = Color(0xFFE8F5E9)
                )
            }
            item {
                RecommendationSection(
                    icon = Icons.Default.Science,
                    iconTint = Color(0xFF1565C0),
                    title = "Chemical Control",
                    content = disease.chemicalMitigation,
                    containerColor = Color(0xFFE3F2FD)
                )
            }
            item {
                RecommendationSection(
                    icon = Icons.Default.CheckCircle,
                    iconTint = Color(0xFF6A1B9A),
                    title = "Prevention & Cultural Practices",
                    content = disease.prevention,
                    containerColor = Color(0xFFF3E5F5)
                )
            }
        } else {
            item {
                RecommendationSection(
                    icon = Icons.Default.CheckCircle,
                    iconTint = Color(0xFF2E7D32),
                    title = "Your plant looks healthy!",
                    content = disease.prevention,
                    containerColor = Color(0xFFE8F5E9)
                )
            }
            item {
                RecommendationSection(
                    icon = Icons.Default.Eco,
                    iconTint = Color(0xFF2E7D32),
                    title = "Best Organic Practices to Maintain Health",
                    content = disease.organicMitigation,
                    containerColor = Color(0xFFF1F8E9)
                )
            }
            item {
                RecommendationSection(
                    icon = Icons.Default.Science,
                    iconTint = Color(0xFF1565C0),
                    title = "Agrochemical Maintenance Tips",
                    content = disease.chemicalMitigation,
                    containerColor = Color(0xFFE3F2FD)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "These recommendations are for general guidance. Consult a local agronomist or extension officer from Ghana MoFA for location-specific advice and approved chemical products.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ── Shared section card ────────────────────────────────────────────────────────

@Composable
private fun RecommendationSection(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    content: String,
    containerColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = iconTint
                    )
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            )
        }
    }
}
