package com.soniq.player.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.soniq.player.R
import com.soniq.player.core.designsystem.theme.SoniqTheme

/**
 * Translucent frosted-glass card with vertical sheen and delicate specular border highlight.
 * Uses layered gradients instead of expensive per-card runtime blur for smooth 60/120fps scrolling.
 */
@Composable
fun SoniqGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = SoniqTheme.glassMetrics.cardRadius,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(SoniqTheme.tokens.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = SoniqTheme.glass
    val metrics = SoniqTheme.glassMetrics
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val fillBrush = remember(glass.surfaceElevated, glass.surfaceTop, glass.surfaceBottom, elevated) {
        Brush.verticalGradient(
            colors = listOf(
                if (elevated) glass.surfaceElevated else glass.surfaceTop,
                glass.surfaceBottom,
            ),
        )
    }
    val borderBrush = remember(glass.borderHighlight, glass.borderSubtle) {
        Brush.linearGradient(
            colors = listOf(
                glass.borderHighlight,
                glass.borderSubtle,
            ),
        )
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fillBrush)
            .border(width = metrics.borderWidth, brush = borderBrush, shape = shape)
            .then(clickableModifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(SoniqTheme.tokens.spacing.sm),
            content = content,
        )
    }
}

/** Circular frosted-glass icon button maintaining >= 48dp accessible touch target. */
@Composable
fun SoniqGlassIconButton(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = SoniqTheme.tokens.sizes.minTouchTarget,
    iconSize: Dp = SoniqTheme.tokens.sizes.icon,
    highlighted: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val glass = SoniqTheme.glass
    val metrics = SoniqTheme.glassMetrics
    val backgroundBrush = remember(
        highlighted,
        glass.activePillStart,
        glass.activePillEnd,
        glass.surfaceElevated,
        glass.surfaceBottom,
    ) {
        if (highlighted) {
            Brush.linearGradient(listOf(glass.activePillStart, glass.activePillEnd))
        } else {
            Brush.verticalGradient(listOf(glass.surfaceElevated, glass.surfaceBottom))
        }
    }
    val borderBrush = remember(glass.borderHighlight, glass.borderSubtle) {
        Brush.linearGradient(listOf(glass.borderHighlight, glass.borderSubtle))
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = metrics.borderWidth,
            brush = borderBrush,
        ),
        modifier = modifier
            .size(size)
            .semantics(mergeDescendants = true) {},
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush),
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = if (highlighted) Color.White else tint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/** Pill-shaped frosted-glass tab / filter chip with glowing active gradient. */
@Composable
fun SoniqGlassPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = SoniqTheme.glass
    val metrics = SoniqTheme.glassMetrics
    val spacing = SoniqTheme.tokens.spacing
    val shape = remember(metrics.pillRadius) { RoundedCornerShape(metrics.pillRadius) }
    val fillBrush = remember(
        selected,
        glass.activePillStart,
        glass.activePillEnd,
        glass.surfaceTop,
        glass.surfaceBottom,
    ) {
        if (selected) {
            Brush.horizontalGradient(listOf(glass.activePillStart, glass.activePillEnd))
        } else {
            Brush.verticalGradient(listOf(glass.surfaceTop, glass.surfaceBottom))
        }
    }
    val borderBrush = remember(glass.borderHighlight, glass.borderSubtle) {
        Brush.linearGradient(listOf(glass.borderHighlight, glass.borderSubtle))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(fillBrush)
            .border(
                width = metrics.borderWidth,
                brush = borderBrush,
                shape = shape,
            )
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = spacing.lg, vertical = spacing.sm + 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Translucent frosted-glass search field. */
@Composable
fun SoniqGlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val glass = SoniqTheme.glass
    val metrics = SoniqTheme.glassMetrics
    val spacing = SoniqTheme.tokens.spacing
    val shape = RoundedCornerShape(metrics.pillRadius)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(glass.surfaceTop, glass.surfaceBottom)))
            .border(
                width = metrics.borderWidth,
                brush = Brush.linearGradient(listOf(glass.borderHighlight, glass.borderSubtle)),
                shape = shape,
            )
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(SoniqTheme.tokens.sizes.iconSmall),
        )
        Spacer(modifier = Modifier.width(spacing.sm))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.cd_clear_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(SoniqTheme.tokens.sizes.iconSmall),
                )
            }
        }
    }
}

/** Reusable Liquid Glass media row with artwork thumbnail, metadata, Play button, and More menu. */
@Composable
fun SoniqGlassMediaRow(
    title: String,
    subtitle: String,
    artworkUri: String?,
    fallbackIcon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    val metrics = SoniqTheme.glassMetrics
    val glass = SoniqTheme.glass
    val thumbShape = RoundedCornerShape(metrics.thumbnailRadius)

    SoniqGlassCard(
        modifier = modifier,
        cornerRadius = metrics.cardRadius,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.sm + 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(sizes.trackArtwork)
                    .clip(thumbShape)
                    .background(Brush.linearGradient(listOf(glass.surfaceElevated, glass.surfaceBottom)))
                    .border(metrics.borderWidth, glass.borderSubtle, thumbShape),
            ) {
                Icon(
                    painter = fallbackIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(sizes.icon),
                )
                if (!artworkUri.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailing != null) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    trailing()
                }
            }
        }
    }
}

/** Ambient glowing progress track used on MiniPlayer and cards. */
@Composable
fun SoniqGlassLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
) {
    val glass = SoniqTheme.glass
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(glass.borderSubtle),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(height)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(glass.scrubberStart, glass.scrubberEnd))),
            )
        }
    }
}

/** Translucent Liquid Glass modal bottom sheet shared across Queue, Audio Effects, and Tracks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoniqGlassModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = SoniqTheme.glass
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = glass.ambientMid.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.56f),
        content = content,
    )
}

/** Translucent Liquid Glass dialog wrapper shared across playlists and theme dialogs. */
@Composable
fun SoniqGlassDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    val glass = SoniqTheme.glass
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        containerColor = glass.ambientMid.copy(alpha = 0.96f),
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
