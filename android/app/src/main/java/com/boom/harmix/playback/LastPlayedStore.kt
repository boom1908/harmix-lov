package com.boom.harmix.playback

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class LastPlayedTrack(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val url: String
)

/**
 * Remembers whatever was playing last so a cold start can paint the mini player
 * immediately instead of flashing "Nothing playing" while the session reconnects.
 */
@Singleton
class LastPlayedStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("harmix_last_played", Context.MODE_PRIVATE)

    fun save(track: LastPlayedTrack) {
        prefs.edit()
            .putString("title", track.title)
            .putString("artist", track.artist)
            .putString("artwork", track.artworkUrl)
            .putString("url", track.url)
            .apply()
    }

    fun read(): LastPlayedTrack? {
        val title = prefs.getString("title", null) ?: return null
        val url = prefs.getString("url", null) ?: return null
        return LastPlayedTrack(
            title = title,
            artist = prefs.getString("artist", "").orEmpty(),
            artworkUrl = prefs.getString("artwork", null),
            url = url
        )
    }
}
