package com.boom.harmix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.boom.harmix.data.local.PlaylistUi
import com.boom.harmix.extractor.StreamItem
import com.boom.harmix.metadata.LyricsResult
import com.boom.harmix.navigation.HarmixNavHost
import com.boom.harmix.navigation.bottomNavItemsFor
import com.boom.harmix.playback.QueueItemUi
import com.boom.harmix.ui.theme.CoolGray
import com.boom.harmix.ui.theme.GlassBorder
import com.boom.harmix.ui.theme.GlassFill
import com.boom.harmix.ui.theme.MistWhite
import com.boom.harmix.ui.theme.ZenCyan

@Composable
fun MainScreen(
    playTrack: (StreamItem) -> Unit,
    onPlayQueue: (List<StreamItem>, Int) -> Unit,
    onPlayNext: (StreamItem) -> Unit,
    onAddToQueue: (StreamItem) -> Unit,
    currentSongTitle: String,
    currentArtist: String,
    currentArtworkUrl: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    canSkipNext: Boolean,
    canSkipPrevious: Boolean,
    queueItems: List<QueueItemUi>,
    playlists: List<PlaylistUi>,
    isGuest: Boolean,
    lyricsResult: LyricsResult?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onQueueItemClick: (index: Int) -> Unit,
    onQueueItemRemove: (index: Int) -> Unit,
    onLyricsClick: () -> Unit,
    playlistDialogTarget: StreamItem?,
    currentTrackForPlaylist: StreamItem?,
    onAddToPlaylistRequest: (StreamItem) -> Unit,
    onDismissPlaylistDialog: () -> Unit,
    onSelectPlaylistForTarget: (playlistId: Long) -> Unit,
    onCreatePlaylistForTarget: (name: String) -> Unit
) {
    val navController = rememberNavController()
    var isFullPlayerExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = isFullPlayerExpanded) {
        isFullPlayerExpanded = false
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                Column {
                    MiniPlayer(
                        songTitle = currentSongTitle,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        onPlayPauseClick = onPlayPauseClick,
                        onExpandClick = { isFullPlayerExpanded = true }
                    )
                    HarmixBottomBar(navController = navController, isGuest = isGuest)
                }
            }
        ) { innerPadding ->
            HarmixNavHost(
                navController = navController,
                playTrack = playTrack,
                onPlayQueue = onPlayQueue,
                isGuest = isGuest,
                onSignIn = onSignIn,
                onSignOut = onSignOut,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylistRequest = onAddToPlaylistRequest,
                modifier = Modifier.padding(innerPadding)
            )
        }

        AnimatedVisibility(
            visible = isFullPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }),
            exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight })
        ) {
            FullScreenPlayerScreen(
                songTitle = currentSongTitle,
                artist = currentArtist,
                artworkUrl = currentArtworkUrl,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                canSkipNext = canSkipNext,
                canSkipPrevious = canSkipPrevious,
                isGuest = isGuest,
                queueItems = queueItems,
                lyricsResult = lyricsResult,
                onPlayPauseClick = onPlayPauseClick,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onSeekTo = onSeekTo,
                onAddCurrentTrackToPlaylistRequest = {
                    currentTrackForPlaylist?.let { onAddToPlaylistRequest(it) }
                },
                onQueueItemClick = onQueueItemClick,
                onQueueItemRemove = onQueueItemRemove,
                onLyricsClick = onLyricsClick,
                onCollapse = { isFullPlayerExpanded = false }
            )
        }
    }

    if (playlistDialogTarget != null) {
        PlaylistSelectionDialog(
            playlists = playlists,
            onDismiss = onDismissPlaylistDialog,
            onSelectPlaylist = onSelectPlaylistForTarget,
            onCreateAndSelect = onCreatePlaylistForTarget
        )
    }
}

@Composable
private fun MiniPlayer(
    songTitle: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPauseClick: () -> Unit,
    onExpandClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onExpandClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = songTitle,
            color = if (songTitle == "Nothing playing") CoolGray else MistWhite,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        // Only show buffering spinner if we are NOT actively playing audio
        if (isBuffering && !isPlaying) {
            CircularProgressIndicator(
                color = ZenCyan,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = ZenCyan
                )
            }
        }
    }
}

@Composable
private fun HarmixBottomBar(navController: androidx.navigation.NavHostController, isGuest: Boolean) {
    val items = bottomNavItemsFor(isGuest)

    NavigationBar(
        containerColor = GlassFill,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { 
                    Text(
                        text = destination.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ZenCyan,
                    selectedTextColor = ZenCyan,
                    unselectedIconColor = CoolGray,
                    unselectedTextColor = CoolGray,
                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    }
}
