package com.boom.harmix.extractor

import com.chaquo.python.PyException
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class AudioStreamResult(
    val url: String,
    val durationSeconds: Int?
)

@Singleton
class YtDlpRepository @Inject constructor() {

    suspend fun getAudioStreamUrl(videoIdOrUrl: String): AudioStreamResult = withContext(Dispatchers.IO) {
        val python = Python.getInstance()
        val extractorModule = python.getModule("harmix_engine")

        try {
            val resultString = extractorModule.callAttr("get_audio_url", videoIdOrUrl).toString()
            
            // BULLETPROOF FALLBACK: If Python returns a raw URL instead of JSON, catch it and force playback anyway!
            if (resultString.startsWith("http")) {
                return@withContext AudioStreamResult(url = resultString, durationSeconds = null)
            }

            val json = JSONObject(resultString)
            val url = json.optString("url").ifBlank {
                throw NoSuchElementException("extractor returned empty URL")
            }
            val durationSeconds = if (json.isNull("durationSeconds")) null else json.optInt("durationSeconds")

            AudioStreamResult(url = url, durationSeconds = durationSeconds)
        } catch (e: Exception) {
            throw RuntimeException("yt-dlp failed: ${e.message}", e)
        }
    }
}
