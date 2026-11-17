package com.nkwabyte.cropdiseasedetection.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelCatalog
import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelSpec
import com.nkwabyte.cropdiseasedetection.common.models.ModelDownloadState
import com.nkwabyte.cropdiseasedetection.common.models.ModelDownloads
import com.nkwabyte.cropdiseasedetection.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private fun megabytes(bytes: Long): String {
    val tenths = (bytes * 10) / (1024 * 1024)
    return "${tenths / 10}.${tenths % 10} MB"
}

/** Lists every detector with whether it is built in, downloaded, or downloadable. */
@Composable
internal fun ModelPickerDialog(
    selectedId: String,
    downloads: ModelDownloads,
    onSelect: (DetectionModelSpec) -> Unit,
    onDelete: (DetectionModelSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(Res.string.settings_detection_model),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column {
                DetectionModelCatalog.all.forEach { spec ->
                    val state = if (spec.requiresDownload) downloads.stateOf(spec.id) else ModelDownloadState.Downloaded
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(spec) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = spec.id == selectedId, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(spec.displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = when {
                                    !spec.requiresDownload -> stringResource(Res.string.model_builtin)
                                    state is ModelDownloadState.Downloaded -> stringResource(Res.string.model_downloaded)
                                    state is ModelDownloadState.Downloading -> stringResource(Res.string.model_downloading_short)
                                    else -> stringResource(Res.string.model_tap_to_download)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        when {
                            !spec.requiresDownload -> Unit
                            state is ModelDownloadState.Downloaded -> IconButton(onClick = { onDelete(spec) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(Res.string.model_delete_download),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            state is ModelDownloadState.Downloading -> CircularProgressIndicator(
                                progress = { state.fraction ?: 0f },
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                            )
                            else -> Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = stringResource(Res.string.model_download),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel_text)) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
    )
}

/** Asked when the user picks a model whose weights are not on the device yet. */
@Composable
internal fun ModelDownloadPrompt(
    spec: DetectionModelSpec,
    onDownload: () -> Unit,
    onUseDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(Res.string.model_download_title, spec.displayName)) },
        text = { Text(stringResource(Res.string.model_download_message, spec.displayName)) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onDownload() }) {
                Text(stringResource(Res.string.model_download), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(); onUseDefault() }) {
                Text(stringResource(Res.string.model_use_default, DetectionModelCatalog.default.displayName))
            }
        },
    )
}

/** Progress or failure of an in-flight download, shown under the model row. */
@Composable
internal fun ModelDownloadStatus(
    spec: DetectionModelSpec,
    state: ModelDownloadState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
) {
    when (state) {
        is ModelDownloadState.Downloading -> Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.model_downloading, spec.displayName),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        state.totalBytes?.let { "${megabytes(state.downloadedBytes)} / ${megabytes(it)}" }
                            ?: megabytes(state.downloadedBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.model_cancel_download))
                }
            }
            val fraction = state.fraction
            if (fraction != null) {
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        is ModelDownloadState.Failed -> Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(Res.string.model_download_failed, spec.displayName, state.message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.model_retry))
            }
            IconButton(onClick = onDismissError) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        }

        ModelDownloadState.Downloaded, ModelDownloadState.NotDownloaded -> Unit
    }
}

/** Shown instead of downloading while [com.nkwabyte.cropdiseasedetection.common.model.ModelHosting.DOWNLOADS_ENABLED] is off. */
@Composable
internal fun ModelDownloadsUnavailableDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(Res.string.model_unavailable_title)) },
        text = { Text(stringResource(Res.string.model_unavailable_message)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ok_text)) } },
    )
}
