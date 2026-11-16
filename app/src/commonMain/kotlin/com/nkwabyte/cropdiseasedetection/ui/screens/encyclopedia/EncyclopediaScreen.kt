package com.nkwabyte.cropdiseasedetection.ui.screens.encyclopedia

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nkwabyte.cropdiseasedetection.common.data.DiseaseDatabase
import com.nkwabyte.cropdiseasedetection.common.data.DiseaseInfo
import com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.generated.resources.*
import com.nkwabyte.cropdiseasedetection.ui.components.PillChip
import com.nkwabyte.cropdiseasedetection.ui.screens.home.FormattedBulletList
import com.nkwabyte.cropdiseasedetection.ui.screens.home.displayNameRes
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Crop filters; `null` is "All". Filtering is on the canonical crop id, never on text. */
private val cropFilters: List<SupportedCrop?> = listOf(null, SupportedCrop.CORN, SupportedCrop.PEPPER, SupportedCrop.TOMATO)

/**
 * The Encyclopedia tab — the counterpart of iOS `EncyclopediaView`: a search field, crop
 * filter chips and a list of every disease. A row opens [DiseaseDetailScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncyclopediaScreen(
    onOpenDisease: (diseaseId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCropName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCrop = selectedCropName?.let { SupportedCrop.valueOf(it) }

    val filtered = remember(searchQuery, selectedCrop) {
        DiseaseDatabase.diseases.filter { disease ->
            val matchesCrop = selectedCrop == null || disease.crop.equals(selectedCrop.canonicalLabel, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                disease.name.contains(searchQuery, ignoreCase = true) ||
                disease.localName.contains(searchQuery, ignoreCase = true)
            matchesCrop && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AppBar(title = stringResource(Res.string.tab_encyclopedia), scrollBehavior = scrollBehavior) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(Res.string.enc_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                cropFilters.forEach { crop ->
                    PillChip(
                        text = stringResource(crop?.displayNameRes() ?: Res.string.enc_filter_all),
                        selected = crop == selectedCrop,
                        onClick = { selectedCropName = crop?.name },
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(Res.string.enc_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(filtered, key = { it.id }) { disease ->
                        DiseaseRow(
                            disease = disease,
                            onClick = {
                                focusManager.clearFocus()
                                onOpenDisease(disease.id)
                            },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 94.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiseaseRow(disease: DiseaseInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(getDiseaseDrawable(disease.id)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                disease.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CropCapsule(disease.crop)
                HealthLabel(disease.isHealthy)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CropCapsule(crop: String) {
    Text(
        text = stringResource(cropNameRes(crop)),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun HealthLabel(isHealthy: Boolean) {
    Text(
        text = stringResource(if (isHealthy) Res.string.enc_healthy else Res.string.enc_disease),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
}

/** The translated name for a database crop string ("Corn", "Tomato", ...). */
private fun cropNameRes(crop: String): StringResource =
    SupportedCrop.fromCanonicalId(crop).displayNameRes()

/**
 * One disease — the counterpart of iOS `DiseaseDetailView`. Pushed from the encyclopedia,
 * a result summary row, or a saved scan's conditions list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseDetailScreen(
    diseaseId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val disease = remember(diseaseId) {
        DiseaseDatabase.diseases.firstOrNull { it.id == diseaseId } ?: DiseaseDatabase.diseases.first()
    }

    Scaffold(
        modifier = modifier,
        topBar = { AppBar(title = disease.name, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Image(
                painter = painterResource(getDiseaseDrawable(disease.id)),
                contentDescription = disease.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(disease.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (disease.localName.isNotBlank()) {
                    Text(
                        disease.localName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.enc_target_crop, stringResource(cropNameRes(disease.crop))),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    val tint = if (disease.isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    Text(
                        (if (disease.isHealthy) "✓ " else "⚠️ ") +
                            stringResource(if (disease.isHealthy) Res.string.enc_healthy else Res.string.enc_disease),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tint,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(tint.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DetailSection(Res.string.enc_overview, Icons.Default.Info, disease.description)
                DetailSection(Res.string.enc_symptoms, Icons.Default.LocalHospital, disease.symptoms)
                DetailSection(Res.string.enc_organic, Icons.Default.Eco, disease.organicMitigation)
                DetailSection(Res.string.enc_chemical, Icons.Default.Science, disease.chemicalMitigation)
                DetailSection(Res.string.enc_prevention, Icons.Default.Shield, disease.prevention)
            }
        }
    }
}

@Composable
private fun DetailSection(title: StringResource, icon: ImageVector, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        FormattedBulletList(content = content, bulletColor = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun getDiseaseDrawable(id: Int): DrawableResource {
    return when (id) {
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
