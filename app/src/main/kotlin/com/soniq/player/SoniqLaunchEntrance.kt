package com.soniq.player

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.soniq.player.core.designsystem.token.SoniqGlassPalettes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen branded entrance displayed once after the system splash screen on cold start.
 *
 * Uses SONIQ's dark atmospheric palette and the unmodified symbol and wordmark PNG assets.
 */
@Composable
fun SoniqLaunchEntrance(
    isReady: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val darkGlass = SoniqGlassPalettes.dark(
        accent = MaterialTheme.colorScheme.primary,
        amoled = true,
    )
    val backgroundBrush = remember(darkGlass.ambientTop, darkGlass.ambientMid, darkGlass.ambientBottom) {
        Brush.verticalGradient(
            colors = listOf(darkGlass.ambientTop, darkGlass.ambientMid, darkGlass.ambientBottom),
        )
    }

    val symbolAlpha = remember { Animatable(0f) }
    val symbolRotation = remember { Animatable(0f) }
    val symbolScale = remember { Animatable(0.86f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkOffsetDp = remember { Animatable(16f) }
    val overlayAlpha = remember { Animatable(1f) }

    LaunchedEffect(isReady) {
        if (!isReady) return@LaunchedEffect

        val animatorScale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)

        if (animatorScale == 0f) {
            onFinished()
            return@LaunchedEffect
        }

        val settleEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

        val symbolFadeJob = launch {
            symbolAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            )
        }
        val symbolRotateJob = launch {
            symbolRotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 950, easing = settleEasing),
            )
        }
        val symbolScaleJob = launch {
            symbolScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 950, easing = settleEasing),
            )
        }

        // Enter the wordmark smoothly as the symbol nears completion of its 360° rotation.
        delay(700L)
        val wordmarkFadeJob = launch {
            wordmarkAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = settleEasing),
            )
        }
        val wordmarkSlideJob = launch {
            wordmarkOffsetDp.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500, easing = settleEasing),
            )
        }

        symbolFadeJob.join()
        symbolRotateJob.join()
        symbolScaleJob.join()
        wordmarkFadeJob.join()
        wordmarkSlideJob.join()

        // Brief settled hold before revealing Home.
        delay(240L)

        overlayAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing),
        )
        onFinished()
    }

    val appName = stringResource(R.string.app_name)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag(LAUNCH_ENTRANCE_TEST_TAG)
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(backgroundBrush)
            .drawWithCache {
                val c1 = Offset(size.width * 0.22f, size.height * 0.28f)
                val r1 = size.width * 0.62f
                val orb1 = Brush.radialGradient(
                    colors = listOf(darkGlass.glowTertiary, Color.Transparent),
                    center = c1,
                    radius = r1,
                )
                val c2 = Offset(size.width * 0.78f, size.height * 0.68f)
                val r2 = size.width * 0.58f
                val orb2 = Brush.radialGradient(
                    colors = listOf(darkGlass.glowSecondary, Color.Transparent),
                    center = c2,
                    radius = r2,
                )
                onDrawBehind {
                    drawCircle(brush = orb1, center = c1, radius = r1)
                    drawCircle(brush = orb2, center = c2, radius = r2)
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .systemBarsPadding()
            .padding(horizontal = 32.dp)
            .semantics { contentDescription = appName },
        contentAlignment = Alignment.Center,
    ) {
        val shortestSide = minOf(maxWidth, maxHeight)
        val symbolSize = (shortestSide * 0.30f).coerceIn(96.dp, 132.dp)
        val wordmarkWidth = (symbolSize * 1.24f).coerceIn(120.dp, 168.dp)
        val wordmarkTranslationPx = with(density) { wordmarkOffsetDp.value.dp.toPx() }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.soniq_logo_symbol),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(symbolSize)
                    .testTag(LAUNCH_SYMBOL_TEST_TAG)
                    .graphicsLayer {
                        alpha = symbolAlpha.value
                        rotationZ = symbolRotation.value
                        scaleX = symbolScale.value
                        scaleY = symbolScale.value
                    },
            )
            Spacer(modifier = Modifier.height(22.dp))
            Image(
                painter = painterResource(id = R.drawable.soniq_logo_wordmark),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(wordmarkWidth)
                    .testTag(LAUNCH_WORDMARK_TEST_TAG)
                    .graphicsLayer {
                        alpha = wordmarkAlpha.value
                        translationY = wordmarkTranslationPx
                    },
            )
        }
    }
}

internal const val LAUNCH_ENTRANCE_TEST_TAG = "soniq_launch_entrance"
internal const val LAUNCH_SYMBOL_TEST_TAG = "soniq_launch_symbol"
internal const val LAUNCH_WORDMARK_TEST_TAG = "soniq_launch_wordmark"
