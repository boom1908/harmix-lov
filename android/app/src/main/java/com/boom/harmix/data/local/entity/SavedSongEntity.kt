package com.boom.harmix.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_songs")
data class SavedSongEntity(
    @PrimaryKey val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val uploader: String,
    val savedAtMillis: Long = System.currentTimeMillis()
)
