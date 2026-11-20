package com.soniq.player.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.soniq.player.core.designsystem.theme.SoniqTheme

/**
 * Standard screen scaffold with transparent container color so [SoniqBackground] shows through.
 */
@Composable
fun SoniqScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = topBar,
        snackbarHost = snackbarHost,
        content = content,
    )
}

/** Top app bar styled with SONIQ tokens and transparent container for background continuity. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoniqTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(
                alpha = SoniqTheme.tokens.alpha.topBarScrolled,
            ),
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

/** Section label used in settings and library screens. */
@Composable
fun SoniqSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .semantics { heading() }
            .padding(
                top = spacing.lg,
                bottom = spacing.sm,
            ),
    )
}

/** Frosted-glass surface card that groups related controls or information using design tokens. */
@Composable
fun SoniqSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    val glass = SoniqTheme.glass
    val metrics = SoniqTheme.glassMetrics
    val shape = RoundedCornerShape(metrics.cardRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(glass.surfaceTop, glass.surfaceBottom)))
            .border(
                width = metrics.borderWidth,
                brush = Brush.linearGradient(listOf(glass.borderHighlight, glass.borderSubtle)),
                shape = shape,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            content = content,
        )
    }
}

/** Centered empty/permission state presentation with optional primary action. */
@Composable
fun SoniqEmptyState(
    icon: Painter,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(sizes.emptyStateIcon),
        )
        Spacer(modifier = Modifier.height(spacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = sizes.contentMaxWidth),
        )
        Spacer(modifier = Modifier.height(spacing.sm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = sizes.contentMaxWidth),
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(spacing.xl))
            Button(onClick = onActionClick) {
                Text(text = actionLabel)
            }
        }
    }
}

/** Reusable Material 3 single-choice segmented selector bound to enum or domain options. */
@Composable
fun <T> SoniqSegmentedChoice(
    options: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = labelFor(option),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}
