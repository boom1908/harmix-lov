import { createFileRoute, Link } from "@tanstack/react-router";
import { LogOut, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { PageHeader } from "@/components/harmix/AppShell";
import { ARTISTS } from "@/lib/harmix-data";
import { useHarmix } from "@/lib/harmix-store";

export const Route = createFileRoute("/profile")({
  head: () => ({
    meta: [
      { title: "Profile & History — Harmix" },
      {
        name: "description",
        content: "Your listening history, top artists, connected accounts and Harmix settings.",
      },
      { property: "og:title", content: "Profile & History — Harmix" },
      { property: "og:description", content: "Listening history, top artists and settings." },
    ],
  }),
  component: ProfilePage,
});

function ProfilePage() {
  const h = useHarmix();
  const recent = h.listening.map((l) => h.trackById(l.id)).filter(Boolean);

  return (
    <div className="space-y-6 pb-6">
      <PageHeader title="Profile" subtitle="Harmix" />

      <section className="mx-4 flex items-center gap-4 rounded-2xl border border-border bg-card p-4">
        <div className="grid size-14 place-items-center rounded-full sunset-gradient text-xl font-bold text-black">
          {h.account ? h.account.name.charAt(0) : "H"}
        </div>
        <div className="min-w-0 flex-1">
          <p className="truncate font-semibold">{h.account?.name ?? "Not signed in"}</p>
          <p className="truncate text-xs text-muted-foreground">
            {h.account?.email ?? "Connect Google to sync YT Music"}
          </p>
        </div>
        {h.account ? (
          <Button variant="outline" size="sm" onClick={h.signOut}>
            <LogOut className="size-4" /> Sign out
          </Button>
        ) : (
          <Button asChild size="sm">
            <Link to="/login">Sign in</Link>
          </Button>
        )}
      </section>

      <section className="mx-4 grid grid-cols-3 gap-3">
        {[
          { label: "Minutes", value: "4,382" },
          { label: "Songs", value: String(h.tracks.length) },
          { label: "Playlists", value: String(h.playlists.length) },
        ].map((s) => (
          <div key={s.label} className="rounded-2xl border border-border p-3 text-center">
            <p className="font-display text-2xl gold-text">{s.value}</p>
            <p className="text-xs text-muted-foreground">{s.label}</p>
          </div>
        ))}
      </section>

      <section className="px-4">
        <h2 className="mb-3 font-display text-xl tracking-wide">Top artists</h2>
        <div className="no-scrollbar flex gap-4 overflow-x-auto">
          {ARTISTS.slice(0, 5).map((a) => (
            <div key={a.id} className="w-20 shrink-0 text-center">
              <div className="size-20 rounded-full border border-border/60" style={{ backgroundImage: a.art }} role="img" aria-label={a.name} />
              <p className="mt-1 truncate text-xs">{a.name}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="px-4">
        <h2 className="mb-3 font-display text-xl tracking-wide">Listening history</h2>
        {recent.length ? (
          <ul className="space-y-1">
            {recent.map(
              (t) =>
                t && (
                  <li key={t.id} className="flex items-center gap-3 rounded-xl px-2 py-2">
                    <span className="size-10 rounded-lg border border-border/60" style={{ backgroundImage: t.art }} />
                    <span className="min-w-0">
                      <span className="block truncate text-sm">{t.title}</span>
                      <span className="block truncate text-xs text-muted-foreground">{t.artist}</span>
                    </span>
                  </li>
                ),
            )}
          </ul>
        ) : (
          <p className="rounded-2xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
            Play something and it will show up here.
          </p>
        )}
      </section>

      <section className="mx-4 space-y-3 rounded-2xl border border-border p-4">
        <h2 className="font-display text-xl tracking-wide">Settings</h2>
        <Row label="High quality audio" />
        <Row label="Download over Wi-Fi only" defaultOn />
        <Row label="Personalized recommendations" defaultOn />
        <div className="flex items-center justify-between pt-1">
          <span className="text-sm">YouTube Music sync</span>
          <Button variant="outline" size="sm" disabled={!h.account}>
            <RefreshCw className="size-4" /> Sync now
          </Button>
        </div>
      </section>
    </div>
  );
}

function Row({ label, defaultOn }: { label: string; defaultOn?: boolean }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-sm">{label}</span>
      <Switch defaultChecked={defaultOn ?? false} aria-label={label} />
    </div>
  );
}