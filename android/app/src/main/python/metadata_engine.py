import json
import re
import requests
from ytmusicapi import YTMusic

_ytmusic = YTMusic()

def _duration_seconds(entry):
    if entry.get("duration_seconds") is not None:
        return entry.get("duration_seconds")
    raw = entry.get("duration") or entry.get("length")
    if not raw or not isinstance(raw, str) or ":" not in raw:
        return None
    try:
        parts = [int(p) for p in raw.split(":")]
        seconds = 0
        for part in parts:
            seconds = seconds * 60 + part
        return seconds
    except (ValueError, TypeError):
        return None

def _thumbnail_url(thumbnails):
    if not thumbnails:
        return None
    url = thumbnails[-1]["url"]
    high_res_url = re.sub(r"=w\d+-h\d+.*$", "=w1080-h1080-l90-rj", url)
    return high_res_url

def _artist_name(artists):
    return artists[0]["name"] if artists and artists[0].get("name") else ""

def get_up_next(video_id: str, limit: int = 10) -> str:
    result = _ytmusic.get_watch_playlist(videoId=video_id, limit=limit + 5)
    tracks = result.get("tracks", [])

    up_next = []
    for track in tracks:
        track_video_id = track.get("videoId")
        if not track_video_id or track_video_id == video_id:
            continue
        up_next.append({
            "videoId": track_video_id,
            "title": track.get("title", "Unknown title"),
            "artist": _artist_name(track.get("artists")),
            "thumbnailUrl": _thumbnail_url(track.get("thumbnail") or []),
            "durationSeconds": _duration_seconds(track),
        })
        if len(up_next) >= limit:
            break
    return json.dumps(up_next)

def _entry_to_item(entry):
    video_id = entry.get("videoId")
    if not video_id:
        return None
    return {
        "videoId": video_id,
        "title": entry.get("title", "Unknown title"),
        "artist": _artist_name(entry.get("artists")) or (entry.get("author") or ""),
        "thumbnailUrl": _thumbnail_url(entry.get("thumbnails") or []),
        "durationSeconds": _duration_seconds(entry),
    }

def _score(item, query, position, source_bonus):
    """Rank the way a person would expect from typing the same words on YouTube."""
    q = query.lower().strip()
    tokens = [t for t in re.split(r"\s+", q) if t]
    haystack = (item["title"] + " " + (item["artist"] or "")).lower()

    score = source_bonus
    if q and q in haystack:
        score += 60
    if q and item["title"].lower().startswith(q):
        score += 25
    matched = sum(1 for t in tokens if t in haystack)
    if tokens:
        score += 45 * (matched / len(tokens))
    # Prefer real tracks over hour-long mixes / 20-second clips.
    duration = item.get("durationSeconds") or 0
    if 60 <= duration <= 900:
        score += 12
    elif duration > 2400:
        score -= 20
    # Keep each provider's own ordering as a tiebreaker.
    score -= position * 0.6
    return score

def search_songs(query: str, limit: int = 20) -> str:
    """
    Search songs, videos and the "top result" shelf together, then merge and
    re-rank so the list matches what the same keywords return on YouTube.
    """
    buckets = []
    for filter_name, bonus in (("songs", 22), ("videos", 8), (None, 14)):
        try:
            if filter_name:
                results = _ytmusic.search(query, filter=filter_name, limit=limit)
            else:
                results = _ytmusic.search(query, limit=limit)
        except Exception:
            continue
        buckets.append((results or [], bonus))

    merged = {}
    for results, bonus in buckets:
        for position, entry in enumerate(results):
            if entry.get("resultType") in ("artist", "album", "playlist"):
                continue
            item = _entry_to_item(entry)
            if not item:
                continue
            score = _score(item, query, position, bonus)
            existing = merged.get(item["videoId"])
            if existing is None:
                item["_score"] = score
                merged[item["videoId"]] = item
            else:
                # Appearing in several shelves is itself a relevance signal.
                existing["_score"] = max(existing["_score"], score) + 10
                if not existing.get("durationSeconds"):
                    existing["durationSeconds"] = item.get("durationSeconds")
                if not existing.get("artist"):
                    existing["artist"] = item.get("artist")

    ranked = sorted(merged.values(), key=lambda i: i["_score"], reverse=True)[:limit]
    for item in ranked:
        item.pop("_score", None)
    return json.dumps(ranked)

def get_trending(limit: int = 15) -> str:
    items = []
    try:
        charts = _ytmusic.get_charts(country="IN")
        videos_section = charts.get("videos") or charts.get("trending") or {}
        raw_list = videos_section.get("items") if isinstance(videos_section, dict) else videos_section
        raw_list = raw_list or []

        for entry in raw_list:
            video_id = entry.get("videoId")
            if not video_id:
                continue
            items.append({
                "videoId": video_id,
                "title": entry.get("title", "Unknown title"),
                "artist": _artist_name(entry.get("artists")),
                "thumbnailUrl": _thumbnail_url(entry.get("thumbnails") or []),
                "durationSeconds": _duration_seconds(entry),
            })
    except Exception:
        items = []

    if not items:
        fallback = json.loads(search_songs("trending music 2026", limit=limit))
        items = fallback
    return json.dumps(items[:limit])

def get_lyrics(title: str, artist: str, duration_seconds: int = 0) -> str:
    clean_artist = re.sub(r"\s*-\s*Topic$", "", artist).strip()
    params = {
        "track_name": title,
        "artist_name": clean_artist,
    }
    if duration_seconds > 0:
        params["duration"] = duration_seconds

    try:
        response = requests.get(
            "https://lrclib.net/api/get",
            params=params,
            headers={"User-Agent": "Harmix/0.1 (Android app)"},
            timeout=8,
        )
        if response.status_code != 200:
            return json.dumps({"syncedLyrics": None, "plainLyrics": None, "found": False})

        data = response.json()
        return json.dumps({
            "syncedLyrics": data.get("syncedLyrics"),
            "plainLyrics": data.get("plainLyrics"),
            "found": True,
        })
    except Exception:
        return json.dumps({"syncedLyrics": None, "plainLyrics": None, "found": False})
