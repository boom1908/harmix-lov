package com.boom.harmix.data.local

import com.boom.harmix.data.local.dao.PlaylistDao
import com.boom.harmix.data.local.dao.SavedSongDao
import com.boom.harmix.data.local.entity.PlaylistEntity
import com.boom.harmix.data.local.entity.PlaylistSongCrossRef
import com.boom.harmix.data.local.entity.SavedSongEntity
import com.boom.harmix.extractor.StreamItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class PlaylistUi(
    val id: Long,
    val name: String,
    val songs: List<StreamItem>
)

@Singleton
class LibraryRepository @Inject constructor(
    private val savedSongDao: SavedSongDao,
    private val playlistDao: PlaylistDao
) {

    fun getSavedSongs(): Flow<List<StreamItem>> =
        savedSongDao.getAllSongs().map { list -> list.map { it.toStreamItem() } }

    suspend fun saveSong(item: StreamItem) {
        savedSongDao.insertSong(item.toEntity())
    }

    suspend fun removeSong(item: StreamItem) {
        savedSongDao.deleteSongByUrl(item.url)
    }

    suspend fun isSongSaved(url: String): Boolean = savedSongDao.isSongSaved(url)

    fun getPlaylists(): Flow<List<PlaylistUi>> =
        playlistDao.getPlaylistsWithSongs().map { list ->
            list.map { withSongs ->
                PlaylistUi(
                    id = withSongs.playlist.playlistId,
                    name = withSongs.playlist.name,
                    songs = withSongs.songs.map { it.toStreamItem() }
                )
            }
        }

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name))

    suspend fun addSongToPlaylist(playlistId: Long, item: StreamItem) {
        savedSongDao.insertSong(item.toEntity())
        val nextPosition = playlistDao.getNextPosition(playlistId)
        playlistDao.addSongToPlaylist(
            PlaylistSongCrossRef(playlistId = playlistId, songUrl = item.url, position = nextPosition)
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songUrl: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songUrl)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }
}

private fun SavedSongEntity.toStreamItem() = StreamItem(
    title = title,
    url = url,
    thumbnailUrl = thumbnailUrl,
    uploader = uploader
)

private fun StreamItem.toEntity() = SavedSongEntity(
    url = url,
    title = title,
    thumbnailUrl = thumbnailUrl,
    uploader = uploader
)
