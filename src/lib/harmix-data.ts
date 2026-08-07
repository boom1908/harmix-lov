export type Track = {
  id: string;
  title: string;
  artist: string;
  album: string;
  duration: number; // seconds
  art: string; // css gradient
  liked?: boolean;
  downloaded?: boolean;
};

export type Playlist = {
  id: string;
  title: string;
  description: string;
  art: string;
  trackIds: string[];
  source: "local" | "ytmusic";
};

const g = (a: string, b: string) => `linear-gradient(135deg, ${a}, ${b})`;

export const ART = {
  ember: g("oklch(0.62 0.2 32)", "oklch(0.83 0.15 82)"),
  dusk: g("oklch(0.35 0.12 20)", "oklch(0.72 0.16 66)"),
  amber: g("oklch(0.78 0.16 70)", "oklch(0.28 0.05 50)"),
  night: g("oklch(0.22 0.04 60)", "oklch(0.6 0.14 48)"),
  copper: g("oklch(0.5 0.14 40)", "oklch(0.9 0.09 88)"),
  smoke: g("oklch(0.3 0.02 70)", "oklch(0.68 0.1 76)"),
  wine: g("oklch(0.32 0.14 15)", "oklch(0.75 0.14 60)"),
  sand: g("oklch(0.86 0.1 84)", "oklch(0.45 0.1 44)"),
};

const artList = Object.values(ART);
export const artFor = (seed: string) =>
  artList[[...seed].reduce((a, c) => a + c.charCodeAt(0), 0) % artList.length]!;

const t = (
  id: string,
  title: string,
  artist: string,
  album: string,
  duration: number,
  extra: Partial<Track> = {},
): Track => ({ id, title, artist, album, duration, art: artFor(id + title), ...extra });

export const TRACKS: Track[] = [
  t("t1", "Golden Hour", "Nova Vale", "Sunset Tapes", 214, { liked: true }),
  t("t2", "Ember Skies", "Kaito Rein", "Afterglow", 187, { downloaded: true }),
  t("t3", "Midnight Copper", "Lila Sun", "Brass & Dust", 243, { liked: true }),
  t("t4", "Neon Dunes", "Aster Wolf", "Mirage", 198),
  t("t5", "Slow Burn", "The Amber Room", "Low Light", 265, { liked: true, downloaded: true }),
  t("t6", "Paper Lanterns", "Miya Fields", "Drift", 176),
  t("t7", "Velvet Static", "Nova Vale", "Sunset Tapes", 221),
  t("t8", "Dust & Gold", "Kaito Rein", "Afterglow", 202, { liked: true }),
  t("t9", "Last Light", "Solene", "Horizon Line", 254, { downloaded: true }),
  t("t10", "Cassette Sun", "Aster Wolf", "Mirage", 189),
  t("t11", "Low Orbit", "The Amber Room", "Low Light", 231),
  t("t12", "Sundial", "Solene", "Horizon Line", 208, { liked: true }),
  t("t13", "Chrome Desert", "Miya Fields", "Drift", 245),
  t("t14", "Afterglow", "Lila Sun", "Brass & Dust", 196, { downloaded: true }),
  t("t15", "Harbour Lights", "Solene", "Horizon Line", 227),
  t("t16", "Molten", "Kaito Rein", "Afterglow", 179),
];

export const PLAYLISTS: Playlist[] = [
  {
    id: "p1",
    title: "Sunset Drive",
    description: "Warm synths for the long way home.",
    art: ART.ember,
    trackIds: ["t1", "t4", "t7", "t10", "t13", "t2"],
    source: "local",
  },
  {
    id: "p2",
    title: "Liked from YT Music",
    description: "Synced from your YouTube Music likes.",
    art: ART.dusk,
    trackIds: ["t3", "t5", "t8", "t12", "t1"],
    source: "ytmusic",
  },
  {
    id: "p3",
    title: "Late Night Focus",
    description: "Low light, low tempo, no vocals in the way.",
    art: ART.night,
    trackIds: ["t9", "t11", "t6", "t15"],
    source: "local",
  },
  {
    id: "p4",
    title: "Brass & Dust",
    description: "Horns, tape hiss and desert reverb.",
    art: ART.copper,
    trackIds: ["t14", "t3", "t16", "t13", "t5"],
    source: "ytmusic",
  },
];

export type Album = { id: string; title: string; artist: string; art: string; year: number };
export const ALBUMS: Album[] = [
  { id: "a1", title: "Sunset Tapes", artist: "Nova Vale", art: ART.ember, year: 2024 },
  { id: "a2", title: "Afterglow", artist: "Kaito Rein", art: ART.amber, year: 2023 },
  { id: "a3", title: "Brass & Dust", artist: "Lila Sun", art: ART.copper, year: 2025 },
  { id: "a4", title: "Mirage", artist: "Aster Wolf", art: ART.sand, year: 2022 },
  { id: "a5", title: "Low Light", artist: "The Amber Room", art: ART.smoke, year: 2024 },
  { id: "a6", title: "Horizon Line", artist: "Solene", art: ART.wine, year: 2025 },
];

export type Artist = { id: string; name: string; art: string; listeners: string };
export const ARTISTS: Artist[] = [
  { id: "ar1", name: "Nova Vale", art: ART.ember, listeners: "1.2M" },
  { id: "ar2", name: "Kaito Rein", art: ART.amber, listeners: "840K" },
  { id: "ar3", name: "Lila Sun", art: ART.copper, listeners: "2.4M" },
  { id: "ar4", name: "Aster Wolf", art: ART.sand, listeners: "610K" },
  { id: "ar5", name: "Solene", art: ART.wine, listeners: "1.8M" },
  { id: "ar6", name: "The Amber Room", art: ART.smoke, listeners: "430K" },
];

export const MOODS = [
  "Sunset",
  "Focus",
  "Workout",
  "Chill",
  "Drive",
  "Lo-fi",
  "Indie",
  "Bollywood",
  "Hip-hop",
  "Classics",
];

export function formatTime(seconds: number) {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, "0")}`;
}