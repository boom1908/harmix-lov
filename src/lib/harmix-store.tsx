import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { PLAYLISTS, TRACKS, type Playlist, type Track } from "./harmix-data";

type PersistShape = {
  playlists: Playlist[];
  liked: string[];
  downloaded: string[];
  searchHistory: string[];
  listening: { id: string; at: number }[];
  account: { name: string; email: string } | null;
};

const KEY = "harmix-state-v1";

type Ctx = {
  tracks: Track[];
  trackById: (id: string) => Track | undefined;
  playlists: Playlist[];
  liked: string[];
  downloaded: string[];
  searchHistory: string[];
  listening: { id: string; at: number }[];
  account: { name: string; email: string } | null;
  // player
  queue: string[];
  currentIndex: number;
  current?: Track;
  isPlaying: boolean;
  progress: number;
  shuffle: boolean;
  repeat: "off" | "all" | "one";
  expanded: boolean;
  setExpanded: (v: boolean) => void;
  playTracks: (ids: string[], startIndex?: number, shuffled?: boolean) => void;
  togglePlay: () => void;
  next: () => void;
  prev: () => void;
  seek: (v: number) => void;
  toggleShuffle: () => void;
  cycleRepeat: () => void;
  playNext: (id: string) => void;
  addToQueue: (id: string) => void;
  removeFromQueue: (index: number) => void;
  moveInQueue: (from: number, to: number) => void;
  // library
  toggleLike: (id: string) => void;
  toggleDownload: (id: string) => void;
  createPlaylist: (title: string, description?: string) => string;
  updatePlaylist: (id: string, patch: Partial<Pick<Playlist, "title" | "description" | "art">>) => void;
  deletePlaylist: (id: string) => void;
  addToPlaylist: (playlistId: string, trackIds: string[]) => void;
  removeFromPlaylist: (playlistId: string, trackId: string) => void;
  reorderPlaylist: (playlistId: string, from: number, to: number) => void;
  // search / account
  pushSearch: (q: string) => void;
  removeSearch: (q: string) => void;
  clearSearch: () => void;
  signIn: () => void;
  signOut: () => void;
};

const HarmixContext = createContext<Ctx | null>(null);

function move<T>(arr: T[], from: number, to: number) {
  const copy = arr.slice();
  const [item] = copy.splice(from, 1);
  if (item === undefined) return arr;
  copy.splice(to, 0, item);
  return copy;
}

