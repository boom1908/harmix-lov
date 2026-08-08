package com.boom.harmix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.boom.harmix.playback.QueueItemUi
import com.boom.harmix.ui.theme.CoolGray
import com.boom.harmix.ui.theme.DeepMidnight
import com.boom.harmix.ui.theme.GlassFill
import com.boom.harmix.ui.theme.MistWhite
import com.boom.harmix.ui.theme.ZenCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queueItems: List<QueueItemUi>,
    onDismiss: () -> Unit,
    onItemClick: (index: Int) -> Unit,
    onRemoveItem: (index: Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepMidnight
    ) {
        Text(
            text = "Up Next",
            color = MistWhite,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        if (queueItems.isEmpty()) {
            Text(
                text = "Queue is empty.",
                color = CoolGray,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.height(420.dp)) {
                items(queueItems, key = { it.index }) { queueItem ->
                    QueueRow(
                        item = queueItem,
                        onClick = { onItemClick(queueItem.index) },
                        onRemove = { onRemoveItem(queueItem.index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    item: QueueItemUi,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (item.isCurrent) GlassFill else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.thumbnailUrl != null) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = CoolGray,
                modifier = Modifier.size(48.dp)
            )
        }

        Row(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = item.title,
                color = if (item.isCurrent) ZenCyan else MistWhite,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }

        IconButton(onClick = onRemove) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Remove from queue", tint = CoolGray)
        }
    }
}
