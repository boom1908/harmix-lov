import { Link, useRouterState } from "@tanstack/react-router";
import { Home, Library, Search, User } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";
import { Player } from "./Player";

const NAV = [
  { to: "/", label: "Home", icon: Home },
  { to: "/search", label: "Search", icon: Search },
  { to: "/library", label: "Library", icon: Library },
  { to: "/profile", label: "Profile", icon: User },
] as const;

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const bare = pathname.startsWith("/login");

  if (bare) return <>{children}</>;

  return (
    <div className="min-h-screen bg-background pb-40">
      {children}
      <Player />
      <nav className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-card/95 backdrop-blur">
        <ul className="mx-auto flex max-w-lg">
          {NAV.map(({ to, label, icon: Icon }) => {
            const active = to === "/" ? pathname === "/" : pathname.startsWith(to);
            return (
              <li key={to} className="flex-1">
                <Link
                  to={to}
                  className={cn(
                    "flex flex-col items-center gap-1 py-2.5 text-[11px] font-medium transition-colors",
                    active ? "text-primary" : "text-muted-foreground",
                  )}
                >
                  <Icon className={cn("size-5", active && "drop-shadow-[0_0_8px_var(--gold)]")} />
                  {label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
    </div>
  );
}

export function PageHeader({
  title,
  subtitle,
  action,
}: {
  title: string;
  subtitle?: string;
  action?: ReactNode;
}) {
  return (
    <header className="flex items-end justify-between px-4 pb-4 pt-6">
      <div>
        {subtitle && (
          <p className="text-xs uppercase tracking-[0.22em] text-muted-foreground">{subtitle}</p>
        )}
        <h1 className="font-display text-3xl tracking-wide gold-text">{title}</h1>
      </div>
      {action}
    </header>
  );
}