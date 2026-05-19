import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";
import path from "path";

const withNextIntl = createNextIntlPlugin("./src/i18n.ts");

const nextConfig: NextConfig = {
   turbopack: {
      root: path.join(__dirname, ".."),
   },
   images: {
      remotePatterns: [
         {
            protocol: "https",
            hostname: "images.unsplash.com",
         },
         {
            protocol: "https",
            hostname: "flagcdn.com",
         },
         {
            protocol: "https",
            hostname: "i.pravatar.cc",
         },
         {
            protocol: "https",
            hostname: "res.cloudinary.com",
         },
         {
            protocol: "https",
            hostname: "upload.wikimedia.org",
         },
      ],
   },
};

export default withNextIntl(nextConfig);
