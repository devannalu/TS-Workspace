import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "TS Workspace", template: "%s | TS Workspace" },
  description: "Workspace interno da Tech Sisters.",
  robots: { index: false, follow: false },
};

export const viewport: Viewport = { themeColor: "#f7f5f3" };

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="pt-BR"><body>{children}</body></html>;
}
