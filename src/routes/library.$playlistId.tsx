import { useState } from "react";
import { createFileRoute, Link, useParams } from "@tanstack/react-router";
import { ArrowLeft, MoreVertical, Pencil, Play, Plus, Shuffle, Trash2 } from "lucide-react";
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
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { TrackRow, AddToPlaylistDialog } from "@/components/harmix/TrackRow";
import { useHarmix } from "@/lib/harmix-store";
import type { Track } from "@/lib/harmix-data";

export const Route = createFileRoute("/library/$playlistId")({
  head: () => ({
    meta: [
      { title: "Playlist — Harmix" },
      { name: "description", content: "Play, reorder and manage the songs in your Harmix playlist." },
      { property: "og:title", content: "Playlist — Harmix" },
      { property: "og:description", content: "Play, reorder and manage your playlist songs." },
    ],
  }),
  component: PlaylistPage,
});

function PlaylistPage() {
  const { playlistId } = useParams({ from: "/library/$playlistId" });
  const h = useHarmix();
  const playlist = h.playlists.find((p) => p.id === playlistId);
  const [editOpen, setEditOpen] = useState(false);
  const [selectMode, setSelectMode] = useState(false);
  const [selected, setSelected] = useState<string[]>([]);
  const [addOpen, setAddOpen] = useState(false);
  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [title, setTitle] = useState(playlist?.title ?? "");
  const [description, setDescription] = useState(playlist?.description ?? "");

  if (!playlist) {
    return (
      <div className="p-8 text-center">
        <p className="text-sm text-muted-foreground">This playlist no longer exists.</p>
        <Button asChild className="mt-4">
          <Link to="/library">Back to library</Link>
        </Button>
      </div>
    );
  }

  const tracks = playlist.trackIds.map(h.trackById).filter((t): t is Track => Boolean(t));
  const total = tracks.reduce((a, t) => a + t.duration, 0);

  return (
    <div className="pb-6">
      <div className="relative px-4 pb-4 pt-6">
        <div
          className="pointer-events-none absolute inset-x-0 top-0 h-64 opacity-25 blur-3xl"
          style={{ backgroundImage: playlist.art }}
          aria-hidden
        />
        <div className="relative flex items-center justify-between">
          <Link to="/library" aria-label="Back to library">
            <ArrowLeft className="size-5" />
          </Link>
          <DropdownMenu>
            <DropdownMenuTrigger aria-label="Playlist options">
              <MoreVertical className="size-5" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onSelect={() => setEditOpen(true)}>
                <Pencil className="size-4" /> Edit details
              </DropdownMenuItem>
              <DropdownMenuItem onSelect={() => setSelectMode((s) => !s)}>
                <Plus className="size-4" /> {selectMode ? "Exit selection" : "Select songs"}
              </DropdownMenuItem>
              <DropdownMenuItem
                className="text-destructive focus:text-destructive"
                onSelect={() => {
                  h.deletePlaylist(playlist.id);
                  toast.success("Playlist deleted");
                }}
              >
                <Trash2 className="size-4" /> Delete playlist
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        <div className="relative mt-4 flex flex-col items-center text-center">
          <div
            className="glow size-44 rounded-2xl border border-border/60"
            style={{ backgroundImage: playlist.art }}
            role="img"
            aria-label={playlist.title}
          />
          <h1 className="mt-4 font-display text-3xl tracking-wide">{playlist.title}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{playlist.description}</p>
          <p className="mt-1 text-xs text-muted-foreground">
            {tracks.length} songs · {Math.round(total / 60)} min
            {playlist.source === "ytmusic" && " · synced from YT Music"}
          </p>
          <div className="mt-4 flex gap-2">
            <Button className="rounded-full" onClick={() => h.playTracks(playlist.trackIds)}>
              <Play className="size-4 fill-current" /> Play
            </Button>
            <Button
              variant="outline"
              className="rounded-full"
              onClick={() => h.playTracks(playlist.trackIds, 0, true)}
            >
              <Shuffle className="size-4" /> Shuffle
            </Button>
          </div>
        </div>
      </div>

      {selectMode && (
        <div className="mx-4 mb-3 flex items-center justify-between rounded-xl border border-border p-2">
          <span className="text-xs text-muted-foreground">{selected.length} selected</span>
          <div className="flex gap-2">
            <Button size="sm" variant="outline" disabled={!selected.length} onClick={() => setAddOpen(true)}>
              Add to…
            </Button>
            <Button
              size="sm"
              variant="destructive"
              disabled={!selected.length}
              onClick={() => {
                selected.forEach((id) => h.removeFromPlaylist(playlist.id, id));
                setSelected([]);
                toast.success("Removed from playlist");
              }}
            >
              Remove
            </Button>
          </div>
        </div>
      )}

      <div className="space-y-1 px-4">
        {tracks.length === 0 && (
          <p className="rounded-2xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
            No songs here yet. Add some from Search.
          </p>
        )}
        {tracks.map((t, i) => (
          <TrackRow
            key={t.id}
            track={t}
            index={i}
            draggable={!selectMode}
            dragHandlers={{
              onDragStart: () => setDragIndex(i),
              onDragOver: (e) => e.preventDefault(),
              onDrop: () => {
                if (dragIndex !== null && dragIndex !== i) h.reorderPlaylist(playlist.id, dragIndex, i);
                setDragIndex(null);
              },
            }}
            selectable={selectMode}
            selected={selected.includes(t.id)}
            onSelect={(checked) =>
              setSelected((s) => (checked ? [...s, t.id] : s.filter((x) => x !== t.id)))
            }
            onPlay={() => h.playTracks(playlist.trackIds, i)}
            onRemove={() => {
              h.removeFromPlaylist(playlist.id, t.id);
              toast.success("Removed from playlist");
            }}
          />
        ))}
      </div>

      <AddToPlaylistDialog open={addOpen} onOpenChange={setAddOpen} trackIds={selected} />

      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit playlist</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <Input value={title} onChange={(e) => setTitle(e.target.value)} aria-label="Playlist name" />
            <Textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              aria-label="Playlist description"
            />
          </div>
          <DialogFooter>
            <Button
              onClick={() => {
                h.updatePlaylist(playlist.id, { title, description });
                setEditOpen(false);
                toast.success("Playlist updated");
              }}
            >
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}