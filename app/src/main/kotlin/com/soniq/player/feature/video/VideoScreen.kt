package com.soniq.player.feature.video

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.soniq.player.R
import com.soniq.player.core.common.MediaFormatters
import com.soniq.player.core.designsystem.component.SoniqEmptyState
import com.soniq.player.core.designsystem.component.SoniqGlassCard
import com.soniq.player.core.designsystem.component.SoniqGlassIconButton
import com.soniq.player.core.designsystem.component.SoniqGlassLinearProgress
import com.soniq.player.core.designsystem.component.SoniqGlassPillChip
import com.soniq.player.core.designsystem.component.SoniqGlassSearchBar
import com.soniq.player.core.designsystem.component.SoniqScaffold
import com.soniq.player.core.designsystem.component.SoniqSurfaceCard
import com.soniq.player.core.designsystem.component.SoniqTopAppBar
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.model.media.MediaFolderSummary
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import com.soniq.player.core.model.media.VideoSortOrder
import com.soniq.player.core.permissions.MediaAccessState
import com.soniq.player.core.permissions.rememberMediaPermissionController

@Composable
fun VideoRoute(
    modifier: Modifier = Modifier,
    onPlayVideo: (VideoItem) -> Unit = {},
    viewModel: VideoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionController = rememberMediaPermissionController(
        kind = MediaKind.VIDEO,
        onResult = viewModel::onPermissionResult,
    )

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermission()
        onPauseOrDispose {}
    }

    VideoScreen(
        uiState = uiState,
        onRequestPermission = permissionController.requestPermission,
        onOpenAppSettings = permissionController.openAppSettings,
        onSelectTab = viewModel::selectTab,
        onSelectSortOrder = viewModel::selectSortOrder,
        onToggleSearch = viewModel::setSearchActive,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onFolderClick = viewModel::selectFolder,
        onClearFilter = viewModel::clearFilter,
        onRefresh = viewModel::refreshLibrary,
        onVideoClick = onPlayVideo,
        modifier = modifier,
    )
}

