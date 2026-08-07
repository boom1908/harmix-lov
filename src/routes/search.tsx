import { useMemo, useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { Clock, Search as SearchIcon, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MediaCard } from "@/components/harmix/MediaCard";
import { TrackRow } from "@/components/harmix/TrackRow";
import { PageHeader } from "@/components/harmix/AppShell";
import { ALBUMS, ARTISTS, MOODS } from "@/lib/harmix-data";
import { useHarmix } from "@/lib/harmix-store";

export const Route = createFileRoute("/search")({
  head: () => ({
    meta: [
      { title: "Search — Harmix" },
      {
        name: "description",
        content: "Search songs, albums, artists and playlists in Harmix, with your recent searches.",
      },
      { property: "og:title", content: "Search — Harmix" },
      { property: "og:description", content: "Search songs, albums, artists and playlists." },
    ],
  }),
  component: SearchPage,
});

function SearchPage() {
  const h = useHarmix();
  const [q, setQ] = useState("");
  const query = q.trim().toLowerCase();

  const songs = useMemo(
    () =>
      query
        ? h.tracks.filter(
            (t) =>
              t.title.toLowerCase().includes(query) ||
              t.artist.toLowerCase().includes(query) ||
              t.album.toLowerCase().includes(query),
          )
        : [],
    [query, h.tracks],
  );
  const albums = ALBUMS.filter(
    (a) => query && (a.title.toLowerCase().includes(query) || a.artist.toLowerCase().includes(query)),
  );
  const artists = ARTISTS.filter((a) => query && a.name.toLowerCase().includes(query));
  const playlists = h.playlists.filter((p) => query && p.title.toLowerCase().includes(query));

  return (
    <div className="space-y-6">
      <PageHeader title="Search" subtitle="Find anything" />

      <div className="px-4">
        <div className="relative">
          <SearchIcon className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && q.trim()) h.pushSearch(q.trim());
            }}
            placeholder="Songs, artists, albums…"
            aria-label="Search"
            className="h-12 rounded-full pl-10 pr-10"
          />
          {q && (
            <button
              type="button"
              aria-label="Clear search field"
              onClick={() => setQ("")}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground"
            >
              <X className="size-4" />
            </button>
          )}
        </div>
      </div>

      {!query ? (
        <>
          {h.searchHistory.length > 0 && (
            <section className="px-4">
              <div className="mb-2 flex items-center justify-between">
                <h2 className="font-display text-xl tracking-wide">Recent searches</h2>
                <button
                  type="button"
                  onClick={h.clearSearch}
                  className="text-xs font-semibold text-primary"
                >
                  Clear all
                </button>
              </div>
              <ul className="space-y-1">
                {h.searchHistory.map((item) => (
                  <li key={item} className="flex items-center gap-3 rounded-xl px-2 py-2 hover:bg-secondary/60">
                    <Clock className="size-4 text-muted-foreground" />
                    <button
                      type="button"
                      onClick={() => setQ(item)}
                      className="flex-1 truncate text-left text-sm"
                    >
                      {item}
                    </button>
                    <button
                      type="button"
                      aria-label={`Remove ${item} from history`}
                      onClick={() => h.removeSearch(item)}
                      className="text-muted-foreground"
                    >
                      <X className="size-4" />
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          )}

          <section className="px-4">
            <h2 className="mb-3 font-display text-xl tracking-wide">Browse moods</h2>
            <div className="grid grid-cols-2 gap-3">
              {MOODS.map((m, i) => (
                <button
                  key={m}
                  type="button"
                  onClick={() => setQ(m)}
                  className="relative h-20 overflow-hidden rounded-2xl border border-border/60 p-3 text-left"
                  style={{
                    backgroundImage:
                      i % 2 === 0
                        ? "linear-gradient(135deg, oklch(0.5 0.17 32), oklch(0.2 0.03 60))"
                        : "linear-gradient(135deg, oklch(0.72 0.15 78), oklch(0.25 0.04 55))",
                  }}
                >
                  <span className="font-display text-lg tracking-wide text-white drop-shadow">{m}</span>
                </button>
              ))}
            </div>
          </section>
        </>
      ) : (
        <Tabs defaultValue="songs" className="px-4">
          <TabsList className="w-full">
            <TabsTrigger value="songs" className="flex-1">
              Songs
            </TabsTrigger>
            <TabsTrigger value="albums" className="flex-1">
              Albums
            </TabsTrigger>
            <TabsTrigger value="artists" className="flex-1">
              Artists
            </TabsTrigger>
            <TabsTrigger value="playlists" className="flex-1">
              Playlists
            </TabsTrigger>
          </TabsList>

          <TabsContent value="songs" className="mt-4 space-y-1">
            {songs.length ? (
              songs.map((t, i) => (
                <TrackRow
                  key={t.id}
                  track={t}
                  index={i}
                  onPlay={() => {
                    h.pushSearch(q.trim());
                    h.playTracks(songs.map((s) => s.id), i);
                  }}
                />
              ))
            ) : (
              <Empty />
            )}
          </TabsContent>
          <TabsContent value="albums" className="mt-4">
            <Grid>
              {albums.length ? (
                albums.map((a) => <MediaCard key={a.id} title={a.title} subtitle={a.artist} art={a.art} className="w-full" />)
              ) : (
                <Empty />
              )}
            </Grid>
          </TabsContent>
          <TabsContent value="artists" className="mt-4">
            <Grid>
              {artists.length ? (
                artists.map((a) => (
                  <MediaCard key={a.id} title={a.name} subtitle={`${a.listeners} listeners`} art={a.art} round className="w-full" />
                ))
              ) : (
                <Empty />
              )}
            </Grid>
          </TabsContent>
          <TabsContent value="playlists" className="mt-4">
            <Grid>
              {playlists.length ? (
                playlists.map((p) => (
                  <MediaCard
                    key={p.id}
                    title={p.title}
                    subtitle={`${p.trackIds.length} songs`}
                    art={p.art}
                    className="w-full"
                    to="/library/$playlistId"
                    params={{ playlistId: p.id }}
                  />
                ))
              ) : (
                <Empty />
              )}
            </Grid>
          </TabsContent>
        </Tabs>
      )}
    </div>
  );
}

function Grid({ children }: { children: React.ReactNode }) {
  return <div className="grid grid-cols-2 gap-4">{children}</div>;
}

function Empty() {
  return (
    <p className="col-span-2 rounded-2xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
      Nothing found. Try another search.
    </p>
  );
}