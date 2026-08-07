import { useState } from "react";
import {
  ChevronDown,
  Heart,
  ListMusic,
  Pause,
  Play,
  Repeat,
  Repeat1,
  Shuffle,
  SkipBack,
  SkipForward,
  X,
} from "lucide-react";
import { Slider } from "@/components/ui/slider";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { cn } from "@/lib/utils";
import { formatTime } from "@/lib/harmix-data";
import { useHarmix } from "@/lib/harmix-store";

export function Player() {
  const h = useHarmix();
  const [queueOpen, setQueueOpen] = useState(false);
  if (!h.current) return null;
  const track = h.current;
  const isLiked = h.liked.includes(track.id);

  return (
    <>
      {/* Mini bar */}
      <div className="pointer-events-auto fixed inset-x-0 bottom-16 z-40 px-3 pb-2">
        <div
          className="glow flex items-center gap-3 rounded-2xl border border-border bg-card/95 p-2 backdrop-blur"
          role="region"
          aria-label="Now playing"
        >
          <button
            type="button"
            onClick={() => h.setExpanded(true)}
            className="flex min-w-0 flex-1 items-center gap-3 text-left"
          >
            <span
              className="size-11 shrink-0 rounded-lg border border-border/60"
              style={{ backgroundImage: track.art }}
            />
            <span className="min-w-0">
              <span className="block truncate text-sm font-semibold">{track.title}</span>
              <span className="block truncate text-xs text-muted-foreground">{track.artist}</span>
            </span>
          </button>
          <button
            type="button"
            aria-label={isLiked ? "Unlike" : "Like"}
            onClick={() => h.toggleLike(track.id)}
            className="p-1 text-muted-foreground"
          >
            <Heart className={cn("size-5", isLiked && "fill-primary text-primary")} />
          </button>
          <button
            type="button"
            aria-label={h.isPlaying ? "Pause" : "Play"}
            onClick={h.togglePlay}
            className="grid size-10 shrink-0 place-items-center rounded-full sunset-gradient text-black"
          >
            {h.isPlaying ? <Pause className="size-5 fill-current" /> : <Play className="size-5 fill-current" />}
          </button>
        </div>
        <div className="mx-4 mt-1 h-0.5 overflow-hidden rounded-full bg-secondary">
          <div
            className="h-full sunset-gradient transition-all"
            style={{ width: `${(h.progress / track.duration) * 100}%` }}
          />
        </div>
      </div>

      {/* Full screen */}
      {h.expanded && (
        <div className="fixed inset-0 z-50 flex flex-col bg-background">
          <div
            className="pointer-events-none absolute inset-x-0 top-0 h-2/3 opacity-30 blur-3xl"
            style={{ backgroundImage: track.art }}
            aria-hidden
          />
          <div className="relative flex items-center justify-between p-4">
            <button type="button" aria-label="Collapse player" onClick={() => h.setExpanded(false)}>
              <ChevronDown className="size-6" />
            </button>
            <p className="text-xs uppercase tracking-[0.2em] text-muted-foreground">Now playing</p>
            <button type="button" aria-label="Open queue" onClick={() => setQueueOpen(true)}>
              <ListMusic className="size-5" />
            </button>
          </div>

          <div className="relative flex flex-1 flex-col overflow-y-auto px-6 pb-8">
            <div
              className="glow mx-auto aspect-square w-full max-w-sm rounded-3xl border border-border/60"
              style={{ backgroundImage: track.art }}
              role="img"
              aria-label={`${track.title} cover art`}
            />
            <div className="mt-6 flex items-start gap-3">
              <div className="min-w-0 flex-1">
                <h1 className="truncate font-display text-3xl tracking-wide">{track.title}</h1>
                <p className="truncate text-sm text-muted-foreground">
                  {track.artist} · {track.album}
                </p>
              </div>
              <button
                type="button"
                aria-label={isLiked ? "Unlike" : "Like"}
                onClick={() => h.toggleLike(track.id)}
              >
                <Heart className={cn("size-6", isLiked && "fill-primary text-primary")} />
              </button>
            </div>

            <div className="mt-6">
              <Slider
                value={[h.progress]}
                max={track.duration}
                step={1}
                onValueChange={([v]) => h.seek(v ?? 0)}
                aria-label="Seek"
              />
              <div className="mt-1 flex justify-between text-xs tabular-nums text-muted-foreground">
                <span>{formatTime(h.progress)}</span>
                <span>-{formatTime(Math.max(0, track.duration - h.progress))}</span>
              </div>
            </div>

            <div className="mt-6 flex items-center justify-between">
              <button
                type="button"
                aria-label="Shuffle"
                onClick={h.toggleShuffle}
                className={h.shuffle ? "text-primary" : "text-muted-foreground"}
              >
                <Shuffle className="size-5" />
              </button>
              <button type="button" aria-label="Previous" onClick={h.prev}>
                <SkipBack className="size-7 fill-current" />
              </button>
              <button
                type="button"
                aria-label={h.isPlaying ? "Pause" : "Play"}
                onClick={h.togglePlay}
                className="glow grid size-16 place-items-center rounded-full sunset-gradient text-black"
              >
                {h.isPlaying ? (
                  <Pause className="size-7 fill-current" />
                ) : (
                  <Play className="size-7 fill-current" />
                )}
              </button>
              <button type="button" aria-label="Next" onClick={h.next}>
                <SkipForward className="size-7 fill-current" />
              </button>
              <button
                type="button"
                aria-label="Repeat"
                onClick={h.cycleRepeat}
                className={h.repeat === "off" ? "text-muted-foreground" : "text-primary"}
              >
                {h.repeat === "one" ? <Repeat1 className="size-5" /> : <Repeat className="size-5" />}
              </button>
            </div>

            <Tabs defaultValue="up-next" className="mt-8">
              <TabsList className="w-full">
                <TabsTrigger value="up-next" className="flex-1">
                  Up next
                </TabsTrigger>
                <TabsTrigger value="lyrics" className="flex-1">
                  Lyrics
                </TabsTrigger>
              </TabsList>
              <TabsContent value="up-next" className="mt-3 space-y-1">
                <QueueList />
              </TabsContent>
              <TabsContent value="lyrics" className="mt-3">
                <p className="rounded-2xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
                  Lyrics will appear here once the backend is connected.
                </p>
              </TabsContent>
            </Tabs>
          </div>
        </div>
      )}

      <Sheet open={queueOpen} onOpenChange={setQueueOpen}>
        <SheetContent side="bottom" className="max-h-[70vh] overflow-y-auto">
          <SheetHeader>
            <SheetTitle>Queue</SheetTitle>
          </SheetHeader>
          <div className="space-y-1 p-4 pt-0">
            <QueueList removable />
          </div>
        </SheetContent>
      </Sheet>
    </>
  );
}

