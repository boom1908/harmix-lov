package com.boom.harmix.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.boom.harmix.extractor.StreamItem
import com.boom.harmix.ui.theme.CoolGray
import com.boom.harmix.ui.theme.GlassBorder
import com.boom.harmix.ui.theme.MistWhite
import com.boom.harmix.ui.theme.ZenCyan
import com.boom.harmix.ui.viewmodel.SearchUiState
import com.boom.harmix.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    isGuest: Boolean,
    onItemClick: (StreamItem) -> Unit,
    onPlayNext: (StreamItem) -> Unit,
    onAddToQueue: (StreamItem) -> Unit,
    onAddToPlaylistRequest: (StreamItem) -> Unit
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var optionsSheetTarget by remember { mutableStateOf<StreamItem?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChanged,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
            placeholder = { Text("Search any song...", color = CoolGray) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { viewModel.runSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ZenCyan,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = MistWhite,
                unfocusedTextColor = MistWhite,
                cursorColor = ZenCyan
            )
        )

        Column(modifier = Modifier.padding(top = 20.dp)) {
            when (val state = uiState) {
                is SearchUiState.Idle -> Text(text = "Search results will show up here.", color = CoolGray)
                is SearchUiState.Loading -> Text(text = "Searching...", color = CoolGray)
                is SearchUiState.Error -> Text(text = "Search failed: ${state.message}", color = CoolGray)
                is SearchUiState.Success -> {
                    LazyColumn {
                        items(state.items) { item ->
                            SearchResultRow(
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
private fun SearchResultRow(item: StreamItem, onClick: () -> Unit, onMoreClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = item.title,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
        )
        Text(
            text = item.title,
            color = MistWhite,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            modifier = Modifier.padding(start = 12.dp).weight(1f)
        )
        IconButton(onClick = onMoreClick) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options", tint = CoolGray)
        }
    }
}
