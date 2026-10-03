import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "TS Workspace", template: "%s | TS Workspace" },
  description: "Workspace interno da Tech Sisters.",
  robots: { index: false, follow: false },
};

// Os metadados exigem a cor literal correspondente ao token de fundo.
export const viewport: Viewport = { themeColor: "#faf7f2" };

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  );
}
