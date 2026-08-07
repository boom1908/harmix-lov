import { createFileRoute, Link } from "@tanstack/react-router";
import { Bell, Play, Shuffle } from "lucide-react";
import { MediaCard, Shelf } from "@/components/harmix/MediaCard";
import { TrackRow } from "@/components/harmix/TrackRow";
import { ALBUMS, ARTISTS } from "@/lib/harmix-data";
import { useHarmix } from "@/lib/harmix-store";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Harmix — Your sunset-gold music player" },
      {
        name: "description",
        content:
          "Harmix is a personal music streaming app: playlists, YT Music sync, listening history and a full-screen player.",
      },
      { property: "og:title", content: "Harmix — Your sunset-gold music player" },
      {
        property: "og:description",
        content: "Playlists, YT Music sync, listening history and a full-screen player.",
      },
    ],
  }),
  component: Index,
});

function Index() {
  const h = useHarmix();
  const recent = h.listening.length
    ? h.listening.map((l) => h.trackById(l.id)).filter(Boolean)
    : h.tracks.slice(0, 6);
  const forYou = h.tracks.slice(6, 12);

  return (
    <div className="space-y-8">
      <header className="relative overflow-hidden px-4 pb-6 pt-8">
        <div
          className="pointer-events-none absolute -right-16 -top-24 size-64 rounded-full opacity-30 blur-3xl sunset-gradient"
          aria-hidden
        />
        <div className="relative flex items-start justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.24em] text-muted-foreground">
              {greeting()}
            </p>
            <h1 className="font-display text-4xl tracking-wide gold-text">Harmix</h1>
          </div>
          <div className="flex items-center gap-3">
            <button type="button" aria-label="Notifications" className="text-muted-foreground">
              <Bell className="size-5" />
            </button>
            <Link
              to="/profile"
              className="grid size-9 place-items-center rounded-full sunset-gradient text-sm font-bold text-black"
            >
              {h.account ? h.account.name.charAt(0) : "H"}
            </Link>
          </div>
        </div>

        <div className="relative mt-5 flex gap-2">
          <button
            type="button"
            onClick={() => h.playTracks(h.tracks.map((t) => t.id))}
            className="glow inline-flex items-center gap-2 rounded-full sunset-gradient px-5 py-2.5 text-sm font-semibold text-black"
          >
            <Play className="size-4 fill-current" /> Play all
          </button>
          <button
            type="button"
            onClick={() => h.playTracks(h.tracks.map((t) => t.id), 0, true)}
            className="inline-flex items-center gap-2 rounded-full border border-border px-5 py-2.5 text-sm font-semibold"
          >
            <Shuffle className="size-4" /> Shuffle
          </button>
        </div>
      </header>

      <Shelf title="Recently played">
        {recent.map(
          (t) =>
            t && (
              <MediaCard
                key={t.id}
                title={t.title}
                subtitle={t.artist}
                art={t.art}
                onPlay={() => h.playTracks([t.id])}
              />
            ),
        )}
      </Shelf>

      <Shelf title="Your playlists">
        {h.playlists.map((p) => (
          <MediaCard
            key={p.id}
            title={p.title}
            subtitle={`${p.trackIds.length} songs`}
            art={p.art}
            to="/library/$playlistId"
            params={{ playlistId: p.id }}
            onPlay={() => h.playTracks(p.trackIds)}
          />
        ))}
      </Shelf>

      <Shelf title="Because you listened to Nova Vale">
        {forYou.map((t) => (
          <MediaCard
            key={t.id}
            title={t.title}
            subtitle={t.artist}
            art={t.art}
            onPlay={() => h.playTracks([t.id])}
          />
        ))}
      </Shelf>

      <Shelf title="Artists you love">
        {ARTISTS.map((a) => (
          <MediaCard key={a.id} title={a.name} subtitle={`${a.listeners} listeners`} art={a.art} round />
        ))}
      </Shelf>

      <Shelf title="New releases">
        {ALBUMS.map((a) => (
          <MediaCard key={a.id} title={a.title} subtitle={`${a.artist} · ${a.year}`} art={a.art} />
        ))}
      </Shelf>

      <section className="space-y-1 px-4">
        <h2 className="mb-2 font-display text-xl tracking-wide">Trending now</h2>
        {h.tracks.slice(0, 6).map((t, i) => (
          <TrackRow
            key={t.id}
            track={t}
            index={i}
            onPlay={() =>
              h.playTracks(
                h.tracks.slice(0, 6).map((x) => x.id),
                i,
              )
            }
          />
        ))}
      </section>
    </div>
  );
}

function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 18) return "Good afternoon";
  return "Good evening";
}
