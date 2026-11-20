package com.soniq.player.core.designsystem.component

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.BackgroundStyle

/**
 * Renders SONIQ's Liquid Glass atmospheric background (multi-stop lavender/periwinkle/violet
 * gradient with soft radial ambient lighting orbs, AMOLED-safe mode, or user-selected image
 * with configurable legibility scrim and API 31+ blur) behind [content].
 */
@Composable
fun SoniqBackground(
    modifier: Modifier = Modifier,
    appearance: AppearanceConfig = SoniqTheme.appearance,
    content: @Composable BoxScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val glass = SoniqTheme.glass
    val backgroundBrush = remember(glass.ambientTop, glass.ambientMid, glass.ambientBottom) {
        Brush.verticalGradient(
            colors = listOf(glass.ambientTop, glass.ambientMid, glass.ambientBottom),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .drawWithCache {
                val c1 = Offset(size.width * 0.15f, size.height * 0.18f)
                val r1 = size.width * 0.65f
                val orb1 = Brush.radialGradient(
                    colors = listOf(glass.glowPrimary, Color.Transparent),
                    center = c1,
                    radius = r1,
                )
                val c2 = Offset(size.width * 0.88f, size.height * 0.42f)
                val r2 = size.width * 0.6f
                val orb2 = Brush.radialGradient(
                    colors = listOf(glass.glowSecondary, Color.Transparent),
                    center = c2,
                    radius = r2,
                )
                val c3 = Offset(size.width * 0.45f, size.height * 0.86f)
                val r3 = size.width * 0.7f
                val orb3 = Brush.radialGradient(
                    colors = listOf(glass.glowTertiary, Color.Transparent),
                    center = c3,
                    radius = r3,
                )
                onDrawBehind {
                    drawCircle(brush = orb1, center = c1, radius = r1)
                    drawCircle(brush = orb2, center = c2, radius = r2)
                    drawCircle(brush = orb3, center = c3, radius = r3)
                }
            },
    ) {

        when (appearance.effectiveBackgroundStyle) {
            BackgroundStyle.SOLID -> Unit
            BackgroundStyle.GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    colorScheme.primary.copy(alpha = 0.18f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
            }
            BackgroundStyle.IMAGE -> {
                val uri = appearance.backgroundImageUri?.let(Uri::parse)
                if (uri != null) {
                    val blurModifier = if (
                        appearance.backgroundBlurDp > 0 &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ) {
                        Modifier.fillMaxSize().blur(appearance.backgroundBlurDp.dp)
                    } else {
                        Modifier.fillMaxSize()
                    }
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = blurModifier,
                    )
                    val scrimAlpha = appearance.backgroundScrimAlpha.coerceIn(
                        AppearanceConfig.MIN_BACKGROUND_SCRIM_ALPHA,
                        AppearanceConfig.MAX_BACKGROUND_SCRIM_ALPHA,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorScheme.background.copy(alpha = scrimAlpha)),
                    )
                }
            }
        }
        CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground) {
            content()
        }
    }
}
