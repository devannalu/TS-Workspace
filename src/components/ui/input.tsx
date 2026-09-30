import type { ComponentProps } from "react";

export function Input({ className = "", ...props }: ComponentProps<"input">) {
  return <input className={`min-h-12 w-full rounded-xl border border-border bg-card px-4 py-3 text-base text-foreground placeholder:text-muted-foreground disabled:opacity-60 ${className}`} {...props} />;
}
