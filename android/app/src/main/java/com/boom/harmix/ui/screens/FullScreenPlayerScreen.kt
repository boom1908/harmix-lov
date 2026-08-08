package com.boom.harmix.ui.screens
import androidx.compose.foundation.clickable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.boom.harmix.metadata.LyricsResult
import com.boom.harmix.playback.QueueItemUi
import com.boom.harmix.ui.theme.CoolGray
import com.boom.harmix.ui.theme.DeepMidnight
import com.boom.harmix.ui.theme.MistWhite
import com.boom.harmix.ui.theme.ZenCyan
import java.util.concurrent.TimeUnit

@Composable
fun FullScreenPlayerScreen(
    songTitle: String,
    artist: String,
    artworkUrl: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    canSkipNext: Boolean,
    canSkipPrevious: Boolean,
    isGuest: Boolean,
    queueItems: List<QueueItemUi>,
    lyricsResult: LyricsResult?,
    onPlayPauseClick: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onAddCurrentTrackToPlaylistRequest: () -> Unit,
    onQueueItemClick: (index: Int) -> Unit,
    onQueueItemRemove: (index: Int) -> Unit,
    onLyricsClick: () -> Unit,
    onCollapse: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepMidnight)
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse player", tint = MistWhite)
                }

                Row {
                    IconButton(onClick = { showLyricsSheet = true; onLyricsClick() }) {
                        Icon(imageVector = Icons.Filled.Lyrics, contentDescription = "Lyrics", tint = ZenCyan)
                    }
                    if (!isGuest) {
                        IconButton(onClick = onAddCurrentTrackToPlaylistRequest) {
                            Icon(imageVector = Icons.Filled.PlaylistAdd, contentDescription = "Add to Playlist", tint = ZenCyan)
                        }
                    }
                    IconButton(onClick = { showQueueSheet = true }) {
                        Icon(imageVector = Icons.Filled.QueueMusic, contentDescription = "Queue", tint = ZenCyan)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(vertical = 32.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                AsyncImage(model = artworkUrl, contentDescription = songTitle, modifier = Modifier.fillMaxSize())
            }

            Text(text = songTitle, color = MistWhite, style = MaterialTheme.typography.headlineSmall, maxLines = 2)
            Text(
                text = artist,
                color = CoolGray,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            val sliderPosition = if (isDragging) dragPositionMs else currentPositionMs.toFloat()
            val sliderMax = durationMs.coerceAtLeast(1L).toFloat()

            Slider(
                value = sliderPosition.coerceIn(0f, sliderMax),
                onValueChange = { isDragging = true; dragPositionMs = it },
                onValueChangeFinished = { onSeekTo(dragPositionMs.toLong()); isDragging = false },
                valueRange = 0f..sliderMax,
                colors = SliderDefaults.colors(
                    thumbColor = ZenCyan,
                    activeTrackColor = ZenCyan,
                    inactiveTrackColor = CoolGray.copy(alpha = 0.3f)
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = formatMillis(sliderPosition.toLong()), color = CoolGray, style = MaterialTheme.typography.labelSmall)
                Text(text = formatMillis(durationMs), color = CoolGray, style = MaterialTheme.typography.labelSmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSkipPrevious, enabled = canSkipPrevious) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = if (canSkipPrevious) MistWhite else CoolGray.copy(alpha = 0.4f),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if (isBuffering && !isPlaying) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ZenCyan)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = DeepMidnight,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ZenCyan)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = DeepMidnight
                        )
                    }
                }

                IconButton(onClick = onSkipNext, enabled = canSkipNext) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = if (canSkipNext) MistWhite else CoolGray.copy(alpha = 0.4f),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            queueItems = queueItems,
            onDismiss = { showQueueSheet = false },
            onItemClick = onQueueItemClick,
            onRemoveItem = onQueueItemRemove
        )
    }

    if (showLyricsSheet) {
        LyricsBottomSheet(
            lyricsResult = lyricsResult,
            currentPositionMs = currentPositionMs,
            onDismiss = { showLyricsSheet = false }
        )
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis.coerceAtLeast(0L))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
