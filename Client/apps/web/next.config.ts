import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";
import path from "path";
import fs from "fs";
import { withSentryConfig } from "@sentry/nextjs";

// Load environment variables from root .env
function loadRootEnv() {
   try {
      const envPath = path.resolve(__dirname, "../../../.env");
      if (fs.existsSync(envPath)) {
         console.log("Loading root .env from:", envPath);
         const envContent = fs.readFileSync(envPath, "utf8");
         envContent.split("\n").forEach((line) => {
            const trimmed = line.trim();
            if (!trimmed || trimmed.startsWith("#")) return;
            const parts = trimmed.split("=");
            const key = parts[0].trim();
            let value = parts.slice(1).join("=").trim();
            if (value.startsWith('"') && value.endsWith('"')) value = value.slice(1, -1);
            if (value.startsWith("'") && value.endsWith("'")) value = value.slice(1, -1);

            if (key === "NODE_ENV") return;
            if (key && !(key in process.env)) {
               process.env[key] = value;
            }
         });
      } else {
         console.warn("Root .env file not found at:", envPath);
      }
   } catch (error) {
      console.warn("Could not load root .env file:", error);
   }
}

loadRootEnv();

const withNextIntl = createNextIntlPlugin("./src/i18n.ts");

// Calculate allowedDevOrigins dynamically
const webUrl = process.env.NEXT_PUBLIC_WEB_URL || "https://zeion.online";
const partnerUrl = process.env.NEXT_PUBLIC_PARTNER_URL || "https://partner.zeion.online";
const getHostname = (url: string) => {
   try {
      return new URL(url).hostname;
   } catch {
      return url
         .replace(/https?:\/\//, "")
         .split("/")[0]
         .split(":")[0];
   }
};
const webHost = getHostname(webUrl);
const partnerHost = getHostname(partnerUrl);
const rootDomain = webHost.split(".").slice(-2).join(".");

const nextConfig: NextConfig = {
   transpilePackages: ["@omnibooking/shared"],
   allowedDevOrigins: [webHost, partnerHost, `*.${rootDomain}`],
   env: {
      NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL,
      NEXT_PUBLIC_WEB_URL: process.env.NEXT_PUBLIC_WEB_URL,
      NEXT_PUBLIC_PARTNER_URL: process.env.NEXT_PUBLIC_PARTNER_URL,
      NEXT_PUBLIC_VIETMAP_API_KEY: process.env.NEXT_PUBLIC_VIETMAP_API_KEY,
      NEXT_PUBLIC_VIETMAP_TILE_KEY: process.env.NEXT_PUBLIC_VIETMAP_TILE_KEY,
      NEXT_PUBLIC_GOONG_API_KEY: process.env.NEXT_PUBLIC_GOONG_API_KEY,
      NEXT_PUBLIC_GOONG_MAPTILES_KEY: process.env.NEXT_PUBLIC_GOONG_MAPTILES_KEY,
      NEXT_PUBLIC_TURNSTILE_SITE_KEY: process.env.NEXT_PUBLIC_TURNSTILE_SITE_KEY,
      JWT_SECRET: process.env.JWT_SECRET,
   },
   async rewrites() {
      return [
         {
            source: "/api/v1/:path*",
            destination: "http://127.0.0.1:8080/api/v1/:path*",
         },
      ];
   },
   turbopack: {
      root: path.join(__dirname, "../.."),
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
