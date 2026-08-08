package com.boom.harmix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.boom.harmix.metadata.LyricLine
import com.boom.harmix.metadata.LyricsResult
import com.boom.harmix.ui.theme.CoolGray
import com.boom.harmix.ui.theme.DeepMidnight
import com.boom.harmix.ui.theme.MistWhite
import com.boom.harmix.ui.theme.ZenCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsBottomSheet(
    lyricsResult: LyricsResult?,
    currentPositionMs: Long,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepMidnight
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(500.dp).padding(horizontal = 20.dp)) {
            when (lyricsResult) {
                null -> Text(text = "Loading lyrics...", color = CoolGray)
                is LyricsResult.NotFound -> Text(text = "No lyrics found for this track.", color = CoolGray)
                is LyricsResult.PlainOnly -> {
                    LazyColumn {
                        item {
                            Text(
                                text = lyricsResult.text,
                                color = MistWhite,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
                is LyricsResult.Synced -> SyncedLyricsList(lyricsResult.lines, currentPositionMs)
            }
        }
    }
}

@Composable
private fun SyncedLyricsList(lines: List<LyricLine>, currentPositionMs: Long) {
    val listState = rememberLazyListState()

    // Active line = the last line whose timestamp has already passed.
    val activeIndex = lines.indexOfLast { it.timestampMs <= currentPositionMs }.coerceAtLeast(0)

    LaunchedEffect(activeIndex) {
        // Center the active line roughly mid-sheet
        listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
    ) {
        items(lines.size) { index ->
            val line = lines[index]
            val isActive = index == activeIndex

            Text(
                text = line.text,
                color = if (isActive) ZenCyan else CoolGray,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
