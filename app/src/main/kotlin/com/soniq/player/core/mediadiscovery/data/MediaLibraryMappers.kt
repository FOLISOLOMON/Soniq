package com.soniq.player.core.mediadiscovery.data

import com.soniq.player.core.database.media.entity.AudioTrackEntity
import com.soniq.player.core.database.media.entity.VideoItemEntity
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.VideoItem

internal fun AudioTrackEntity.toDomain(): AudioTrack = AudioTrack(
    id = id,
    contentUri = contentUri,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    albumArtUri = albumArtUri,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    folderPath = folderPath,
    folderName = folderName,
    trackNumber = trackNumber,
    year = year,
    dateAddedSeconds = dateAddedSeconds,
    dateModifiedSeconds = dateModifiedSeconds,
)

internal fun AudioTrack.toEntity(): AudioTrackEntity = AudioTrackEntity(
    id = id,
    contentUri = contentUri,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    albumArtUri = albumArtUri,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    folderPath = folderPath,
    folderName = folderName,
    trackNumber = trackNumber,
    year = year,
    dateAddedSeconds = dateAddedSeconds,
    dateModifiedSeconds = dateModifiedSeconds,
)

internal fun VideoItemEntity.toDomain(): VideoItem = VideoItem(
    id = id,
    contentUri = contentUri,
    title = title,
    displayName = displayName,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    width = width,
    height = height,
    folderPath = folderPath,
    folderName = folderName,
    dateAddedSeconds = dateAddedSeconds,
    dateModifiedSeconds = dateModifiedSeconds,
)

internal fun VideoItem.toEntity(): VideoItemEntity = VideoItemEntity(
    id = id,
    contentUri = contentUri,
    title = title,
    displayName = displayName,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    width = width,
    height = height,
    folderPath = folderPath,
    folderName = folderName,
    dateAddedSeconds = dateAddedSeconds,
    dateModifiedSeconds = dateModifiedSeconds,
)
