import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";
import path from "path";
import { withSentryConfig } from "@sentry/nextjs";

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

const sentryConfig = {
   silent: true,
   org: "omnibooking",
   project: "omnibooking-client",
   widenClientFileUpload: true,
   tunnelRoute: "/monitoring",
   hideSourceMaps: true,
   disableLogger: true,
};

export default withSentryConfig(withNextIntl(nextConfig), sentryConfig);
