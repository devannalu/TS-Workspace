"use client";
import { Button } from "./button";
export function Pagination({
  page,
  size,
  total,
  onPage,
  label = "registros",
}: {
  page: number;
  size: number;
  total: number;
  onPage: (page: number) => void;
  label?: string;
}) {
  const pages = Math.max(1, Math.ceil(total / size));
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border pt-4 text-xs text-muted-foreground">
      <p>
        {total} {label} · Página {page + 1} de {pages}
      </p>
      <div className="flex gap-2">
        <Button
          variant="secondary"
          disabled={page === 0}
          onClick={() => onPage(page - 1)}
        >
          Anterior
        </Button>
        <Button
          variant="secondary"
          disabled={page + 1 >= pages}
          onClick={() => onPage(page + 1)}
        >
          Próxima
        </Button>
      </div>
    </div>
  );
}
