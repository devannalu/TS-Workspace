import type { ComponentProps } from "react";
export function Badge({
  className = "",
  tone = "neutral",
  ...props
}: ComponentProps<"span"> & {
  tone?: "neutral" | "pink" | "success" | "danger" | "warning";
}) {
  const styles = {
    neutral: "bg-muted text-muted-foreground",
    pink: "bg-accent text-accent-foreground",
    success: "bg-success-soft text-success",
    danger: "bg-danger-soft text-danger",
    warning: "bg-warning-soft text-warning",
  };
  return (
    <span
      className={`inline-flex items-center rounded-md px-2.5 py-1 text-xs font-medium ${styles[tone]} ${className}`}
      {...props}
    />
  );
}
