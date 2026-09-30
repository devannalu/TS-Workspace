import type { ComponentProps } from "react";

export function Button({ className = "", type = "button", ...props }: ComponentProps<"button">) {
  return <button type={type} className={`inline-flex min-h-11 items-center justify-center rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:cursor-wait disabled:opacity-60 ${className}`} {...props} />;
}
