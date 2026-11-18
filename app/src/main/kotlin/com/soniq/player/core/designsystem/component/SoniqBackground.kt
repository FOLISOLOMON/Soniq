package com.soniq.player.core.designsystem.component

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.BackgroundStyle

/**
 * Renders the user-configured app background (solid surface, subtle accent gradient, or
 * user-selected local image with a legibility scrim) behind [content].
 */
@Composable
fun SoniqBackground(
    modifier: Modifier = Modifier,
    appearance: AppearanceConfig = SoniqTheme.appearance,
    content: @Composable BoxScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val alphaTokens = SoniqTheme.tokens.alpha

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background),
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
                                    colorScheme.primaryContainer.copy(alpha = alphaTokens.gradientAccent),
                                    colorScheme.background,
                                ),
                            ),
                        ),
                )
            }
            BackgroundStyle.IMAGE -> {
                val uri = appearance.backgroundImageUri?.let(Uri::parse)
                if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Scrim guarantees WCAG legibility regardless of image brightness.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorScheme.background.copy(alpha = alphaTokens.backgroundImageScrim)),
                    )
                }
            }
        }
        content()
    }
}
