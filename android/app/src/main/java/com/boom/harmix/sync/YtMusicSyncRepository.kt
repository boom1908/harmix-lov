package com.boom.harmix.sync

import com.boom.harmix.auth.GoogleAccountsRepository
import com.boom.harmix.core.NetworkMonitor
import com.boom.harmix.data.local.LibraryRepository
import com.boom.harmix.extractor.StreamItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class SyncSummary(val playlists: Int, val songs: Int)

/**
 * Pulls the playlists of the *YouTube sync* Google account (never the main
 * Harmix account) and stores them in the local library.
 */
@Singleton
class YtMusicSyncRepository @Inject constructor(
    private val accounts: GoogleAccountsRepository,
    private val libraryRepository: LibraryRepository,
    private val networkMonitor: NetworkMonitor
) {

    private val http = OkHttpClient()

    suspend fun syncPlaylists(): SyncSummary = withContext(Dispatchers.IO) {
        networkMonitor.requireOnline()
        val token = accounts.youtubeAccessToken()
            ?: throw IllegalStateException("Connect a Google account for YouTube Music sync first.")

        var playlistCount = 0
        var songCount = 0

        var pageToken: String? = null
        do {
            val url = buildString {
                append("https://www.googleapis.com/youtube/v3/playlists")
                append("?part=snippet&mine=true&maxResults=50")
                if (pageToken != null) append("&pageToken=$pageToken")
            }
            val json = getJson(url, token)
            val items = json.optJSONArray("items") ?: break

            for (i in 0 until items.length()) {
                val playlist = items.optJSONObject(i) ?: continue
                val playlistId = playlist.optString("id").ifBlank { continue }
                val name = playlist.optJSONObject("snippet")?.optString("title").orEmpty()
                    .ifBlank { "YouTube playlist" }

                val localId = libraryRepository.createPlaylist("$name (YouTube)")
                playlistCount++
                songCount += importPlaylistItems(playlistId, localId, token)
            }
            pageToken = json.optString("nextPageToken").ifBlank { null }
        } while (pageToken != null)

        SyncSummary(playlistCount, songCount)
    }

    private suspend fun importPlaylistItems(
        remotePlaylistId: String,
        localPlaylistId: Long,
        token: String
    ): Int {
        var added = 0
        var pageToken: String? = null
        do {
            val url = buildString {
                append("https://www.googleapis.com/youtube/v3/playlistItems")
                append("?part=snippet&maxResults=50&playlistId=$remotePlaylistId")
                if (pageToken != null) append("&pageToken=$pageToken")
            }
            val json = getJson(url, token)
            val items = json.optJSONArray("items") ?: break

            for (i in 0 until items.length()) {
                val snippet = items.optJSONObject(i)?.optJSONObject("snippet") ?: continue
                val videoId = snippet.optJSONObject("resourceId")?.optString("videoId").orEmpty()
                if (videoId.isBlank()) continue
                val title = snippet.optString("title", "Unknown title")
                if (title == "Private video" || title == "Deleted video") continue

                val thumb = snippet.optJSONObject("thumbnails")
                    ?.let { it.optJSONObject("high") ?: it.optJSONObject("default") }
                    ?.optString("url")

                libraryRepository.addSongToPlaylist(
                    localPlaylistId,
                    StreamItem(
                        title = title,
                        url = "https://www.youtube.com/watch?v=$videoId",
                        thumbnailUrl = thumb?.ifBlank { null },
                        uploader = snippet.optString("videoOwnerChannelTitle", "")
                    )
                )
                added++
            }
            pageToken = json.optString("nextPageToken").ifBlank { null }
        } while (pageToken != null)
        return added
    }

    private fun getJson(url: String, token: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()

        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("YouTube sync failed (${response.code}). $body")
            }
            return JSONObject(body)
        }
    }
}