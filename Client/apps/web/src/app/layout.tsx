import { Be_Vietnam_Pro } from "next/font/google";
import "./globals.css";
import "leaflet/dist/leaflet.css";

const beVietnamPro = Be_Vietnam_Pro({
   variable: "--font-be-vietnam-pro",
   subsets: ["vietnamese"],
   weight: ["100", "200", "300", "400", "500", "600", "700", "800", "900"],
});

export default function RootLayout({ children }: { children: React.ReactNode }) {
   return (
      <html lang="vi">
         <body className={`${beVietnamPro.variable} antialiased font-sans`}>{children}</body>
      </html>
   );
}
