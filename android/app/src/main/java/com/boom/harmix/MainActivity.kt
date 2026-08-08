package com.boom.harmix

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.boom.harmix.data.local.LibraryRepository
import com.boom.harmix.data.local.PlaylistUi
import com.boom.harmix.extractor.StreamItem
import com.boom.harmix.metadata.LyricsResult
import com.boom.harmix.metadata.MetadataRepository
import com.boom.harmix.playback.HarmixPlaybackService
import com.boom.harmix.playback.QueueItemUi
import com.boom.harmix.ui.screens.MainScreen
import com.boom.harmix.ui.theme.HarmixTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var libraryRepository: LibraryRepository

    @Inject
    lateinit var metadataRepository: MetadataRepository

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var currentSongTitle by mutableStateOf("Nothing playing")
    private var currentArtist by mutableStateOf("")
    private var currentArtworkUrl by mutableStateOf<String?>(null)
    private var currentTrackUrl by mutableStateOf<String?>(null)
    private var isPlaying by mutableStateOf(false)
    private var currentPositionMs by mutableLongStateOf(0L)
    private var rawPlayerDurationMs by mutableLongStateOf(0L)
    private var prefetchedDurationMs by mutableLongStateOf(0L)

    private val effectiveDurationMs: Long
        get() = if (rawPlayerDurationMs > 0) rawPlayerDurationMs else prefetchedDurationMs

    private var canSkipNext by mutableStateOf(false)
    private var canSkipPrevious by mutableStateOf(false)
    private var playlists by mutableStateOf<List<PlaylistUi>>(emptyList())
    private var isGuest by mutableStateOf(true)
    private var queueItems by mutableStateOf<List<QueueItemUi>>(emptyList())
    private var playlistDialogTarget by mutableStateOf<StreamItem?>(null)
    private var lyricsResult by mutableStateOf<LyricsResult?>(null)
    private var isExtendingQueue = false

    private var isBuffering by mutableStateOf(false)
    private var pendingBufferingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observePlaylists()

        val sessionToken = SessionToken(this, ComponentName(this, HarmixPlaybackService::class.java))

        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync().also { future ->
            future.addListener(
                {
                    try {
                        mediaController = future.get()
                        attachPlayerListener()
                        startPositionTicker()
                    } catch (e: Exception) {
                        Log.e("Harmix", "Failed to connect MediaController", e)
                    }
                },
                MoreExecutors.directExecutor()
            )
        }

        setContent {
            HarmixTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        playTrack = { item -> playQueue(listOf(item), 0) },
                        onPlayQueue = ::playQueue,
                        onPlayNext = ::playNext,
                        onAddToQueue = ::addToQueue,
                        currentSongTitle = currentSongTitle,
                        currentArtist = currentArtist,
                        currentArtworkUrl = currentArtworkUrl,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        currentPositionMs = currentPositionMs,
                        durationMs = effectiveDurationMs,
                        canSkipNext = canSkipNext,
                        canSkipPrevious = canSkipPrevious,
                        queueItems = queueItems,
                        playlists = playlists,
                        isGuest = isGuest,
                        onSignIn = { isGuest = false },
                        onSignOut = { isGuest = true },
                        onPlayPauseClick = ::togglePlayPause,
                        onSkipNext = { mediaController?.seekToNext() },
                        onSkipPrevious = { mediaController?.seekToPrevious() },
                        onSeekTo = { positionMs -> mediaController?.seekTo(positionMs) },
                        onQueueItemClick = { index -> mediaController?.seekTo(index, 0L) },
                        onQueueItemRemove = ::removeQueueItem,
                        playlistDialogTarget = playlistDialogTarget,
                        onAddToPlaylistRequest = { item -> playlistDialogTarget = item },
                        onDismissPlaylistDialog = { playlistDialogTarget = null },
                        onSelectPlaylistForTarget = ::addTargetToPlaylist,
                        onCreatePlaylistForTarget = ::createPlaylistAndAddTarget,
                        currentTrackForPlaylist = currentStreamItemOrNull(),
                        lyricsResult = lyricsResult,
                        onLyricsClick = ::fetchLyricsForCurrentTrack
                    )
                }
            }
        }
    }

    private fun observePlaylists() {
        lifecycleScope.launch { libraryRepository.getPlaylists().collect { list -> playlists = list } }
    }

    private fun attachPlayerListener() {
        val controller = mediaController ?: return
        controller.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                currentSongTitle = "Playback error — see logs"
                clearBufferingImmediately()
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) clearBufferingImmediately()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_BUFFERING) scheduleBufferingIndicator()
                else clearBufferingImmediately()
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentSongTitle = mediaItem?.mediaMetadata?.title?.toString() ?: "Nothing playing"
                currentArtist = mediaItem?.mediaMetadata?.artist?.toString() ?: ""
                currentArtworkUrl = mediaItem?.mediaMetadata?.artworkUri?.toString()
                currentTrackUrl = mediaItem?.mediaId
                prefetchedDurationMs = mediaItem?.mediaMetadata?.extras?.getLong("harmix_duration_ms") ?: 0L
                rawPlayerDurationMs = 0L
                lyricsResult = null
                isExtendingQueue = false
                refreshQueueState()
                maybeExtendQueue()
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                refreshQueueState()
            }
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_METADATA_CHANGED, Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
                    val reportedDuration = player.duration
                    if (reportedDuration != C.TIME_UNSET && reportedDuration > 0) {
                        rawPlayerDurationMs = reportedDuration
                    }
                    canSkipNext = player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT)
                    canSkipPrevious = player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS)
                }
            }
        })
    }

    private fun scheduleBufferingIndicator() {
        if (pendingBufferingJob?.isActive == true) return
        pendingBufferingJob = lifecycleScope.launch {
            delay(300)
            isBuffering = true
        }
    }

    private fun clearBufferingImmediately() {
        pendingBufferingJob?.cancel()
        pendingBufferingJob = null
        isBuffering = false
    }

    private fun startPositionTicker() {
        lifecycleScope.launch {
            while (isActive) {
                currentPositionMs = mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L
                delay(500)
            }
        }
    }

    private fun refreshQueueState() {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex
        queueItems = (0 until controller.mediaItemCount).map { index ->
            val item = controller.getMediaItemAt(index)
            QueueItemUi(
                index = index,
                title = item.mediaMetadata.title?.toString() ?: "Unknown title",
                thumbnailUrl = item.mediaMetadata.artworkUri?.toString(),
                isCurrent = index == currentIndex
            )
        }
    }

    private fun maybeExtendQueue() {
        val controller = mediaController ?: return
        if (isExtendingQueue) return
        if ((controller.mediaItemCount - 1 - controller.currentMediaItemIndex) > 2) return
        val videoId = extractVideoId(currentTrackUrl ?: return) ?: return
        isExtendingQueue = true
        lifecycleScope.launch {
            try {
                val related = metadataRepository.getUpNext(videoId, limit = 10)
                val existingUrls = (0 until controller.mediaItemCount).map { controller.getMediaItemAt(it).mediaId }.toSet()
                related.filter { it.url !in existingUrls }.forEach { controller.addMediaItem(it.toMediaItem()) }
                refreshQueueState()
            } catch (e: Exception) { Log.e("Harmix", "Autoplay error", e) }
        }
    }

    private fun extractVideoId(url: String): String? = runCatching { Uri.parse(url).getQueryParameter("v") }.getOrNull()
    private fun currentStreamItemOrNull(): StreamItem? = currentTrackUrl?.let { StreamItem(title = currentSongTitle, url = it, thumbnailUrl = currentArtworkUrl, uploader = currentArtist) }
    private fun fetchLyricsForCurrentTrack() {
        lyricsResult = null
        lifecycleScope.launch { lyricsResult = metadataRepository.getLyrics(currentSongTitle, currentArtist, (effectiveDurationMs / 1000L).toInt()) }
    }
    private fun addTargetToPlaylist(playlistId: Long) {
        val item = playlistDialogTarget ?: return
        lifecycleScope.launch {
            libraryRepository.addSongToPlaylist(playlistId, item)
            playlistDialogTarget = null
        }
    }
    private fun createPlaylistAndAddTarget(name: String) {
        if (name.isBlank()) return
        val item = playlistDialogTarget ?: return
        lifecycleScope.launch {
            val newPlaylistId = libraryRepository.createPlaylist(name)
            libraryRepository.addSongToPlaylist(newPlaylistId, item)
            playlistDialogTarget = null
        }
    }

    private fun playQueue(items: List<StreamItem>, startIndex: Int) {
        val controller = mediaController ?: return
        if (items.isEmpty()) return
        val mediaItems = items.map { it.toMediaItem() }
        val safeIndex = startIndex.coerceIn(0, mediaItems.lastIndex)
        isBuffering = true
        controller.setMediaItems(mediaItems, safeIndex, 0L)
        controller.prepare()
        controller.play()
        val startItem = items[safeIndex]
        currentSongTitle = startItem.title
        currentArtist = startItem.uploader
        currentArtworkUrl = startItem.thumbnailUrl
        currentTrackUrl = startItem.url
        prefetchedDurationMs = (startItem.durationSeconds ?: 0) * 1000L
        rawPlayerDurationMs = 0L
    }

    private fun playNext(item: StreamItem) { mediaController?.let { isBuffering = true
        it.addMediaItem((it.currentMediaItemIndex + 1).coerceAtMost(it.mediaItemCount), item.toMediaItem()); refreshQueueState() } }
    private fun addToQueue(item: StreamItem) { mediaController?.let { it.addMediaItem(item.toMediaItem()); refreshQueueState() } }
    private fun removeQueueItem(index: Int) { mediaController?.let { it.removeMediaItem(index); refreshQueueState() } }
    private fun togglePlayPause() { mediaController?.let { if (it.isPlaying) it.pause() else it.play() } }
    override fun onDestroy() { controllerFuture?.let { MediaController.releaseFuture(it) }; super.onDestroy() }
}

private fun StreamItem.toMediaItem(): MediaItem {
    val extras = android.os.Bundle().apply { durationSeconds?.let { putLong("harmix_duration_ms", it.toLong() * 1000L) } }
    return MediaItem.Builder().setMediaId(url)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(url)).build())
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(uploader).setExtras(extras).apply { thumbnailUrl?.let { setArtworkUri(Uri.parse(it)) } }.build())
        .build()
}
