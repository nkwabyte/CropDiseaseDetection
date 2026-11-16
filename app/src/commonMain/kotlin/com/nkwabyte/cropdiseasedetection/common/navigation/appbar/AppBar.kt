package com.nkwabyte.cropdiseasedetection.common.navigation.appbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.nkwabyte.cropdiseasedetection.generated.resources.Res
import com.nkwabyte.cropdiseasedetection.generated.resources.nav_back
import org.jetbrains.compose.resources.stringResource

/**
 * The one top bar every screen uses, matching the iOS navigation structure:
 *
 * - A tab's root screen (no [onBack]) gets a large, left-aligned title that collapses on
 *   scroll — the Material equivalent of a SwiftUI large `navigationTitle`.
 * - A screen pushed inside a tab (with [onBack]) gets a compact centred title and a back
 *   arrow — the equivalent of `.navigationBarTitleDisplayMode(.inline)`.
 *
 * Navigation between sections lives in the bottom tab bar, so there is no drawer button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
    )

    if (onBack == null) {
        LargeTopAppBar(
            modifier = modifier,
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    } else {
        CenterAlignedTopAppBar(
            modifier = modifier,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.nav_back),
                    )
                }
            },
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    }
}
