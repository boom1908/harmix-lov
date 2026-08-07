import { Link } from "@tanstack/react-router";
import { Play } from "lucide-react";
import { cn } from "@/lib/utils";

type Props = {
  title: string;
  subtitle?: string;
  art: string;
  round?: boolean;
  className?: string;
  to?: string;
  params?: Record<string, string>;
  onPlay?: () => void;
};

export function MediaCard({ title, subtitle, art, round, className, to, params, onPlay }: Props) {
  const body = (
    <div className={cn("group w-36 shrink-0 sm:w-40", className)}>
      <div className="relative">
        <div
          className={cn(
            "aspect-square w-full overflow-hidden border border-border/60 shadow-lg",
            round ? "rounded-full" : "rounded-2xl",
          )}
          style={{ backgroundImage: art }}
          role="img"
          aria-label={title}
        />
        {onPlay && (
          <button
            type="button"
            aria-label={`Play ${title}`}
            onClick={(e) => {
              e.preventDefault();
              onPlay();
            }}
            className="absolute bottom-2 right-2 grid size-10 place-items-center rounded-full bg-primary text-primary-foreground opacity-0 shadow-lg transition group-hover:opacity-100 focus-visible:opacity-100"
          >
            <Play className="size-4 fill-current" />
          </button>
        )}
      </div>
      <p className="mt-2 truncate text-sm font-semibold text-foreground">{title}</p>
      {subtitle && <p className="truncate text-xs text-muted-foreground">{subtitle}</p>}
    </div>
  );

  if (to) {
    return (
      <Link to={to} params={params as never} className="block">
        {body}
      </Link>
    );
  }
  return body;
}

export function Shelf({
  title,
  action,
  children,
}: {
  title: string;
  action?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <section className="space-y-3">
      <div className="flex items-end justify-between px-4">
        <h2 className="font-display text-xl tracking-wide text-foreground">{title}</h2>
        {action}
      </div>
      <div className="no-scrollbar flex gap-3 overflow-x-auto px-4 pb-1">{children}</div>
    </section>
  );
}