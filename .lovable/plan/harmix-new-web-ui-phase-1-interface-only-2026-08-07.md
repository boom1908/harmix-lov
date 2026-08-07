# Harmix — New Web UI (Phase 1: interface only)

Build the full Harmix interface as a web app in this workspace, using your Android app as the blueprint. No backend work in this phase: every screen runs on realistic placeholder data so you can review the look and flow in the preview. Once you connect GitHub, we wire it to your existing Kotlin + yt-dlp backend as-is.

## Theme: Sunset Gold on Black

- Deep black base (#08070A) with warm charcoal surfaces
- Gold accent ramp: amber → gold → soft champagne for highlights, active states, progress bars
- Sunset gradient (ember orange → gold) for hero art, the now-playing bar glow, and primary buttons
- All colors defined as design tokens so nothing is hardcoded; dark-first
- Type: bold condensed display headings + clean sans body; generous rounded cards, subtle gold-tinted borders and glows

## Screens

**Login**
- Full-bleed sunset gradient panel, Harmix wordmark, "Continue with Google" button
- States: idle, connecting, connected (shows account chip + "Sync YouTube Music library")
- Visual only in this phase; the real Google auth hooks into your backend later

**Home**
- Greeting header, "Recently played" row, "Made for you" mixes, "Because you listened to…" personalization shelves, "Jump back in"
- Horizontal scrolling carousels of album/playlist cards

**Search**
- Prominent search field, genre/mood tiles
- Search history list with per-item remove and "Clear all"
- Results grouped by Songs / Albums / Artists / Playlists with tabs

**Library (full rebuild)**
- Filter chips: Playlists, Albums, Artists, Liked, Downloaded
- Grid/list toggle, sort by recent / A–Z / most played
- Playlist detail page: cover art, title, description, play/shuffle, track list with duration and menu
- Playlist actions: create, rename, edit cover/description, delete
- Track actions: add to playlist, remove from playlist, drag-to-reorder, play next, add to queue, like, download toggle
- Empty states and multi-select for bulk add/remove

**Player**
- Mini bar pinned above the tab bar; expands to full-screen player
- Large art with sunset glow, scrubber, shuffle/repeat, like, queue sheet with reorderable upcoming tracks
- Lyrics tab placeholder

**Profile / Settings**
- Listening history with stats (top artists, top tracks, minutes listened)
- Connected accounts (Google/YT Music sync status), theme, audio quality, storage

**Removed:** the Community tab. Bottom nav becomes Home · Search · Library · Profile.

## UI audit

Every screen gets checked in the preview at phone width for layout breaks, overflow, contrast, dead buttons, and broken navigation. Anything broken gets fixed before handoff.

## Technical notes

- TanStack Start routes: `/login`, `/` (home), `/search`, `/library`, `/library/$playlistId`, `/profile`
- Theme tokens in `src/styles.css` (oklch), shadcn components restyled to the gold/black system
- Placeholder data in a single mock module so swapping in real API calls later touches one layer
- Reordering uses a lightweight drag-and-drop; player state kept in a shared context ready to be pointed at your backend

## Answers to your questions

- **PWA/backend:** a PWA is just this web app with an install manifest — it does not change your backend. Playlist fetching from your own Kotlin backend works fine as long as that server allows this app's origin (a one-line CORS setting). Background playback works on Android via the media session API, though it is less reliable than the native app; on iOS it is limited.
- **Backend stays as-is:** nothing here touches Kotlin or yt-dlp. Phase 2 connects this UI to your existing endpoints after you link GitHub.
