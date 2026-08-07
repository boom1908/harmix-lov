import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";
import { useHarmix } from "@/lib/harmix-store";

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Sign in — Harmix" },
      {
        name: "description",
        content: "Sign in with Google to sync your YouTube Music playlists into Harmix.",
      },
      { property: "og:title", content: "Sign in — Harmix" },
      { property: "og:description", content: "Connect Google to sync YouTube Music playlists." },
    ],
  }),
  component: LoginPage,
});

function LoginPage() {
  const h = useHarmix();
  const navigate = useNavigate();

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6 px-6 text-center">
      <div className="glow size-24 rounded-3xl sunset-gradient" aria-hidden />
      <div>
        <h1 className="font-display text-5xl tracking-widest gold-text">HARMIX</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Sunset-gold streaming. Sign in with Google to bring your YouTube Music playlists along.
        </p>
      </div>
      <div className="w-full max-w-sm space-y-3">
        <Button
          className="w-full rounded-full"
          onClick={() => {
            h.signIn();
            navigate({ to: "/" });
          }}
        >
          Continue with Google
        </Button>
        <Button variant="outline" className="w-full rounded-full" onClick={() => navigate({ to: "/" })}>
          Continue as guest
        </Button>
      </div>
      <p className="max-w-sm text-xs text-muted-foreground">
        Playlist sync uses the official YouTube Data API once the backend is connected.
      </p>
    </div>
  );
}