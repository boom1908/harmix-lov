package com.boom.harmix.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boom.harmix.data.local.entity.SavedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SavedSongEntity)

    @Delete
    suspend fun deleteSong(song: SavedSongEntity)

    @Query("SELECT * FROM saved_songs ORDER BY savedAtMillis DESC")
    fun getAllSongs(): Flow<List<SavedSongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_songs WHERE url = :url)")
    suspend fun isSongSaved(url: String): Boolean

    @Query("DELETE FROM saved_songs WHERE url = :url")
    suspend fun deleteSongByUrl(url: String)
}