export function HarmixProvider({ children }: { children: ReactNode }) {
  const [playlists, setPlaylists] = useState<Playlist[]>(PLAYLISTS);
  const [liked, setLiked] = useState<string[]>(() =>
    TRACKS.filter((t) => t.liked).map((t) => t.id),
  );
  const [downloaded, setDownloaded] = useState<string[]>(() =>
    TRACKS.filter((t) => t.downloaded).map((t) => t.id),
  );
  const [searchHistory, setSearchHistory] = useState<string[]>([
    "nova vale",
    "sunset drive mix",
    "lo-fi guitar",
  ]);
  const [listening, setListening] = useState<{ id: string; at: number }[]>([]);
  const [account, setAccount] = useState<{ name: string; email: string } | null>(null);

  const [queue, setQueue] = useState<string[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const [shuffle, setShuffle] = useState(false);
  const [repeat, setRepeat] = useState<"off" | "all" | "one">("off");
  const [expanded, setExpanded] = useState(false);
  const hydrated = useRef(false);

  // hydrate
  useEffect(() => {
    try {
      const raw = localStorage.getItem(KEY);
      if (raw) {
        const s = JSON.parse(raw) as Partial<PersistShape>;
        if (s.playlists) setPlaylists(s.playlists);
        if (s.liked) setLiked(s.liked);
        if (s.downloaded) setDownloaded(s.downloaded);
        if (s.searchHistory) setSearchHistory(s.searchHistory);
        if (s.listening) setListening(s.listening);
        if (s.account !== undefined) setAccount(s.account);
      }
    } catch {
      /* ignore */
    }
    hydrated.current = true;
  }, []);

  useEffect(() => {
    if (!hydrated.current) return;
    const payload: PersistShape = {
      playlists,
      liked,
      downloaded,
      searchHistory,
      listening,
      account,
    };
    try {
      localStorage.setItem(KEY, JSON.stringify(payload));
    } catch {
      /* ignore */
    }
  }, [playlists, liked, downloaded, searchHistory, listening, account]);

  const trackById = useCallback((id: string) => TRACKS.find((t) => t.id === id), []);
  const current = queue[currentIndex] ? trackById(queue[currentIndex]!) : undefined;

  const next = useCallback(() => {
    setProgress(0);
    setCurrentIndex((i) => {
      if (repeat === "one") return i;
      if (i + 1 < queue.length) return i + 1;
      return repeat === "all" ? 0 : i;
    });
  }, [queue.length, repeat]);

  // simulated playback clock
  useEffect(() => {
    if (!isPlaying || !current) return;
    const id = setInterval(() => {
      setProgress((p) => {
        if (p + 1 >= current.duration) {
          next();
          return 0;
        }
        return p + 1;
      });
    }, 1000);
    return () => clearInterval(id);
  }, [isPlaying, current, next]);

  const playTracks = useCallback(
    (ids: string[], startIndex = 0, shuffled = false) => {
      if (!ids.length) return;
      const order = shuffled ? [...ids].sort(() => Math.random() - 0.5) : ids;
      setQueue(order);
      setCurrentIndex(shuffled ? 0 : startIndex);
      setProgress(0);
      setIsPlaying(true);
      if (shuffled) setShuffle(true);
      const id = order[shuffled ? 0 : startIndex];
      if (id) setListening((h) => [{ id, at: Date.now() }, ...h.filter((x) => x.id !== id)].slice(0, 50));
    },
    [],
  );

  const value: Ctx = {
    tracks: TRACKS,
    trackById,
    playlists,
    liked,
    downloaded,
    searchHistory,
    listening,
    account,
    queue,
    currentIndex,
    ...(current ? { current } : {}),
    isPlaying,
    progress,
    shuffle,
    repeat,
    expanded,
    setExpanded,
    playTracks,
    togglePlay: () => setIsPlaying((p) => !p),
    next,
    prev: () => {
      setProgress(0);
      setCurrentIndex((i) => Math.max(0, i - 1));
    },
    seek: setProgress,
    toggleShuffle: () => setShuffle((s) => !s),
    cycleRepeat: () => setRepeat((r) => (r === "off" ? "all" : r === "all" ? "one" : "off")),
    playNext: (id) => setQueue((q) => (q.length ? move([...q, id], q.length, currentIndex + 1) : [id])),
    addToQueue: (id) => setQueue((q) => [...q, id]),
    removeFromQueue: (index) => setQueue((q) => q.filter((_, i) => i !== index)),
    moveInQueue: (from, to) => setQueue((q) => move(q, from, to)),
    toggleLike: (id) => setLiked((l) => (l.includes(id) ? l.filter((x) => x !== id) : [id, ...l])),
    toggleDownload: (id) =>
      setDownloaded((d) => (d.includes(id) ? d.filter((x) => x !== id) : [id, ...d])),
    createPlaylist: (title, description = "") => {
      const id = `p${Date.now()}`;
      setPlaylists((p) => [
        {
          id,
          title,
          description,
          art: "linear-gradient(135deg, oklch(0.62 0.2 32), oklch(0.83 0.15 82))",
          trackIds: [],
          source: "local",
        },
        ...p,
      ]);
      return id;
    },
    updatePlaylist: (id, patch) =>
      setPlaylists((p) => p.map((pl) => (pl.id === id ? { ...pl, ...patch } : pl))),
    deletePlaylist: (id) => setPlaylists((p) => p.filter((pl) => pl.id !== id)),
    addToPlaylist: (playlistId, trackIds) =>
      setPlaylists((p) =>
        p.map((pl) =>
          pl.id === playlistId
            ? { ...pl, trackIds: [...pl.trackIds, ...trackIds.filter((t) => !pl.trackIds.includes(t))] }
            : pl,
        ),
      ),
    removeFromPlaylist: (playlistId, trackId) =>
      setPlaylists((p) =>
        p.map((pl) =>
          pl.id === playlistId ? { ...pl, trackIds: pl.trackIds.filter((t) => t !== trackId) } : pl,
        ),
      ),
    reorderPlaylist: (playlistId, from, to) =>
      setPlaylists((p) =>
        p.map((pl) => (pl.id === playlistId ? { ...pl, trackIds: move(pl.trackIds, from, to) } : pl)),
      ),
    pushSearch: (q) =>
      setSearchHistory((h) => [q, ...h.filter((x) => x.toLowerCase() !== q.toLowerCase())].slice(0, 12)),
    removeSearch: (q) => setSearchHistory((h) => h.filter((x) => x !== q)),
    clearSearch: () => setSearchHistory([]),
    signIn: () => setAccount({ name: "Harmix Listener", email: "you@gmail.com" }),
    signOut: () => setAccount(null),
  };

  return <HarmixContext.Provider value={value}>{children}</HarmixContext.Provider>;
}

export function useHarmix() {
  const ctx = useContext(HarmixContext);
  if (!ctx) throw new Error("useHarmix must be used inside HarmixProvider");
  return ctx;
}

export function useLikedTracks() {
  const { liked, trackById } = useHarmix();
  return useMemo(
    () => liked.map(trackById).filter((t): t is Track => Boolean(t)),
    [liked, trackById],
  );
}