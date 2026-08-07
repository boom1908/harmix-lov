import { useMemo, useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  Download,
  Heart,
  LayoutGrid,
  List,
  Plus,
  RefreshCw,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PageHeader } from "@/components/harmix/AppShell";
import { TrackRow } from "@/components/harmix/TrackRow";
import { cn } from "@/lib/utils";
import { ALBUMS, ARTISTS, type Track } from "@/lib/harmix-data";
import { useHarmix } from "@/lib/harmix-store";

export const Route = createFileRoute("/library/")({
  head: () => ({
    meta: [
      { title: "Your Library — Harmix" },
      {
        name: "description",
        content:
          "Playlists, albums, artists, liked songs and downloads — organise your Harmix library your way.",
      },
      { property: "og:title", content: "Your Library — Harmix" },
      {
        property: "og:description",
        content: "Playlists, albums, artists, liked songs and downloads in one place.",
      },
    ],
  }),
  component: LibraryPage,
});

const FILTERS = ["Playlists", "Albums", "Artists", "Liked", "Downloaded"] as const;
type Filter = (typeof FILTERS)[number];

function LibraryPage() {
  const h = useHarmix();
  const [filter, setFilter] = useState<Filter>("Playlists");
  const [view, setView] = useState<"grid" | "list">("grid");
  const [sort, setSort] = useState("recent");
  const [newOpen, setNewOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");

  const playlists = useMemo(() => {
    const copy = [...h.playlists];
    if (sort === "az") copy.sort((a, b) => a.title.localeCompare(b.title));
    if (sort === "size") copy.sort((a, b) => b.trackIds.length - a.trackIds.length);
    return copy;
  }, [h.playlists, sort]);

  const likedTracks = h.liked.map(h.trackById).filter((t): t is Track => Boolean(t));
  const downloadedTracks = h.downloaded.map(h.trackById).filter((t): t is Track => Boolean(t));

  return (
    <div className="space-y-5 pb-6">
      <PageHeader
        title="Your Library"
        subtitle="Harmix"
        action={
          <Dialog open={newOpen} onOpenChange={setNewOpen}>
            <DialogTrigger asChild>
              <Button size="sm" className="rounded-full">
                <Plus className="size-4" /> New
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Create playlist</DialogTitle>
              </DialogHeader>
              <div className="space-y-3">
                <Input
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="Playlist name"
                  aria-label="Playlist name"
                />
                <Textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Description (optional)"
                  aria-label="Playlist description"
                />
              </div>
              <DialogFooter>
                <Button
                  onClick={() => {
                    if (!title.trim()) return;
                    h.createPlaylist(title.trim(), description.trim());
                    setTitle("");
                    setDescription("");
                    setNewOpen(false);
                    toast.success("Playlist created");
                  }}
                >
                  Create
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        }
      />

      <div className="no-scrollbar flex gap-2 overflow-x-auto px-4">
        {FILTERS.map((f) => (
          <button
            key={f}
            type="button"
            onClick={() => setFilter(f)}
            className={cn(
              "shrink-0 rounded-full border px-4 py-1.5 text-sm font-medium transition",
              filter === f
                ? "border-transparent sunset-gradient text-black"
                : "border-border text-muted-foreground",
            )}
          >
            {f}
          </button>
        ))}
      </div>

      <div className="flex items-center justify-between px-4">
        <Select value={sort} onValueChange={setSort}>
          <SelectTrigger className="h-9 w-40" aria-label="Sort by">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="recent">Recently added</SelectItem>
            <SelectItem value="az">A – Z</SelectItem>
            <SelectItem value="size">Most songs</SelectItem>
          </SelectContent>
        </Select>
        <div className="flex gap-1 rounded-full border border-border p-1">
          <button
            type="button"
            aria-label="Grid view"
            onClick={() => setView("grid")}
            className={cn("rounded-full p-1.5", view === "grid" && "bg-secondary text-primary")}
          >
            <LayoutGrid className="size-4" />
          </button>
          <button
            type="button"
            aria-label="List view"
            onClick={() => setView("list")}
            className={cn("rounded-full p-1.5", view === "list" && "bg-secondary text-primary")}
          >
            <List className="size-4" />
          </button>
        </div>
      </div>

      {filter === "Playlists" && (
        <div className="px-4">
          <div className="mb-3 flex gap-3">
            <QuickCard
              icon={<Heart className="size-5" />}
              label="Liked songs"
              count={likedTracks.length}
              onClick={() => setFilter("Liked")}
            />
            <QuickCard
              icon={<Download className="size-5" />}
              label="Downloads"
              count={downloadedTracks.length}
              onClick={() => setFilter("Downloaded")}
            />
          </div>

          {playlists.length === 0 ? (
            <EmptyState onCreate={() => setNewOpen(true)} />
          ) : view === "grid" ? (
            <div className="grid grid-cols-2 gap-4">
              {playlists.map((p) => (
                <Link key={p.id} to="/library/$playlistId" params={{ playlistId: p.id }}>
                  <div
                    className="aspect-square rounded-2xl border border-border/60 shadow-lg"
                    style={{ backgroundImage: p.art }}
                    role="img"
                    aria-label={p.title}
                  />
                  <p className="mt-2 truncate text-sm font-semibold">{p.title}</p>
                  <p className="truncate text-xs text-muted-foreground">
                    {p.trackIds.length} songs {p.source === "ytmusic" && "· YT Music"}
                  </p>
                </Link>
              ))}
            </div>
          ) : (
            <ul className="space-y-1">
              {playlists.map((p) => (
                <li key={p.id}>
                  <Link
                    to="/library/$playlistId"
                    params={{ playlistId: p.id }}
                    className="flex items-center gap-3 rounded-xl px-2 py-2 hover:bg-secondary/60"
                  >
                    <span
                      className="size-12 rounded-lg border border-border/60"
                      style={{ backgroundImage: p.art }}
                    />
                    <span className="min-w-0">
                      <span className="block truncate text-sm font-semibold">{p.title}</span>
                      <span className="block truncate text-xs text-muted-foreground">
                        {p.trackIds.length} songs {p.source === "ytmusic" && "· YT Music"}
                      </span>
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}

          <div className="mt-6 flex items-center justify-between rounded-2xl border border-border p-4">
            <div>
              <p className="text-sm font-semibold">YouTube Music sync</p>
              <p className="text-xs text-muted-foreground">
                {h.account ? `Connected as ${h.account.email}` : "Not connected yet"}
              </p>
            </div>
            {h.account ? (
              <Button variant="outline" size="sm" onClick={() => toast.info("Sync runs once the backend is connected")}>
                <RefreshCw className="size-4" /> Sync
              </Button>
            ) : (
              <Button asChild size="sm">
                <Link to="/login">Connect</Link>
              </Button>
            )}
          </div>
        </div>
      )}

      {filter === "Albums" && (
        <div className="grid grid-cols-2 gap-4 px-4">
          {ALBUMS.map((a) => (
            <div key={a.id}>
              <div className="aspect-square rounded-2xl border border-border/60" style={{ backgroundImage: a.art }} role="img" aria-label={a.title} />
              <p className="mt-2 truncate text-sm font-semibold">{a.title}</p>
              <p className="truncate text-xs text-muted-foreground">{a.artist} · {a.year}</p>
            </div>
          ))}
        </div>
      )}

      {filter === "Artists" && (
        <div className="grid grid-cols-3 gap-4 px-4">
          {ARTISTS.map((a) => (
            <div key={a.id} className="text-center">
              <div className="aspect-square rounded-full border border-border/60" style={{ backgroundImage: a.art }} role="img" aria-label={a.name} />
              <p className="mt-2 truncate text-xs font-semibold">{a.name}</p>
            </div>
          ))}
        </div>
      )}

      {filter === "Liked" && <TrackList tracks={likedTracks} empty="No liked songs yet." />}
      {filter === "Downloaded" && (
        <TrackList tracks={downloadedTracks} empty="Nothing downloaded yet." />
      )}
    </div>
  );
}

function TrackList({ tracks, empty }: { tracks: Track[]; empty: string }) {
  const h = useHarmix();
  if (!tracks.length)
    return (
      <p className="mx-4 rounded-2xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
        {empty}
      </p>
    );
  return (
    <div className="space-y-1 px-4">
      {tracks.map((t, i) => (
        <TrackRow
          key={t.id}
          track={t}
          index={i}
          onPlay={() => h.playTracks(tracks.map((x) => x.id), i)}
        />
      ))}
    </div>
  );
}

function QuickCard({
  icon,
  label,
  count,
  onClick,
}: {
  icon: React.ReactNode;
  label: string;
  count: number;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex flex-1 items-center gap-3 rounded-2xl border border-border bg-card p-3 text-left"
    >
      <span className="grid size-10 place-items-center rounded-xl sunset-gradient text-black">{icon}</span>
      <span className="min-w-0">
        <span className="block truncate text-sm font-semibold">{label}</span>
        <span className="block text-xs text-muted-foreground">{count} songs</span>
      </span>
    </button>
  );
}

function EmptyState({ onCreate }: { onCreate: () => void }) {
  return (
    <div className="rounded-2xl border border-dashed border-border p-8 text-center">
      <p className="font-display text-xl tracking-wide">No playlists yet</p>
      <p className="mt-1 text-sm text-muted-foreground">Create your first playlist to get going.</p>
      <Button className="mt-4" onClick={onCreate}>
        <Plus className="size-4" /> New playlist
      </Button>
    </div>
  );
}