@Composable
fun VideoScreen(
    uiState: VideoUiState,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectTab: (VideoLibraryTab) -> Unit = {},
    onSelectSortOrder: (VideoSortOrder) -> Unit = {},
    onToggleSearch: (Boolean) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onFolderClick: (MediaFolderSummary) -> Unit = {},
    onClearFilter: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onVideoClick: (VideoItem) -> Unit = {},
) {
    val hasAccess = uiState.accessState == MediaAccessState.GRANTED ||
        uiState.accessState == MediaAccessState.PARTIAL
    var sortMenuExpanded by remember { mutableStateOf(false) }

    SoniqScaffold(
        modifier = modifier,
        topBar = {
            SoniqTopAppBar(
                title = uiState.activeFolder?.folderName ?: stringResource(R.string.destination_video),
                navigationIcon = {
                    if (uiState.activeFolder != null) {
                        SoniqGlassIconButton(
                            onClick = onClearFilter,
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.library_action_clear_filter),
                            size = 40.dp,
                            iconSize = SoniqTheme.tokens.sizes.iconSmall,
                            modifier = Modifier.padding(start = SoniqTheme.tokens.spacing.sm),
                        )
                    }
                },
                actions = {
                    if (hasAccess) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(SoniqTheme.tokens.spacing.xs),
                            modifier = Modifier.padding(end = SoniqTheme.tokens.spacing.sm),
                        ) {
                            SoniqGlassIconButton(
                                onClick = { onToggleSearch(!uiState.isSearchActive) },
                                painter = painterResource(
                                    if (uiState.isSearchActive) R.drawable.ic_close else R.drawable.ic_search,
                                ),
                                contentDescription = stringResource(
                                    if (uiState.isSearchActive) {
                                        R.string.library_action_close_search
                                    } else {
                                        R.string.library_action_search
                                    },
                                ),
                                highlighted = uiState.isSearchActive,
                                size = 40.dp,
                                iconSize = SoniqTheme.tokens.sizes.iconSmall,
                            )
                            Box {
                                SoniqGlassIconButton(
                                    onClick = { sortMenuExpanded = true },
                                    painter = painterResource(R.drawable.ic_sort),
                                    contentDescription = stringResource(R.string.library_action_sort),
                                    size = 40.dp,
                                    iconSize = SoniqTheme.tokens.sizes.iconSmall,
                                )
                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false },
                                ) {
                                    VideoSortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(order.labelRes())) },
                                            onClick = {
                                                sortMenuExpanded = false
                                                onSelectSortOrder(order)
                                            },
                                            trailingIcon = {
                                                if (order == uiState.sortOrder) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_check),
                                                        contentDescription = stringResource(R.string.cd_selected),
                                                    )
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                            SoniqGlassIconButton(
                                onClick = onRefresh,
                                painter = painterResource(R.drawable.ic_refresh),
                                contentDescription = stringResource(R.string.library_action_refresh),
                                size = 40.dp,
                                iconSize = SoniqTheme.tokens.sizes.iconSmall,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState.accessState) {
            MediaAccessState.NOT_GRANTED -> {
                SoniqEmptyState(
                    icon = painterResource(R.drawable.ic_lock),
                    title = stringResource(R.string.permission_video_title),
                    message = stringResource(R.string.permission_video_message),
                    actionLabel = stringResource(R.string.permission_grant),
                    onActionClick = onRequestPermission,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            MediaAccessState.DENIED_PERMANENTLY -> {
                SoniqEmptyState(
                    icon = painterResource(R.drawable.ic_lock),
                    title = stringResource(R.string.permission_video_title),
                    message = stringResource(R.string.permission_denied_permanently),
                    actionLabel = stringResource(R.string.permission_open_settings),
                    onActionClick = onOpenAppSettings,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            MediaAccessState.GRANTED,
            MediaAccessState.PARTIAL,
            -> {
                VideoLibraryContent(
                    uiState = uiState,
                    onManageSelectedVideos = onRequestPermission,
                    onSelectTab = onSelectTab,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onFolderClick = onFolderClick,
                    onRefresh = onRefresh,
                    onVideoClick = onVideoClick,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun VideoLibraryContent(
    uiState: VideoUiState,
    onManageSelectedVideos: () -> Unit,
    onSelectTab: (VideoLibraryTab) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFolderClick: (MediaFolderSummary) -> Unit,
    onRefresh: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (uiState.accessState == MediaAccessState.PARTIAL) {
            SoniqSurfaceCard(
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.video_partial_access_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onManageSelectedVideos) {
                        Text(text = stringResource(R.string.permission_manage_video_access))
                    }
                }
            }
        }

        if (uiState.isSearchActive) {
            SoniqGlassSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChanged,
                placeholder = stringResource(R.string.video_search_hint),
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
            )
        }

        if (uiState.activeFolder == null) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = spacing.screenHorizontal, vertical = spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(items = VideoLibraryTab.entries, key = { it.name }) { tab ->
                    SoniqGlassPillChip(
                        label = stringResource(tab.labelRes()),
                        selected = tab == uiState.selectedTab,
                        onClick = { onSelectTab(tab) },
                    )
                }
            }
        }

        if (uiState.hasSyncError) {
            SoniqSurfaceCard(
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.library_sync_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRefresh) {
                        Text(text = stringResource(R.string.library_action_retry))
                    }
                }
            }
        }

        val showVideos = uiState.activeFolder != null || uiState.selectedTab == VideoLibraryTab.VIDEOS
        if (showVideos) {
            VideoListSection(
                videos = uiState.videos,
                resumePositions = uiState.resumePositions,
                hasSearchQuery = uiState.searchQuery.isNotBlank(),
                onRefresh = onRefresh,
                onVideoClick = onVideoClick,
            )
        } else {
            VideoFolderListSection(
                folders = uiState.folders,
                hasSearchQuery = uiState.searchQuery.isNotBlank(),
                onRefresh = onRefresh,
                onFolderClick = onFolderClick,
            )
        }
    }
}

@Composable
private fun VideoListSection(
    videos: List<VideoItem>,
    resumePositions: Map<Long, com.soniq.player.core.model.media.MediaResumePosition>,
    hasSearchQuery: Boolean,
    onRefresh: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
) {
    if (videos.isEmpty()) {
        VideoEmptyView(hasSearchQuery = hasSearchQuery, onRefresh = onRefresh)
        return
    }
    val spacing = SoniqTheme.tokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = spacing.screenHorizontal,
            vertical = spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items(items = videos, key = { it.id }) { video ->
            VideoItemRow(
                video = video,
                resumeProgress = resumePositions[video.id]?.progressFraction,
                onClick = { onVideoClick(video) },
            )
        }
    }
}

