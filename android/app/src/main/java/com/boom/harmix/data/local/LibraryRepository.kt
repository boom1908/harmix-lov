package com.boom.harmix.data.local

import com.boom.harmix.data.local.dao.PlaylistDao
import com.boom.harmix.data.local.dao.SavedSongDao
import com.boom.harmix.data.local.entity.PlaylistEntity
import com.boom.harmix.data.local.entity.PlaylistSongCrossRef
import com.boom.harmix.data.local.entity.SavedSongEntity
import com.boom.harmix.extractor.StreamItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    fun getLikedSongs(): Flow<List<StreamItem>> =
        savedSongDao.getLikedSongs().map { list -> list.map { it.toStreamItem() } }

    fun getLikedUrls(): Flow<Set<String>> = savedSongDao.getLikedUrls().map { it.toSet() }

    suspend fun saveSong(item: StreamItem) {
        savedSongDao.insertSong(item.toEntity(liked = true))
    }

    suspend fun removeSong(item: StreamItem) {
        savedSongDao.deleteSongByUrl(item.url)
    }

    /** Heart toggle used by the player and track rows. */
    suspend fun toggleLike(item: StreamItem): Boolean {
        val alreadySaved = savedSongDao.isSongSaved(item.url)
        return if (!alreadySaved) {
            savedSongDao.insertSong(item.toEntity(liked = true))
            true
        } else {
            val liked = savedSongDao.isLiked(item.url)
            savedSongDao.setLiked(item.url, !liked)
            !liked
        }
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

    /** Single playlist with its songs in saved order. */
    fun getPlaylist(playlistId: Long): Flow<PlaylistUi?> =
        combine(
            playlistDao.getPlaylist(playlistId),
            playlistDao.getPlaylistSongs(playlistId)
        ) { playlist, songs ->
            playlist?.let {
                PlaylistUi(it.playlistId, it.name, songs.map { song -> song.toStreamItem() })
            }
        }

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name))

    /**
     * Merge target for a synced YouTube playlist: reuses the local playlist that was
     * created from the same remote id (or adopts a same-named local one) so re-syncing
     * never duplicates playlists.
     */
    suspend fun getOrCreateRemotePlaylist(remoteId: String, name: String): Long {
        playlistDao.findPlaylistByRemoteId(remoteId)?.let { existing ->
            if (existing.name != name) playlistDao.renamePlaylist(existing.playlistId, name)
            return existing.playlistId
        }
        playlistDao.findLocalPlaylistByName(name)?.let { adopted ->
            playlistDao.setRemoteId(adopted.playlistId, remoteId)
            return adopted.playlistId
        }
        return playlistDao.insertPlaylist(PlaylistEntity(name = name, remoteId = remoteId))
    }

    /** True when the song was newly added (false when it was already in the playlist). */
    suspend fun addSongToPlaylistIfAbsent(playlistId: Long, item: StreamItem): Boolean {
        if (playlistDao.isSongInPlaylist(playlistId, item.url)) return false
        addSongToPlaylist(playlistId, item)
        return true
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) {
        if (name.isNotBlank()) playlistDao.renamePlaylist(playlistId, name)
    }

    suspend fun addSongToPlaylist(playlistId: Long, item: StreamItem) {
        savedSongDao.insertSongIfAbsent(item.toEntity())
        if (playlistDao.isSongInPlaylist(playlistId, item.url)) return
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

private fun StreamItem.toEntity(liked: Boolean = false) = SavedSongEntity(
    url = url,
    title = title,
    thumbnailUrl = thumbnailUrl,
    uploader = uploader,
    liked = liked
)
