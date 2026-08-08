import json
import yt_dlp

_COMMON_OPTS = {
    "quiet": True,
    "no_warnings": True,
    "extractor_args": {
        "youtube": {
            "player_client": ["android"],
        }
    },
}

def get_audio_url(video_id: str) -> str:
    url = video_id if video_id.startswith("http") else f"https://www.youtube.com/watch?v={video_id}"
    opts = dict(_COMMON_OPTS)
    opts["format"] = "bestaudio[ext=m4a]/bestaudio/best"

    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(url, download=False)

        direct_url = info.get("url")
        duration_seconds = info.get("duration")

        if not direct_url:
            candidates = info.get("requested_formats") or info.get("formats") or []
            audio_only = [f for f in candidates if f.get("acodec") not in (None, "none") and f.get("vcodec") == "none"]
            pool = audio_only if audio_only else candidates

            if not pool:
                raise ValueError(f"yt-dlp returned no usable formats for {url}")

            best = max(pool, key=lambda f: f.get("abr") or f.get("tbr") or 0)
            direct_url = best.get("url")

            if not direct_url:
                raise ValueError(f"Best-format selection had no direct URL for {url}")

        return json.dumps({
            "url": direct_url,
            "durationSeconds": int(duration_seconds) if duration_seconds else None,
        })

def search(query: str) -> str:
    opts = dict(_COMMON_OPTS)
    opts["extract_flat"] = True 

    search_term = f"ytsearch10:{query}"

    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(search_term, download=False)
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