@Composable
private fun VideoFolderListSection(
    folders: List<MediaFolderSummary>,
    hasSearchQuery: Boolean,
    onRefresh: () -> Unit,
    onFolderClick: (MediaFolderSummary) -> Unit,
) {
    if (folders.isEmpty()) {
        VideoEmptyView(hasSearchQuery = hasSearchQuery, onRefresh = onRefresh)
        return
    }
    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = spacing.screenHorizontal,
            vertical = spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items(items = folders, key = { it.folderPath }) { folder ->
            SoniqGlassCard(
                onClick = { onFolderClick(folder) },
                contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.sm + 2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val thumbShape = RoundedCornerShape(SoniqTheme.glassMetrics.thumbnailRadius)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(sizes.trackArtwork)
                            .clip(thumbShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        SoniqTheme.glass.surfaceElevated,
                                        SoniqTheme.glass.surfaceBottom,
                                    ),
                                ),
                            )
                            .border(SoniqTheme.glassMetrics.borderWidth, SoniqTheme.glass.borderSubtle, thumbShape),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_folder),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(sizes.icon),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.folderName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = folder.folderPath.ifEmpty { folder.folderName },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = videoCountLabel(folder.itemCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoItemRow(
    video: VideoItem,
    resumeProgress: Float?,
    onClick: () -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    val alpha = SoniqTheme.tokens.alpha
    val resolution = MediaFormatters.formatResolution(video.width, video.height)
    val metadataLine = buildList {
        if (resolution != null) add(resolution)
        add(MediaFormatters.formatFileSize(video.sizeBytes))
        add(video.folderName)
    }.joinToString(separator = " • ")

    SoniqGlassCard(
        onClick = onClick,
        contentPadding = PaddingValues(spacing.sm + 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbShape = RoundedCornerShape(SoniqTheme.glassMetrics.thumbnailRadius)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = sizes.videoThumbnailWidth, height = sizes.videoThumbnailHeight)
                    .clip(thumbShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                SoniqTheme.glass.surfaceElevated,
                                SoniqTheme.glass.surfaceBottom,
                            ),
                        ),
                    )
                    .border(SoniqTheme.glassMetrics.borderWidth, SoniqTheme.glass.borderSubtle, thumbShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_movie),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(sizes.icon),
                )
                AsyncImage(
                    model = video.contentUri.toUri(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    text = MediaFormatters.formatDuration(video.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(spacing.xs)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = alpha.mediaBadgeScrim))
                        .padding(horizontal = spacing.xs, vertical = spacing.xxs),
                )
                if (resumeProgress != null && resumeProgress > 0f) {
                    SoniqGlassLinearProgress(
                        progress = resumeProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                    )
                }
            }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metadataLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        }
    }
}

@Composable
private fun VideoEmptyView(
    hasSearchQuery: Boolean,
    onRefresh: () -> Unit,
) {
    if (hasSearchQuery) {
        SoniqEmptyState(
            icon = painterResource(R.drawable.ic_search),
            title = stringResource(R.string.library_no_search_matches_title),
            message = stringResource(R.string.library_no_search_matches_message),
        )
    } else {
        SoniqEmptyState(
            icon = painterResource(R.drawable.ic_movie),
            title = stringResource(R.string.video_empty_title),
            message = stringResource(R.string.video_empty_message),
            actionLabel = stringResource(R.string.library_action_refresh),
            onActionClick = onRefresh,
        )
    }
}

@Composable
private fun videoCountLabel(count: Int): String =
    pluralStringResource(R.plurals.library_video_count, count, count)

private fun VideoLibraryTab.labelRes(): Int = when (this) {
    VideoLibraryTab.VIDEOS -> R.string.video_tab_videos
    VideoLibraryTab.FOLDERS -> R.string.video_tab_folders
}

private fun VideoSortOrder.labelRes(): Int = when (this) {
    VideoSortOrder.DATE_ADDED_DESC -> R.string.video_sort_date_added
    VideoSortOrder.TITLE_ASC -> R.string.video_sort_title
    VideoSortOrder.DURATION_DESC -> R.string.video_sort_duration
    VideoSortOrder.SIZE_DESC -> R.string.video_sort_size
}
