package com.boom.harmix.data.cloud

import com.boom.harmix.data.local.PlaylistUi
import com.boom.harmix.extractor.StreamItem
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreLibraryRepository @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()

    fun getSavedSongs(uid: String): Flow<List<StreamItem>> =
        combine(getLikedSongs(uid), getPlaylists(uid)) { liked, playlists ->
            (liked + playlists.flatMap { it.songs }).distinctBy { it.url }
        }

    fun getLikedSongs(uid: String): Flow<List<StreamItem>> =
        likedSongsQuery(uid).asStream { snapshot ->
            snapshot.documents.mapNotNull { document ->
                document.toStreamItem()
            }
        }

    fun getLikedUrls(uid: String): Flow<Set<String>> =
        getLikedSongs(uid).map { songs -> songs.mapTo(mutableSetOf()) { it.url } }

    fun getPlaylists(uid: String): Flow<List<PlaylistUi>> =
        playlistsQuery(uid).asStream { snapshot ->
            snapshot.documents.mapNotNull { it.toPlaylistUi() }
        }

    fun getPlaylist(uid: String, playlistId: Long): Flow<PlaylistUi?> =
        getPlaylists(uid).map { playlists ->
            playlists.firstOrNull { it.id == playlistId }
        }

    suspend fun saveSong(uid: String, item: StreamItem) {
        Tasks.await(likedSongRef(uid, item.url).set(item.toLikedSongData()))
    }

    suspend fun removeSong(uid: String, item: StreamItem) {
        Tasks.await(likedSongRef(uid, item.url).delete())
    }

    suspend fun toggleLike(uid: String, item: StreamItem): Boolean {
        val reference = likedSongRef(uid, item.url)
        val snapshot = Tasks.await(reference.get())
        return if (snapshot.exists()) {
            Tasks.await(reference.delete())
            false
        } else {
            Tasks.await(reference.set(item.toLikedSongData()))
            true
        }
    }

    suspend fun isSongSaved(uid: String, url: String): Boolean =
        Tasks.await(likedSongRef(uid, url).get()).exists()

    suspend fun createPlaylist(uid: String, name: String): Long {
        var playlistId = System.currentTimeMillis()
        val collection = playlistsRef(uid)
        while (Tasks.await(collection.document(playlistId.toString()).get()).exists()) {
            playlistId++
        }
        Tasks.await(
            collection.document(playlistId.toString()).set(
                mapOf(
                    "name" to name,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "songs" to emptyList<Map<String, Any?>>()
                )
            )
        )
        return playlistId
    }

    suspend fun getOrCreateRemotePlaylist(uid: String, remoteId: String, name: String): Long {
        val collection = playlistsRef(uid)
        val matches = Tasks.await(
            collection.whereEqualTo("remoteId", remoteId).get()
        ).documents

        if (matches.isNotEmpty()) {
            val canonical = matches.first()
            val canonicalId = canonical.id.toLongOrNull()
                ?: createPlaylist(uid, name)
            val duplicateDocuments = matches.drop(1)
            if (canonical.id.toLongOrNull() != null) {
                val mergedSongs = (
                    listOf(canonical.toPlaylistUi()?.songs.orEmpty()) +
                        duplicateDocuments.map { it.toPlaylistUi()?.songs.orEmpty() }
                    ).flatten().distinctBy { it.url }
                if (canonical.getString("name") != name || duplicateDocuments.isNotEmpty()) {
                    Tasks.await(
                        firestore.runTransaction { transaction ->
                            transaction.set(
                                collection.document(canonical.id),
                                mapOf(
                                    "name" to name,
                                    "remoteId" to remoteId,
                                    "songs" to mergedSongs.map { it.toSongData() }
                                ),
                                SetOptions.merge()
                            )
                            duplicateDocuments.forEach { transaction.delete(it.reference) }
                            true
                        }
                    )
                }
                return canonicalId
            }
        }

        // Adopt a same-named cloud playlist that has not been linked to YouTube yet.
        val legacyName = name.removeSuffix(" (YouTube)")
        val namedMatches = Tasks.await(collection.whereEqualTo("name", name).get()).documents +
            Tasks.await(collection.whereEqualTo("name", legacyName).get()).documents
        namedMatches.firstOrNull { it.getString("remoteId").isNullOrBlank() }?.let { adopted ->
            Tasks.await(
                adopted.reference.set(
                    mapOf("name" to name, "remoteId" to remoteId),
                    SetOptions.merge()
                )
            )
            return adopted.id.toLongOrNull() ?: createPlaylist(uid, name)
        }

        val playlistId = createPlaylist(uid, name)
        Tasks.await(
            collection.document(playlistId.toString()).update("remoteId", remoteId)
        )
        return playlistId
    }

    suspend fun addSongToPlaylistIfAbsent(uid: String, playlistId: Long, item: StreamItem): Boolean =
        updatePlaylistSongs(uid, playlistId) { songs ->
            if (songs.any { it.url == item.url }) {
                null
            } else {
                songs + item
            }
        }

    suspend fun addSongToPlaylist(uid: String, playlistId: Long, item: StreamItem) {
        updatePlaylistSongs(uid, playlistId) { songs ->
            if (songs.any { it.url == item.url }) null else songs + item
        }
    }

    suspend fun removeSongFromPlaylist(uid: String, playlistId: Long, songUrl: String) {
        updatePlaylistSongs(uid, playlistId) { songs ->
            songs.filterNot { it.url == songUrl }
        }
    }

    suspend fun renamePlaylist(uid: String, playlistId: Long, name: String) {
        if (name.isNotBlank()) {
            Tasks.await(playlistRef(uid, playlistId).update("name", name))
        }
    }

    suspend fun deletePlaylist(uid: String, playlistId: Long) {
        Tasks.await(playlistRef(uid, playlistId).delete())
    }

    private suspend fun updatePlaylistSongs(
        uid: String,
        playlistId: Long,
        transform: (List<StreamItem>) -> List<StreamItem>?
    ): Boolean {
        val reference = playlistRef(uid, playlistId)
        return Tasks.await(
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(reference)
                if (!snapshot.exists()) return@runTransaction false
                val currentSongs = snapshot.toPlaylistUi()?.songs.orEmpty()
                val updatedSongs = transform(currentSongs) ?: return@runTransaction false
                transaction.update(reference, "songs", updatedSongs.map { it.toSongData() })
                true
            }
        )
    }

    private fun playlistsRef(uid: String) =
        firestore.collection("users").document(uid).collection("playlists")

    private fun playlistRef(uid: String, playlistId: Long) =
        playlistsRef(uid).document(playlistId.toString())

    private fun likedSongsQuery(uid: String): Query =
        firestore.collection("users").document(uid).collection("likedSongs")
            .orderBy("likedAt", Query.Direction.DESCENDING)

    private fun likedSongRef(uid: String, url: String) =
        firestore.collection("users").document(uid)
            .collection("likedSongs").document(url.sha256())

    private fun playlistsQuery(uid: String): Query =
        playlistsRef(uid).orderBy("createdAt", Query.Direction.DESCENDING)

    private fun StreamItem.toLikedSongData(): Map<String, Any?> = mapOf(
        "songId" to url,
        "title" to title,
        "artist" to uploader,
        "thumbnailUrl" to thumbnailUrl,
        "likedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )

    private fun StreamItem.toSongData(): Map<String, Any?> = mapOf(
        "songId" to url,
        "title" to title,
        "artist" to uploader,
        "thumbnailUrl" to thumbnailUrl
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toStreamItem(): StreamItem? {
        val url = getString("songId") ?: getString("url") ?: return null
        return StreamItem(
            title = getString("title").orEmpty().ifBlank { "Unknown title" },
            url = url,
            thumbnailUrl = getString("thumbnailUrl"),
            uploader = getString("artist").orEmpty()
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toPlaylistUi(): PlaylistUi? {
        val id = id.toLongOrNull() ?: return null
        val songs = (get("songs") as? List<*>).orEmpty().mapNotNull { value ->
            val song = value as? Map<*, *> ?: return@mapNotNull null
            val url = (song["songId"] ?: song["url"]) as? String ?: return@mapNotNull null
            StreamItem(
                title = (song["title"] as? String).orEmpty().ifBlank { "Unknown title" },
                url = url,
                thumbnailUrl = song["thumbnailUrl"] as? String,
                uploader = (song["artist"] as? String).orEmpty()
            )
        }
        return PlaylistUi(id = id, name = getString("name").orEmpty(), songs = songs)
    }

    private fun <T> Query.asStream(mapper: (com.google.firebase.firestore.QuerySnapshot) -> T): Flow<T> =
        callbackFlow {
            var registration: ListenerRegistration? = null
            registration = addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else if (snapshot != null) {
                    trySend(mapper(snapshot))
                }
            }
            awaitClose { registration?.remove() }
        }

    private fun String.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
}