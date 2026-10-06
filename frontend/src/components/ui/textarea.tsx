import type { ComponentProps } from "react";

export function Textarea({
  className = "",
  ...props
}: ComponentProps<"textarea">) {
  return (
    <textarea
      className={`min-h-28 w-full resize-y rounded-xl border border-border-strong bg-card px-3 py-2.5 text-sm text-foreground placeholder:text-muted-foreground disabled:opacity-60 ${className}`}
      {...props}
    />
  );
}
