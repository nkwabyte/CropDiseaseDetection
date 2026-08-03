package com.nkwabyte.cropdiseasedetection.ui.screens.encyclopedia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.resources.stringResource
import com.nkwabyte.cropdiseasedetection.generated.resources.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nkwabyte.cropdiseasedetection.common.data.DiseaseDatabase
import com.nkwabyte.cropdiseasedetection.common.data.DiseaseInfo
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.nkwabyte.cropdiseasedetection.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncyclopediaScreen(
    onDrawerButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCropFilter by remember { mutableStateOf("All") }
    var selectedDisease by remember { mutableStateOf<DiseaseInfo?>(null) }

    val crops = listOf("All", "Corn", "Pepper", "Tomato")

    // Filter database entries based on search query and selected crop
    val filteredDiseases = remember(searchQuery, selectedCropFilter) {
        DiseaseDatabase.diseases.filter { disease ->
            val matchesCrop = selectedCropFilter == "All" || disease.crop.equals(selectedCropFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    disease.name.contains(searchQuery, ignoreCase = true) ||
                    disease.localName.contains(searchQuery, ignoreCase = true) ||
                    disease.description.contains(searchQuery, ignoreCase = true) ||
                    disease.symptoms.contains(searchQuery, ignoreCase = true)
            matchesCrop && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppBar(
                title = { 
                    Text(
                        "Disease Encyclopedia", 
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { 
                        Text(
                            "Search symptoms, diseases...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyLarge
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Search",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Filter Chips Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                
                crops.forEach { crop ->
                    val isSelected = selectedCropFilter == crop
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent 
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCropFilter = crop }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (crop) {
                                "Corn" -> "🌽 Corn"
                                "Tomato" -> "🍅 Tomato"
                                "Pepper" -> "🫑 Pepper"
                                else -> crop
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main List/Grid Section
            if (filteredDiseases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔍",
                            fontSize = 40.sp
                        )
                        Text(
                            text = "No conditions found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Try refining your search query or choosing another crop filter.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(filteredDiseases, key = { it.id }) { disease ->
                        DiseaseCard(
                            disease = disease,
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                selectedDisease = disease
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog Overlay
    selectedDisease?.let { disease ->
        DiseaseDetailDialog(
            disease = disease,
            onDismiss = { selectedDisease = null }
        )
    }
}

@Composable
fun getDiseaseDrawable(id: Int): DrawableResource {
    return when(id) {
        0 -> Res.drawable.disease_0
        1 -> Res.drawable.disease_1
        2 -> Res.drawable.disease_2
        3 -> Res.drawable.disease_3
        4 -> Res.drawable.disease_4
        5 -> Res.drawable.disease_5
        6 -> Res.drawable.disease_6
        7 -> Res.drawable.disease_7
        8 -> Res.drawable.disease_8
        9 -> Res.drawable.disease_9
        10 -> Res.drawable.disease_10
        11 -> Res.drawable.disease_11
        12 -> Res.drawable.disease_12
        13 -> Res.drawable.disease_13
        14 -> Res.drawable.disease_14
        15 -> Res.drawable.disease_15
        16 -> Res.drawable.disease_16
        17 -> Res.drawable.disease_17
        18 -> Res.drawable.disease_18
        19 -> Res.drawable.disease_19
        20 -> Res.drawable.disease_20
        21 -> Res.drawable.disease_21
        22 -> Res.drawable.disease_22
        else -> Res.drawable.pulse
    }
}

@Composable
fun DiseaseCard(
    disease: DiseaseInfo,
    onClick: () -> Unit
) {
    // Dynamic styling based on crop type
    val cropTheme = getCropTheme(disease.crop)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, cropTheme.accentColor.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Crop Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(cropTheme.backgroundColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${cropTheme.emoji} ${disease.crop}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = cropTheme.textColor
                        )
                    )
                }

                // Healthy / Diseased indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (disease.isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (disease.isHealthy) "✓ Healthy" else "⚠️ Disease",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (disease.isHealthy) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Disease Name
            Text(
                text = disease.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Short Description
            Text(
                text = disease.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DiseaseDetailDialog(
    disease: DiseaseInfo,
    onDismiss: () -> Unit
) {
    val cropTheme = getCropTheme(disease.crop)
    val uriHandler = LocalUriHandler.current

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
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cropTheme.backgroundColor)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${cropTheme.emoji} ${disease.crop}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = cropTheme.textColor
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (disease.isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (disease.isHealthy) "✓ Healthy State" else "⚠️ Disease",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (disease.isHealthy) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = disease.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (disease.localName.isNotBlank()) {
                            Text(
                                text = disease.localName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
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
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Local disease reference image
                    Image(
                        painter = painterResource(getDiseaseDrawable(disease.id)),
                        contentDescription = disease.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    DetailSection(title = "Description", content = disease.description)
                    DetailSection(title = "Symptoms & Identifiers", content = disease.symptoms)
                    DetailSection(title = "Pathogen & Cause", content = disease.causes)
                    if (!disease.isHealthy) {
                        DetailSection(title = "Effects on Crop & Yield", content = disease.effects)
                    }
                    DetailSection(title = "Prevention & Cultural Control", content = disease.prevention)
                    if (!disease.isHealthy) {
                        DetailSection(title = "Organic / Biological Control", content = disease.organicMitigation)
                        DetailSection(title = "Chemical Control", content = disease.chemicalMitigation)
                    } else {
                        DetailSection(title = "Best Practices", content = disease.organicMitigation)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { uriHandler.openUri(disease.imageUrl) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cropTheme.accentColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "View Reference Image",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(stringResource(Res.string.close_button_text))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        com.nkwabyte.cropdiseasedetection.ui.screens.home.FormattedBulletList(
            content = content,
            bulletColor = MaterialTheme.colorScheme.primary
        )
    }
}

// Helper models for crop visual branding
data class CropTheme(
    val emoji: String,
    val backgroundColor: Color,
    val textColor: Color,
    val accentColor: Color
)

fun getCropTheme(crop: String): CropTheme {
    return when (crop.lowercase()) {
        "corn" -> CropTheme(
            emoji = "🌽",
            backgroundColor = Color(0xFFFFF8E1),
            textColor = Color(0xFFFF8F00),
            accentColor = Color(0xFFFFB300)
        )
        "tomato" -> CropTheme(
            emoji = "🍅",
            backgroundColor = Color(0xFFFFEBEE),
            textColor = Color(0xFFC62828),
            accentColor = Color(0xFFE53935)
        )
        "pepper" -> CropTheme(
            emoji = "🫑",
            backgroundColor = Color(0xFFE8F5E9),
            textColor = Color(0xFF2E7D32),
            accentColor = Color(0xFF43A047)
        )
        else -> CropTheme(
            emoji = "🌱",
            backgroundColor = Color(0xFFF5F5F5),
            textColor = Color(0xFF616161),
            accentColor = Color(0xFF9E9E9E)
        )
    }
}
