package com.soniq.player.feature.music

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniq.player.R
import com.soniq.player.core.designsystem.component.SoniqEmptyState
import com.soniq.player.core.designsystem.component.SoniqScaffold
import com.soniq.player.core.designsystem.component.SoniqTopAppBar
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.permissions.MediaAccessState
import com.soniq.player.core.permissions.rememberMediaPermissionController

@Composable
fun MusicRoute(
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionController = rememberMediaPermissionController(
        kind = MediaKind.AUDIO,
        onResult = viewModel::onPermissionResult,
    )

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermission()
        onPauseOrDispose {}
    }

    MusicScreen(
        uiState = uiState,
        onRequestPermission = permissionController.requestPermission,
        onOpenAppSettings = permissionController.openAppSettings,
        modifier = modifier,
    )
}

@Composable
fun MusicScreen(
    uiState: MusicUiState,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoniqScaffold(
        modifier = modifier,
        topBar = { SoniqTopAppBar(title = stringResource(R.string.destination_music)) },
    ) { innerPadding ->
        when (uiState.accessState) {
            MediaAccessState.GRANTED -> {
                SoniqEmptyState(
                    icon = painterResource(R.drawable.ic_music_note),
                    title = stringResource(R.string.library_music_ready_title),
                    message = stringResource(R.string.library_indexing_pending),
                    modifier = Modifier.padding(innerPadding),
                )
            }

            MediaAccessState.NOT_GRANTED -> {
                SoniqEmptyState(
                    icon = painterResource(R.drawable.ic_lock),
                    title = stringResource(R.string.permission_music_title),
                    message = stringResource(R.string.permission_music_message),
                    actionLabel = stringResource(R.string.permission_grant),
                    onActionClick = onRequestPermission,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            MediaAccessState.DENIED_PERMANENTLY -> {
                SoniqEmptyState(
                    icon = painterResource(R.drawable.ic_lock),
                    title = stringResource(R.string.permission_music_title),
                    message = stringResource(R.string.permission_denied_permanently),
                    actionLabel = stringResource(R.string.permission_open_settings),
                    onActionClick = onOpenAppSettings,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}
