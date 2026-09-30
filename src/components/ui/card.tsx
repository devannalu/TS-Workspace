import type { ComponentProps } from "react";

export function Card({ className = "", ...props }: ComponentProps<"section">) {
  return <section className={`rounded-3xl border border-border bg-card ${className}`} {...props} />;
}
