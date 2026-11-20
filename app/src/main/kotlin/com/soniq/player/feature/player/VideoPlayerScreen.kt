package com.soniq.player.feature.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import com.soniq.player.R
import com.soniq.player.core.common.MediaFormatters
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.designsystem.token.SoniqNeutrals
import com.soniq.player.core.playback.PlaybackState
import com.soniq.player.core.playback.VideoResizeMode
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private const val VIDEO_SEEK_STEP_MS = 10_000L
private const val HUD_AUTO_HIDE_MS = 900L
private const val HORIZONTAL_SEEK_FULL_SWIPE_MS = 90_000L
private val VIDEO_SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

private enum class GestureAxis { NONE, HORIZONTAL, VERTICAL_LEFT, VERTICAL_RIGHT }

/**
 * Full-screen local Video playback experience.
 *
 * Binds a Media3 Compose [PlayerSurface] to the service-owned [player] without taking ownership
 * of the playback engine lifecycle. Supports Fit/Fill/Zoom aspect modes, embedded & external
 * subtitles, audio track selection, speed controls, immersive orientation toggling, and
 * brightness/volume/seek gestures.
 */
@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    player: Player?,
    playbackState: PlaybackState,
    onNavigateBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onLeaveScreen: () -> Unit,
    modifier: Modifier = Modifier,
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {},
    onSetSpeed: (Float) -> Unit = {},
    onSelectAudioTrack: (Int, Int) -> Unit = { _, _ -> },
    onSelectSubtitleTrack: (Int?, Int?) -> Unit = { _, _ -> },
    onAddExternalSubtitle: (String, String, String) -> Unit = { _, _, _ -> },
    onClearError: () -> Unit = {},
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    val alpha = SoniqTheme.tokens.alpha

    var controlsVisible by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableStateOf(VideoResizeMode.FIT) }
    var isLandscapeLocked by remember { mutableStateOf(false) }
    var showTracksSheet by remember { mutableStateOf(false) }
    var showSpeedRow by remember { mutableStateOf(false) }

    // Gesture HUD state
    var hudLabel by remember { mutableStateOf<String?>(null) }
    var hudVersion by remember { mutableIntStateOf(0) }
    var gestureSeekPreviewMs by remember { mutableLongStateOf(-1L) }

    LaunchedEffect(hudVersion) {
        if (hudLabel != null) {
            delay(HUD_AUTO_HIDE_MS)
            hudLabel = null
        }
    }

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val raw = uri.toString()
            val mimeType = if (raw.endsWith(".vtt", ignoreCase = true)) {
                MimeTypes.TEXT_VTT
            } else {
                MimeTypes.APPLICATION_SUBRIP
            }
            val label = uri.lastPathSegment?.substringAfterLast('/') ?: "External subtitle"
            onAddExternalSubtitle(raw, mimeType, label)
            showTracksSheet = false
        }
    }

    // Immersive fullscreen bars & orientation cleanup
    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onLeaveScreen()
        }
    }

    val seekBackText = stringResource(R.string.video_gesture_seek_back)
    val seekForwardText = stringResource(R.string.video_gesture_seek_forward)
    val aspectFitLabel = stringResource(R.string.video_aspect_fit)
    val aspectFillLabel = stringResource(R.string.video_aspect_fill)
    val aspectZoomLabel = stringResource(R.string.video_aspect_zoom)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SoniqNeutrals.AmoledBackground)
            .clipToBounds()
            .pointerInput(playbackState.durationMs, playbackState.positionMs) {
                val widthPx = size.width.toFloat().coerceAtLeast(1f)
                val heightPx = size.height.toFloat().coerceAtLeast(1f)
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { offset ->
                        if (offset.x < widthPx / 2f) {
                            onSeekBy(-VIDEO_SEEK_STEP_MS)
                            hudLabel = seekBackText
                        } else {
                            onSeekBy(VIDEO_SEEK_STEP_MS)
                            hudLabel = seekForwardText
                        }
                        hudVersion++
                    },
                )
            }
            .pointerInput(playbackState.durationMs) {
                val widthPx = size.width.toFloat().coerceAtLeast(1f)
                val heightPx = size.height.toFloat().coerceAtLeast(1f)
                var axis = GestureAxis.NONE
                var startBrightness = 0.5f
                var startVolume = 0f
                var startSeekMs = 0L
                var accumX = 0f
                var accumY = 0f

                detectDragGestures(
                    onDragStart = { startOffset ->
                        axis = GestureAxis.NONE
                        accumX = 0f
                        accumY = 0f
                        startSeekMs = playbackState.positionMs
                        startBrightness = activity?.window?.attributes?.screenBrightness
                            ?.takeIf { it in 0.01f..1.0f } ?: 0.5f
                        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
                        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
                        startVolume = curVol.toFloat() / maxVol.toFloat()
                        if (startOffset.x < widthPx / 2f) {
                            axis = GestureAxis.VERTICAL_LEFT
                        } else {
                            axis = GestureAxis.VERTICAL_RIGHT
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumX += dragAmount.x
                        accumY += dragAmount.y
                        if (axis != GestureAxis.HORIZONTAL && abs(accumX) > abs(accumY) * 1.5f && abs(accumX) > 16f) {
                            axis = GestureAxis.HORIZONTAL
                        }
                        when (axis) {
                            GestureAxis.HORIZONTAL -> {
                                val deltaMs = ((accumX / widthPx) * HORIZONTAL_SEEK_FULL_SWIPE_MS).toLong()
                                val maxDur = playbackState.durationMs.coerceAtLeast(0L)
                                val preview = if (maxDur > 0L) {
                                    (startSeekMs + deltaMs).coerceIn(0L, maxDur)
                                } else {
                                    (startSeekMs + deltaMs).coerceAtLeast(0L)
                                }
                                gestureSeekPreviewMs = preview
                                hudLabel = "${MediaFormatters.formatDuration(preview)} / ${MediaFormatters.formatDuration(maxDur)}"
                                hudVersion++
                            }
                            GestureAxis.VERTICAL_LEFT -> {
                                val delta = -accumY / (heightPx * 0.7f)
                                val nextBrightness = (startBrightness + delta).coerceIn(0.05f, 1.0f)
                                activity?.window?.let { win ->
                                    val lp = win.attributes
                                    lp.screenBrightness = nextBrightness
                                    win.attributes = lp
                                }
                                hudLabel = resources.getString(
                                    R.string.video_gesture_brightness,
                                    (nextBrightness * 100f).roundToInt(),
                                )
                                hudVersion++
                            }
                            GestureAxis.VERTICAL_RIGHT -> {
                                val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
                                val delta = -accumY / (heightPx * 0.7f)
                                val nextRatio = (startVolume + delta).coerceIn(0f, 1f)
                                val targetVol = (nextRatio * maxVol).roundToInt().coerceIn(0, maxVol)
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                hudLabel = resources.getString(
                                    R.string.video_gesture_volume,
                                    (nextRatio * 100f).roundToInt(),
                                )
                                hudVersion++
                            }
                            GestureAxis.NONE -> Unit
                        }
                    },
                    onDragEnd = {
                        if (axis == GestureAxis.HORIZONTAL && gestureSeekPreviewMs >= 0L) {
                            onSeekTo(gestureSeekPreviewMs)
                        }
                        gestureSeekPreviewMs = -1L
                        axis = GestureAxis.NONE
                    },
                    onDragCancel = {
                        gestureSeekPreviewMs = -1L
                        axis = GestureAxis.NONE
                    },
                )
            },
    ) {
        val density = LocalDensity.current
        val boxWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val boxHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val videoRatio = if (playbackState.videoWidth > 0 && playbackState.videoHeight > 0) {
            playbackState.videoWidth.toFloat() / playbackState.videoHeight.toFloat()
        } else {
            16f / 9f
        }
        val boxRatio = boxWidthPx / boxHeightPx

        val surfaceModifier = when (resizeMode) {
            VideoResizeMode.FILL -> Modifier.fillMaxSize()
            VideoResizeMode.FIT -> {
                val (wPx, hPx) = if (videoRatio > boxRatio) {
                    boxWidthPx to (boxWidthPx / videoRatio)
                } else {
                    (boxHeightPx * videoRatio) to boxHeightPx
                }
                with(density) {
                    Modifier
                        .width(wPx.toDp())
                        .height(hPx.toDp())
                        .align(Alignment.Center)
                }
            }
            VideoResizeMode.ZOOM -> {
                val (wPx, hPx) = if (videoRatio > boxRatio) {
                    (boxHeightPx * videoRatio) to boxHeightPx
                } else {
                    boxWidthPx to (boxWidthPx / videoRatio)
                }
                with(density) {
                    Modifier
                        .width(wPx.toDp())
                        .height(hPx.toDp())
                        .align(Alignment.Center)
                }
            }
        }

        if (player != null) {
            PlayerSurface(
                player = player,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                modifier = surfaceModifier,
            )
        }

        // Subtitle cue overlay
        if (playbackState.activeSubtitleCues.isNotEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = SoniqNeutrals.AmoledBackground.copy(alpha = alpha.videoControlsScrim),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = spacing.lg,
                        end = spacing.lg,
                        bottom = if (controlsVisible) spacing.xxxl + spacing.xxl else spacing.xl,
                    ),
            ) {
                Text(
                    text = playbackState.activeSubtitleCues.joinToString("\n"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SoniqNeutrals.DarkOnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
                )
            }
        }

        // Gesture HUD pill
        hudLabel?.let { label ->
            Surface(
                shape = CircleShape,
                color = SoniqNeutrals.AmoledBackground.copy(alpha = alpha.videoControlsScrim),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
                    .padding(top = spacing.xxxl),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = SoniqNeutrals.DarkOnSurface,
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                )
            }
        }

        // Playback error overlay (e.g., deleted or corrupt local file)
        playbackState.playbackError?.let { errorMsg ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(spacing.lg),
            ) {
                Row(
                    modifier = Modifier.padding(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearError) {
                        Text(stringResource(R.string.playback_error_dismiss))
                    }
                }
            }
        }

        if (controlsVisible && !isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoniqNeutrals.AmoledBackground.copy(alpha = alpha.videoControlsScrim))
                    .systemBarsPadding()
                    .padding(spacing.lg),
            ) {
                // Top bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.player_action_close),
                                tint = SoniqNeutrals.DarkOnSurface,
                            )
                        }
                        Text(
                            text = playbackState.title ?: stringResource(R.string.destination_video),
                            style = MaterialTheme.typography.titleMedium,
                            color = SoniqNeutrals.DarkOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalButton(
                            onClick = {
                                if (showSpeedRow) onCycleSpeed() else showSpeedRow = true
                            },
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.player_speed_label,
                                    PlaybackViewModel.formatSpeed(playbackState.playbackSpeed),
                                ),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onEnterPip) {
                            Icon(
                                painter = painterResource(R.drawable.ic_picture_in_picture),
                                contentDescription = stringResource(R.string.video_pip_action),
                                tint = SoniqNeutrals.DarkOnSurface,
                            )
                        }
                        IconButton(onClick = { showTracksSheet = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_subtitles),
                                contentDescription = stringResource(R.string.video_tracks_title),
                                tint = SoniqNeutrals.DarkOnSurface,
                            )
                        }
                        IconButton(
                            onClick = {
                                resizeMode = when (resizeMode) {
                                    VideoResizeMode.FIT -> VideoResizeMode.FILL
                                    VideoResizeMode.FILL -> VideoResizeMode.ZOOM
                                    VideoResizeMode.ZOOM -> VideoResizeMode.FIT
                                }
                                hudLabel = when (resizeMode) {
                                    VideoResizeMode.FIT -> aspectFitLabel
                                    VideoResizeMode.FILL -> aspectFillLabel
                                    VideoResizeMode.ZOOM -> aspectZoomLabel
                                }
                                hudVersion++
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_aspect_ratio),
                                contentDescription = stringResource(R.string.video_aspect_mode_label),
                                tint = SoniqNeutrals.DarkOnSurface,
                            )
                        }
                        IconButton(
                            onClick = {
                                isLandscapeLocked = !isLandscapeLocked
                                activity?.requestedOrientation = if (isLandscapeLocked) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_screen_rotation),
                                contentDescription = stringResource(R.string.video_orientation_toggle),
                                tint = SoniqNeutrals.DarkOnSurface,
                            )
                        }
                    }

                    if (showSpeedRow) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        ) {
                            VIDEO_SPEED_OPTIONS.forEach { speed ->
                                FilterChip(
                                    selected = abs(playbackState.playbackSpeed - speed) < 0.03f,
                                    onClick = {
                                        onSetSpeed(speed)
                                        showSpeedRow = false
                                    },
                                    label = {
                                        Text(
                                            stringResource(
                                                R.string.player_speed_label,
                                                PlaybackViewModel.formatSpeed(speed),
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                // Center transport controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xl),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onSeekBy(-VIDEO_SEEK_STEP_MS) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_replay_10),
                            contentDescription = stringResource(R.string.player_action_rewind),
                            tint = SoniqNeutrals.DarkOnSurface,
                            modifier = Modifier.size(sizes.iconMedium),
                        )
                    }
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        shape = CircleShape,
                        modifier = Modifier.size(sizes.primaryTransportButton),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (playbackState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                            ),
                            contentDescription = stringResource(
                                if (playbackState.isPlaying) R.string.player_action_pause else R.string.player_action_play,
                            ),
                            modifier = Modifier.size(sizes.iconMedium),
                        )
                    }
                    IconButton(onClick = { onSeekBy(VIDEO_SEEK_STEP_MS) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_forward_10),
                            contentDescription = stringResource(R.string.player_action_forward),
                            tint = SoniqNeutrals.DarkOnSurface,
                            modifier = Modifier.size(sizes.iconMedium),
                        )
                    }
                }

                // Bottom seek scrubber
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    PlaybackScrubber(
                        positionMs = if (gestureSeekPreviewMs >= 0L) gestureSeekPreviewMs else playbackState.positionMs,
                        durationMs = playbackState.durationMs,
                        onSeekTo = onSeekTo,
                    )
                }
            }
        }
    }

    if (showTracksSheet) {
        ModalBottomSheet(onDismissRequest = { showTracksSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.video_tracks_title),
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    text = stringResource(R.string.video_subtitles_section),
                    style = MaterialTheme.typography.titleMedium,
                )

                val anySubtitleSelected = playbackState.availableSubtitleTracks.any { it.isSelected }
                TrackChoiceRow(
                    label = stringResource(R.string.video_subtitles_off),
                    selected = !anySubtitleSelected,
                    onClick = {
                        onSelectSubtitleTrack(null, null)
                        showTracksSheet = false
                    },
                )
                playbackState.availableSubtitleTracks.forEach { track ->
                    TrackChoiceRow(
                        label = track.label,
                        selected = track.isSelected,
                        onClick = {
                            onSelectSubtitleTrack(track.groupIndex, track.trackIndex)
                            showTracksSheet = false
                        },
                    )
                }
                OutlinedButton(
                    onClick = {
                        subtitlePickerLauncher.launch(
                            arrayOf("application/x-subrip", "text/vtt", "text/plain", "*/*"),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.video_subtitles_load_external))
                }

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.video_audio_tracks_section),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (playbackState.availableAudioTracks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.video_audio_tracks_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    playbackState.availableAudioTracks.forEach { track ->
                        TrackChoiceRow(
                            label = track.label,
                            selected = track.isSelected,
                            onClick = {
                                onSelectAudioTrack(track.groupIndex, track.trackIndex)
                                showTracksSheet = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.cd_selected),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
