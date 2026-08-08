package com.boom.harmix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.boom.harmix.extractor.StreamItem
import com.boom.harmix.ui.theme.CoolGray
import com.boom.harmix.ui.theme.MistWhite
import com.boom.harmix.ui.viewmodel.HomeUiState
import com.boom.harmix.ui.viewmodel.HomeViewModel
import java.util.Calendar

private val ErrorRed = Color(0xFFFF6B6B)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    isGuest: Boolean,
    onItemClick: (StreamItem) -> Unit,
    onPlayNext: (StreamItem) -> Unit,
    onAddToQueue: (StreamItem) -> Unit,
    onAddToPlaylistRequest: (StreamItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var optionsSheetTarget by remember { mutableStateOf<StreamItem?>(null) }

    LaunchedEffect(Unit) { viewModel.loadRecommendations() }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = greetingForCurrentTime(), color = MistWhite, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Here's what's trending right now",
            color = CoolGray,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        when (val state = uiState) {
            is HomeUiState.Loading -> Text(text = "Loading recommendations...", color = CoolGray)
            is HomeUiState.Error -> HomeStatusBanner(title = "Trending feed failed to load", detail = state.message)
            is HomeUiState.Success -> {
                if (state.items.isEmpty()) {
                    HomeStatusBanner(title = "Trending feed returned no tracks", detail = "Try again shortly.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(state.items) { item ->
                            TrendingRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onMoreClick = { optionsSheetTarget = item }
                            )
                        }
                    }
                }
            }
        }
    }

    optionsSheetTarget?.let { target ->
        SongOptionsBottomSheet(
            item = target,
            isGuest = isGuest,
            onDismiss = { optionsSheetTarget = null },
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onAddToPlaylistRequest = onAddToPlaylistRequest
        )
    }
}

@Composable
private fun HomeStatusBanner(title: String, detail: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ErrorRed.copy(alpha = 0.12f))
            .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(text = title, color = ErrorRed, style = MaterialTheme.typography.titleSmall)
        Text(text = detail, color = MistWhite, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun TrendingRow(
    item: StreamItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = item.title,
                color = MistWhite,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                text = item.uploader,
                color = CoolGray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        IconButton(onClick = onMoreClick) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options", tint = CoolGray)
        }
    }
}

private fun greetingForCurrentTime(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}
