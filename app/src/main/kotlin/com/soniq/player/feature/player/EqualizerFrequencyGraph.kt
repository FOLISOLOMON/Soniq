package com.soniq.player.feature.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soniq.player.R
import com.soniq.player.core.designsystem.component.SoniqGlassCard
import com.soniq.player.core.designsystem.theme.SoniqTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/** Pure math helpers for converting between Android Equalizer millibel values and graph coordinates. */
object EqualizerGraphMath {
    fun gainMbToFraction(levelMb: Int, minMb: Int, maxMb: Int): Float {
        val span = (maxMb - minMb).coerceAtLeast(1)
        return ((levelMb - minMb).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    }

    fun yPositionToGainMb(
        yPx: Float,
        topPadPx: Float,
        graphHeightPx: Float,
        minMb: Int,
        maxMb: Int,
    ): Int {
        val usableHeight = graphHeightPx.coerceAtLeast(1f)
        val fractionFromBottom = 1f - ((yPx - topPadPx) / usableHeight).coerceIn(0f, 1f)
        val span = (maxMb - minMb).coerceAtLeast(1)
        return (minMb + fractionFromBottom * span).roundToInt().coerceIn(minMb, maxMb)
    }

    fun findNearestBandIndex(xPx: Float, bandXPositions: List<Float>): Int {
        if (bandXPositions.isEmpty()) return -1
        var bestIndex = 0
        var bestDist = abs(xPx - bandXPositions[0])
        for (i in 1 until bandXPositions.size) {
            val dist = abs(xPx - bandXPositions[i])
            if (dist < bestDist) {
                bestDist = dist
                bestIndex = i
            }
        }
        return bestIndex
    }

    fun bandXPositions(bandCount: Int, leftPadPx: Float, usableWidthPx: Float): List<Float> {
        if (bandCount <= 0) return emptyList()
        if (bandCount == 1) return listOf(leftPadPx + usableWidthPx / 2f)
        val step = usableWidthPx / (bandCount - 1).toFloat()
        return List(bandCount) { idx -> leftPadPx + idx * step }
    }

    fun formatFrequencyLabel(freqHz: Int): String = if (freqHz >= 1000) {
        val khz = freqHz / 1000f
        if (khz % 1f == 0f) "${khz.toInt()}k" else String.format(java.util.Locale.US, "%.1fk", khz)
    } else {
        "$freqHz"
    }

    fun formatGainDb(levelMb: Int): String {
        val db = (levelMb / 100f).roundToInt()
        return if (db > 0) "+$db dB" else "$db dB"
    }
}

/**
 * Interactive Liquid Glass frequency response graph bound to Android's real Equalizer bands.
 * Dynamically adapts to any number of hardware bands and supported millibel range.
 */
@Composable
fun EqualizerFrequencyGraph(
    bandCenterFreqHz: List<Int>,
    bandLevelsMb: List<Int>,
    minBandLevelMb: Int,
    maxBandLevelMb: Int,
    enabled: Boolean,
    onBandLevelChanged: (bandIndex: Int, levelMb: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing
    val glass = SoniqTheme.glass
    val bandCount = bandCenterFreqHz.size
    if (bandCount == 0) return

    var activeBandIndex by remember { mutableIntStateOf(-1) }
    val currentLevels by rememberUpdatedState(bandLevelsMb)
    val currentOnBandLevelChanged by rememberUpdatedState(onBandLevelChanged)

    val maxDbText = remember(maxBandLevelMb) { EqualizerGraphMath.formatGainDb(maxBandLevelMb) }
    val minDbText = remember(minBandLevelMb) { EqualizerGraphMath.formatGainDb(minBandLevelMb) }

    SoniqGlassCard(
        elevated = true,
        contentPadding = PaddingValues(spacing.md),
        modifier = modifier
            .alpha(if (enabled) 1f else 0.52f)
            .testTag("eq_frequency_graph"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            // Top bar inside graph card: live band readout while dragging or gain span hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.audio_effects_graph_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val badgeText = if (activeBandIndex in 0 until bandCount) {
                    val freq = bandCenterFreqHz[activeBandIndex]
                    val level = currentLevels.getOrElse(activeBandIndex) { 0 }
                    val freqLabel = if (freq >= 1000) {
                        stringResource(R.string.audio_effects_band_khz, freq / 1000f)
                    } else {
                        stringResource(R.string.audio_effects_band_hz, freq)
                    }
                    "$freqLabel • ${EqualizerGraphMath.formatGainDb(level)}"
                } else {
                    "$minDbText .. $maxDbText"
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (activeBandIndex >= 0) {
                                glass.activePillStart.copy(alpha = 0.28f)
                            } else {
                                glass.surfaceBottom
                            },
                        )
                        .border(1.dp, glass.borderSubtle, CircleShape)
                        .padding(horizontal = spacing.sm, vertical = spacing.xxs),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (activeBandIndex >= 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Interactive Canvas graph
            val gridColor = glass.borderSubtle
            val zeroLineColor = glass.borderHighlight
            val curveStart = glass.scrubberStart
            val curveEnd = glass.scrubberEnd
            val graphDesc = stringResource(R.string.audio_effects_graph_title)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(glass.surfaceBottom.copy(alpha = 0.55f))
                    .border(1.dp, glass.borderSubtle, RoundedCornerShape(16.dp))
                    .semantics { contentDescription = graphDesc }
                    .pointerInput(bandCount, minBandLevelMb, maxBandLevelMb, enabled) {
                        if (!enabled) return@pointerInput
                        val padX = 24.dp.toPx()
                        val padY = 22.dp.toPx()
                        val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                        val usableH = (size.height - padY * 2f).coerceAtLeast(1f)
                        val xPositions = EqualizerGraphMath.bandXPositions(bandCount, padX, usableW)

                        detectTapGestures { offset ->
                            val band = EqualizerGraphMath.findNearestBandIndex(offset.x, xPositions)
                            if (band in 0 until bandCount) {
                                val nextMb = EqualizerGraphMath.yPositionToGainMb(
                                    yPx = offset.y,
                                    topPadPx = padY,
                                    graphHeightPx = usableH,
                                    minMb = minBandLevelMb,
                                    maxMb = maxBandLevelMb,
                                )
                                currentOnBandLevelChanged(band, nextMb)
                            }
                        }
                    }
                    .pointerInput(bandCount, minBandLevelMb, maxBandLevelMb, enabled) {
                        if (!enabled) return@pointerInput
                        val padX = 24.dp.toPx()
                        val padY = 22.dp.toPx()
                        val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                        val usableH = (size.height - padY * 2f).coerceAtLeast(1f)
                        val xPositions = EqualizerGraphMath.bandXPositions(bandCount, padX, usableW)

                        detectDragGestures(
                            onDragStart = { startOffset ->
                                activeBandIndex = EqualizerGraphMath.findNearestBandIndex(startOffset.x, xPositions)
                                if (activeBandIndex in 0 until bandCount) {
                                    val nextMb = EqualizerGraphMath.yPositionToGainMb(
                                        yPx = startOffset.y,
                                        topPadPx = padY,
                                        graphHeightPx = usableH,
                                        minMb = minBandLevelMb,
                                        maxMb = maxBandLevelMb,
                                    )
                                    currentOnBandLevelChanged(activeBandIndex, nextMb)
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val band = activeBandIndex
                                if (band in 0 until bandCount) {
                                    val nextMb = EqualizerGraphMath.yPositionToGainMb(
                                        yPx = change.position.y,
                                        topPadPx = padY,
                                        graphHeightPx = usableH,
                                        minMb = minBandLevelMb,
                                        maxMb = maxBandLevelMb,
                                    )
                                    currentOnBandLevelChanged(band, nextMb)
                                }
                            },
                            onDragEnd = { activeBandIndex = -1 },
                            onDragCancel = { activeBandIndex = -1 },
                        )
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val padX = 24.dp.toPx()
                    val padY = 22.dp.toPx()
                    val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                    val usableH = (size.height - padY * 2f).coerceAtLeast(1f)
                    val xPositions = EqualizerGraphMath.bandXPositions(bandCount, padX, usableW)

                    // Subtle horizontal grid lines (top, +50%, 0 dB, -50%, bottom)
                    val horizontalSteps = 4
                    for (step in 0..horizontalSteps) {
                        val y = padY + (usableH * step / horizontalSteps.toFloat())
                        val isCenter = step == horizontalSteps / 2
                        drawLine(
                            color = if (isCenter) zeroLineColor else gridColor,
                            start = Offset(padX, y),
                            end = Offset(size.width - padX, y),
                            strokeWidth = if (isCenter) 1.5.dp.toPx() else 1.dp.toPx(),
                        )
                    }

                    // Subtle vertical grid lines per band
                    xPositions.forEach { x ->
                        drawLine(
                            color = gridColor,
                            start = Offset(x, padY),
                            end = Offset(x, size.height - padY),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    // Compute control points from actual Equalizer band levels
                    val points = List(bandCount) { idx ->
                        val level = currentLevels.getOrElse(idx) { 0 }
                        val frac = EqualizerGraphMath.gainMbToFraction(level, minBandLevelMb, maxBandLevelMb)
                        val y = padY + (1f - frac) * usableH
                        Offset(xPositions[idx], y)
                    }

                    if (points.isNotEmpty()) {
                        val curvePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.lastIndex) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val midX = (p0.x + p1.x) / 2f
                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            }
                        }

                        val bottomY = size.height - padY
                        val fillPath = Path().apply {
                            addPath(curvePath)
                            lineTo(points.last().x, bottomY)
                            lineTo(points.first().x, bottomY)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    curveStart.copy(alpha = 0.34f),
                                    curveEnd.copy(alpha = 0.14f),
                                    Color.Transparent,
                                ),
                                startY = padY,
                                endY = bottomY,
                            ),
                        )

                        drawPath(
                            path = curvePath,
                            brush = Brush.horizontalGradient(listOf(curveStart, curveEnd)),
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )

                        // Circular draggable control points
                        points.forEachIndexed { idx, pt ->
                            val isActive = idx == activeBandIndex
                            val t = if (bandCount > 1) idx / (bandCount - 1).toFloat() else 0.5f
                            val pointColor = androidx.compose.ui.graphics.lerp(curveStart, curveEnd, t)

                            drawCircle(
                                color = pointColor.copy(alpha = if (isActive) 0.36f else 0.20f),
                                radius = if (isActive) 18.dp.toPx() else 12.dp.toPx(),
                                center = pt,
                            )
                            drawCircle(
                                color = pointColor,
                                radius = if (isActive) 8.dp.toPx() else 6.5.dp.toPx(),
                                center = pt,
                            )
                            drawCircle(
                                color = Color.White,
                                radius = if (isActive) 8.dp.toPx() else 6.5.dp.toPx(),
                                center = pt,
                                style = Stroke(width = 1.75.dp.toPx()),
                            )
                        }
                    }
                }
            }

            // Dynamic frequency labels below the graph
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                bandCenterFreqHz.forEach { freqHz ->
                    Text(
                        text = EqualizerGraphMath.formatFrequencyLabel(freqHz),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
