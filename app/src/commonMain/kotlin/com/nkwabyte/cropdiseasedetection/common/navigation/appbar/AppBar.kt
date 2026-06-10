package com.nkwabyte.cropdiseasedetection.common.navigation.appbar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.nkwabyte.cropdiseasedetection.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    onDrawerButtonClick: () -> Unit,
    isHomeScreen: Boolean = false,
    isCameraMode: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    colors: TopAppBarColors? = null,
    menuIconColor: Color? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
) {

    TopAppBar(
        modifier = modifier,
        title = title,
        navigationIcon = {
            if (navigationIcon != null) {
                navigationIcon()
            } else {
                IconButton(onClick = onDrawerButtonClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(Res.string.home_open_drawer_description),
                        tint = menuIconColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            if(isHomeScreen) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isCameraMode) Icons.Default.CameraAlt else Icons.Default.Photo,
                        contentDescription = if (isCameraMode)
                            stringResource(Res.string.home_camera_mode_description)
                        else stringResource(Res.string.home_gallery_mode_description),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = isCameraMode,
                        onCheckedChange = onCheckedChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                        )
                    )
                }
            }
        },
        colors = colors?: TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}