function QueueList({ removable }: { removable?: boolean }) {
  const h = useHarmix();
  const [dragIndex, setDragIndex] = useState<number | null>(null);

  return (
    <div className="space-y-1">
      {h.queue.map((id, i) => {
        const track = h.trackById(id);
        if (!track) return null;
        return (
          <div
            key={`${id}-${i}`}
            draggable
            onDragStart={() => setDragIndex(i)}
            onDragOver={(e) => e.preventDefault()}
            onDrop={() => {
              if (dragIndex !== null && dragIndex !== i) h.moveInQueue(dragIndex, i);
              setDragIndex(null);
            }}
            className={cn(
              "flex items-center gap-3 rounded-xl px-2 py-2",
              i === h.currentIndex && "bg-secondary/70",
            )}
          >
            <span
              className="size-9 shrink-0 rounded-md border border-border/60"
              style={{ backgroundImage: track.art }}
            />
            <span className="min-w-0 flex-1">
              <span
                className={cn(
                  "block truncate text-sm",
                  i === h.currentIndex ? "text-primary" : "text-foreground",
                )}
              >
                {track.title}
              </span>
              <span className="block truncate text-xs text-muted-foreground">{track.artist}</span>
            </span>
            {removable && (
              <button
                type="button"
                aria-label={`Remove ${track.title} from queue`}
                onClick={() => h.removeFromQueue(i)}
                className="text-muted-foreground"
              >
                <X className="size-4" />
              </button>
            )}
          </div>
        );
      })}
      {h.queue.length === 0 && (
        <p className="py-6 text-center text-sm text-muted-foreground">Queue is empty.</p>
      )}
    </div>
  );
}