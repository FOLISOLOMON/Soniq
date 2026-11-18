package com.soniq.player.core.mediadiscovery

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import com.soniq.player.core.model.media.MediaKind
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes changes to Android's local MediaStore for a specific [MediaKind].
 *
 * Emits a signal whenever the system MediaStore inserts, updates, or deletes entries under the
 * observed collection so downstream repositories can schedule incremental re-scans.
 */
interface MediaStoreChangeObserver {
    fun observeChanges(kind: MediaKind): Flow<Unit>
}

@Singleton
class AndroidMediaStoreChangeObserver @Inject constructor(
    private val contentResolver: ContentResolver,
) : MediaStoreChangeObserver {

    override fun observeChanges(kind: MediaKind): Flow<Unit> = callbackFlow {
        val uri = MediaStoreCollections.collectionUri(kind)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        contentResolver.registerContentObserver(uri, true, observer)
        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }
}
