import json
import threading
import time

import yt_dlp

# ---------------------------------------------------------------------------
# Stream URL cache
#
# yt-dlp extraction is the slowest part of playback (cold start can take
# several seconds). Google's stream URLs stay valid for ~6 hours, so we keep
# them in memory and re-use them. This is what removes the long buffering
# stall when jumping between songs in a playlist.
# ---------------------------------------------------------------------------
_CACHE = {}
_CACHE_LOCK = threading.Lock()
_CACHE_TTL_SECONDS = 4 * 60 * 60
_CACHE_MAX_ENTRIES = 200

_BASE_OPTS = {
    "quiet": True,
    "no_warnings": True,
    "noplaylist": True,
    "skip_download": True,
    "socket_timeout": 15,
    "retries": 2,
    "format": "bestaudio[ext=m4a]/bestaudio/best",
}

# Fast path: the android_music client returns audio-only formats without
# needing the (slow) web page download or JS player signature work.
_FAST_EXTRACTOR_ARGS = {
    "youtube": {
        "player_client": ["android_music", "android"],
        "player_skip": ["configs", "webpage"],
        "skip": ["hls", "dash", "translated_subs"],
    }
}

# Fallback: default clients, used only when the fast path fails.
_SAFE_EXTRACTOR_ARGS = {
    "youtube": {
        "player_client": ["web", "android"],
    }
}


def _watch_url(video_id):
    return video_id if video_id.startswith("http") else f"https://www.youtube.com/watch?v={video_id}"


def _cache_get(key):
    with _CACHE_LOCK:
        entry = _CACHE.get(key)
        if not entry:
            return None
        if entry["expiresAt"] <= time.time():
            _CACHE.pop(key, None)
            return None
        return entry["payload"]


def _cache_put(key, payload):
    with _CACHE_LOCK:
        if len(_CACHE) >= _CACHE_MAX_ENTRIES:
            oldest = min(_CACHE, key=lambda k: _CACHE[k]["expiresAt"])
            _CACHE.pop(oldest, None)
        _CACHE[key] = {"payload": payload, "expiresAt": time.time() + _CACHE_TTL_SECONDS}


def _pick_url(info):
    direct_url = info.get("url")
    if direct_url:
        return direct_url

    candidates = info.get("requested_formats") or info.get("formats") or []
    audio_only = [
        f for f in candidates
        if f.get("acodec") not in (None, "none") and f.get("vcodec") in (None, "none")
    ]
    pool = audio_only or candidates
    if not pool:
        return None
    best = max(pool, key=lambda f: f.get("abr") or f.get("tbr") or 0)
    return best.get("url")


def _extract(url, extractor_args):
    opts = dict(_BASE_OPTS)
    opts["extractor_args"] = extractor_args
    with yt_dlp.YoutubeDL(opts) as ydl:
        return ydl.extract_info(url, download=False)


def get_audio_url(video_id: str) -> str:
    """Resolve a playable audio URL. Cached, with a slow-but-reliable fallback."""
    url = _watch_url(video_id)

    cached = _cache_get(url)
    if cached:
        return cached

    last_error = None
    for extractor_args in (_FAST_EXTRACTOR_ARGS, _SAFE_EXTRACTOR_ARGS):
        try:
            info = _extract(url, extractor_args)
            direct_url = _pick_url(info)
            if not direct_url:
                last_error = ValueError("no usable audio format")
                continue
            duration = info.get("duration")
            payload = json.dumps({
                "url": direct_url,
                "durationSeconds": int(duration) if duration else None,
                "title": info.get("title"),
                "uploader": info.get("uploader") or info.get("channel"),
            })
            _cache_put(url, payload)
            return payload
        except Exception as error:  # noqa: BLE001 - fall through to next client
            last_error = error

    raise RuntimeError(f"Could not extract audio for {url}: {last_error}")


def prefetch_audio_url(video_id: str) -> str:
    """Warm the cache for an upcoming track. Never raises."""
    try:
        get_audio_url(video_id)
        return "ok"
    except Exception as error:  # noqa: BLE001
        return f"failed: {error}"


def clear_cache() -> str:
    with _CACHE_LOCK:
        _CACHE.clear()
    return "ok"


def search(query: str) -> str:
    opts = dict(_BASE_OPTS)
    opts["extractor_args"] = _FAST_EXTRACTOR_ARGS
    opts["extract_flat"] = True
    opts.pop("format", None)

    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(f"ytsearch10:{query}", download=False)
        entries = info.get("entries") or []

        results = []
        for entry in entries:
            if not entry:
                continue

            video_id = entry.get("id", "")
            webpage_url = entry.get("url") or entry.get("webpage_url") or (
                f"https://www.youtube.com/watch?v={video_id}" if video_id else ""
            )
            if not webpage_url:
                continue

            thumbnail = entry.get("thumbnail")
            if not thumbnail:
                thumbnails = entry.get("thumbnails") or []
                if thumbnails:
                    thumbnail = thumbnails[-1].get("url")

            results.append({
                "title": entry.get("title", "Unknown title"),
                "url": webpage_url,
                "thumbnailUrl": thumbnail,
                "uploader": entry.get("uploader") or entry.get("channel") or "",
            })

        return json.dumps(results)
