import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "TrafficLab",
  description: "동시성 제어 전략을 실시간으로 비교하는 부하 실험 플랫폼"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
