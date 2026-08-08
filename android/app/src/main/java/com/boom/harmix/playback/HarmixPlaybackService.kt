package com.boom.harmix.playback

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.boom.harmix.extractor.YtDlpRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import javax.inject.Inject

@AndroidEntryPoint
class HarmixPlaybackService : MediaLibraryService() {

    @Inject
    lateinit var ytDlpRepository: YtDlpRepository

    private lateinit var player: ExoPlayer
    private var librarySession: MediaLibrarySession? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            // Setting this tells ExoPlayer to request and manage audio focus itself.
            // It will automatically pause on incoming calls, duck for notifications,
            // and resume when focus is regained!
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true) // pause when headphones unplugged
            .build()

        librarySession = MediaLibrarySession.Builder(this, player, HarmixLibrarySessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return librarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val session = librarySession ?: return
        if (!session.player.isPlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        librarySession?.run {
            player.release()
            release()
            librarySession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showErrorToast(message: String) {
        mainHandler.post {
            Toast.makeText(this@HarmixPlaybackService, "Extraction Error: $message", Toast.LENGTH_LONG).show()
        }
    }

    private inner class HarmixLibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> =
            serviceScope.future {
                mediaItems.map { item -> resolvePlayableItem(item) }.toMutableList()
            }

        private suspend fun resolvePlayableItem(item: MediaItem): MediaItem {
        val sourceIdentifier = item.requestMetadata.mediaUri?.toString() ?: item.mediaId
        
        // VISUAL DIAGNOSTIC 1: Tell the user the background service is working
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(applicationContext, "Fetching Audio...", android.widget.Toast.LENGTH_SHORT).show()
        }

        return try {
            val result = ytDlpRepository.getAudioStreamUrl(sourceIdentifier)
            val updatedMetadata = if (result.durationSeconds != null) {
                val extras = (item.mediaMetadata.extras ?: android.os.Bundle()).apply {
                    putLong("harmix_duration_ms", result.durationSeconds.toLong() * 1000L)
                }
                item.mediaMetadata.buildUpon().setExtras(extras).build()
            } else {
                item.mediaMetadata
            }
            item.buildUpon()
                .setUri(result.url)
                .setMediaMetadata(updatedMetadata)
                .build()
        } catch (e: Exception) {
            // VISUAL DIAGNOSTIC 2: Scream the exact error directly to the screen!
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(applicationContext, "Extractor Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
            item
        }
    }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("harmix_root")
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle("Harmix")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return Futures.immediateFuture(
                LibraryResult.ofItem(rootItem, params)
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return Futures.immediateFuture(
                LibraryResult.ofItemList(
                    ImmutableList.of(),
                    params
                )
            )
        }
    }
}
