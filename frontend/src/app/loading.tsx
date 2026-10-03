import { Skeleton } from "@/components/ui/feedback";
export default function Loading() {
  return (
    <main className="mx-auto min-h-dvh max-w-6xl p-6">
      <Skeleton />
    </main>
  );
}
