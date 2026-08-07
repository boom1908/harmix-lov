import { useState } from "react";
import {
  Check,
  Download,
  GripVertical,
  Heart,
  ListPlus,
  ListEnd,
  MoreVertical,
  Play,
  Plus,
  Trash2,
} from "lucide-react";
import { toast } from "sonner";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Checkbox } from "@/components/ui/checkbox";
import { cn } from "@/lib/utils";
import { formatTime, type Track } from "@/lib/harmix-data";
import { useHarmix } from "@/lib/harmix-store";

type Props = {
  track: Track;
  index?: number;
  onPlay: () => void;
  selectable?: boolean;
  selected?: boolean;
  onSelect?: (checked: boolean) => void;
  onRemove?: () => void;
  draggable?: boolean;
  dragHandlers?: React.HTMLAttributes<HTMLDivElement>;
};

export function TrackRow({
  track,
  index,
  onPlay,
  selectable,
  selected,
  onSelect,
  onRemove,
  draggable,
  dragHandlers,
}: Props) {
  const { liked, downloaded, toggleLike, toggleDownload, playNext, addToQueue, current } =
    useHarmix();
  const [addOpen, setAddOpen] = useState(false);
  const isCurrent = current?.id === track.id;

  return (
    <>
      <div
        {...dragHandlers}
        draggable={draggable}
        className={cn(
          "group flex items-center gap-3 rounded-xl px-2 py-2 transition-colors hover:bg-secondary/60",
          isCurrent && "bg-secondary/70",
        )}
      >
        {selectable ? (
          <Checkbox
            checked={selected}
            onCheckedChange={(v) => onSelect?.(Boolean(v))}
            aria-label={`Select ${track.title}`}
          />
        ) : draggable ? (
          <GripVertical className="size-4 shrink-0 cursor-grab text-muted-foreground" />
        ) : index !== undefined ? (
          <span className="w-5 shrink-0 text-center text-xs text-muted-foreground">{index + 1}</span>
        ) : null}

        <button
          type="button"
          onClick={onPlay}
          className="flex min-w-0 flex-1 items-center gap-3 text-left"
        >
          <span
            className="relative grid size-11 shrink-0 place-items-center rounded-lg border border-border/60"
            style={{ backgroundImage: track.art }}
          >
            <Play className="size-4 fill-current text-black/70 opacity-0 transition group-hover:opacity-100" />
          </span>
          <span className="min-w-0 flex-1">
            <span
              className={cn(
                "block truncate text-sm font-medium",
                isCurrent ? "text-primary" : "text-foreground",
              )}
            >
              {track.title}
            </span>
            <span className="block truncate text-xs text-muted-foreground">
              {track.artist} · {track.album}
            </span>
          </span>
        </button>

        {downloaded.includes(track.id) && (
          <Check className="size-4 shrink-0 text-primary" aria-label="Downloaded" />
        )}
        <span className="shrink-0 text-xs tabular-nums text-muted-foreground">
          {formatTime(track.duration)}
        </span>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              aria-label={`More options for ${track.title}`}
              className="shrink-0 rounded-md p-1 text-muted-foreground hover:text-foreground"
            >
              <MoreVertical className="size-4" />
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuItem onSelect={() => toggleLike(track.id)}>
              <Heart
                className={cn("size-4", liked.includes(track.id) && "fill-primary text-primary")}
              />
              {liked.includes(track.id) ? "Remove from Liked" : "Add to Liked"}
            </DropdownMenuItem>
            <DropdownMenuItem onSelect={() => setAddOpen(true)}>
              <Plus className="size-4" /> Add to playlist
            </DropdownMenuItem>
            <DropdownMenuItem
              onSelect={() => {
                playNext(track.id);
                toast.success("Playing next");
              }}
            >
              <ListPlus className="size-4" /> Play next
            </DropdownMenuItem>
            <DropdownMenuItem
              onSelect={() => {
                addToQueue(track.id);
                toast.success("Added to queue");
              }}
            >
              <ListEnd className="size-4" /> Add to queue
            </DropdownMenuItem>
            <DropdownMenuItem onSelect={() => toggleDownload(track.id)}>
              <Download className="size-4" />
              {downloaded.includes(track.id) ? "Remove download" : "Download"}
            </DropdownMenuItem>
            {onRemove && (
              <>
                <DropdownMenuSeparator />
                <DropdownMenuItem variant="destructive" onSelect={onRemove}>
                  <Trash2 className="size-4" /> Remove from playlist
                </DropdownMenuItem>
              </>
            )}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <AddToPlaylistDialog open={addOpen} onOpenChange={setAddOpen} trackIds={[track.id]} />
    </>
  );
}

export function AddToPlaylistDialog({
  open,
  onOpenChange,
  trackIds,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  trackIds: string[];
}) {
  const { playlists, addToPlaylist, createPlaylist } = useHarmix();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add to playlist</DialogTitle>
          <DialogDescription>
            {trackIds.length} {trackIds.length === 1 ? "song" : "songs"} selected
          </DialogDescription>
        </DialogHeader>
        <div className="max-h-80 space-y-1 overflow-y-auto">
          <button
            type="button"
            onClick={() => {
              const id = createPlaylist("New playlist");
              addToPlaylist(id, trackIds);
              onOpenChange(false);
              toast.success("Created playlist");
            }}
            className="flex w-full items-center gap-3 rounded-xl px-2 py-2 hover:bg-secondary/60"
          >
            <span className="grid size-11 place-items-center rounded-lg border border-dashed border-border">
              <Plus className="size-4 text-primary" />
            </span>
            <span className="text-sm font-medium">New playlist</span>
          </button>
          {playlists.map((p) => (
            <button
              key={p.id}
              type="button"
              onClick={() => {
                addToPlaylist(p.id, trackIds);
                onOpenChange(false);
                toast.success(`Added to ${p.title}`);
              }}
              className="flex w-full items-center gap-3 rounded-xl px-2 py-2 text-left hover:bg-secondary/60"
            >
              <span className="size-11 rounded-lg border border-border/60" style={{ backgroundImage: p.art }} />
              <span className="min-w-0">
                <span className="block truncate text-sm font-medium">{p.title}</span>
                <span className="block text-xs text-muted-foreground">{p.trackIds.length} songs</span>
              </span>
            </button>
          ))}
        </div>
      </DialogContent>
    </Dialog>
  );
}