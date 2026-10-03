import type { ComponentProps } from "react";

export function Input({ className = "", ...props }: ComponentProps<"input">) {
  return (
    <input
      className={`min-h-11 w-full rounded-xl border border-border-strong bg-card px-3 py-2.5 text-sm text-foreground placeholder:text-muted-foreground disabled:opacity-60 ${className}`}
      {...props}
    />
  );
}
