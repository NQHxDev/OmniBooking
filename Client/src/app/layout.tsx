import type { Metadata } from "next";
import { Be_Vietnam_Pro } from "next/font/google";
import "./globals.css";

const beVietnamPro = Be_Vietnam_Pro({
   variable: "--font-be-vietnam-pro",
   subsets: ["latin", "vietnamese"],
   weight: ["400", "500", "600", "700", "800", "900"],
});

export const metadata: Metadata = {
   title: "OmniBooking.com | Official Site | The best hotels & accommodations",
   description:
      "Book hotels, homes and much more on OmniBooking.com, the world's leading booking platform.",
};

import QueryProvider from "@/providers/QueryProvider";
import { Toaster } from "@/components/ui/sonner";

export default function RootLayout({
   children,
}: Readonly<{
   children: React.ReactNode;
}>) {
   return (
      <html lang="vi" className={`${beVietnamPro.variable} h-full antialiased`}>
         <body className="min-h-full flex flex-col font-sans">
            <QueryProvider>
               {children}
               <Toaster position="bottom-right" expand={true} richColors />
            </QueryProvider>
         </body>
      </html>
   );
}
