import { z } from "zod";

const envSchema = z.object({
   NEXT_PUBLIC_API_URL: z.string().url().default("http://localhost:8080/api/v1/"),
   NEXT_PUBLIC_WEB_URL: z.string().url().default("http://localhost:3000"),
   NEXT_PUBLIC_PARTNER_URL: z.string().url().default("http://localhost:3002"),
   NEXT_PUBLIC_OWNER_URL: z.string().url().default("http://localhost:3005"),
   NEXT_PUBLIC_VIETMAP_API_KEY: z.string().default(""),
   NEXT_PUBLIC_VIETMAP_TILE_KEY: z.string().default(""),
   NEXT_PUBLIC_GOONG_API_KEY: z.string().default(""),
   NEXT_PUBLIC_GOONG_MAPTILES_KEY: z.string().default(""),
   NEXT_PUBLIC_TURNSTILE_SITE_KEY: z.string().default("1x00000000000000000000AA"),
   NEXT_PUBLIC_SENTRY_DSN: z.string().optional().or(z.literal("")),
   NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
});

const _env = envSchema.safeParse({
   NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL,
   NEXT_PUBLIC_WEB_URL: process.env.NEXT_PUBLIC_WEB_URL,
   NEXT_PUBLIC_PARTNER_URL: process.env.NEXT_PUBLIC_PARTNER_URL,
   NEXT_PUBLIC_OWNER_URL: process.env.NEXT_PUBLIC_OWNER_URL,
   NEXT_PUBLIC_VIETMAP_API_KEY:
      process.env.NEXT_PUBLIC_VIETMAP_API_KEY || process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY,
   NEXT_PUBLIC_VIETMAP_TILE_KEY: process.env.NEXT_PUBLIC_VIETMAP_TILE_KEY,
   NEXT_PUBLIC_GOONG_API_KEY: process.env.NEXT_PUBLIC_GOONG_API_KEY,
   NEXT_PUBLIC_GOONG_MAPTILES_KEY: process.env.NEXT_PUBLIC_GOONG_MAPTILES_KEY,
   NEXT_PUBLIC_TURNSTILE_SITE_KEY: process.env.NEXT_PUBLIC_TURNSTILE_SITE_KEY,
   NEXT_PUBLIC_SENTRY_DSN: process.env.NEXT_PUBLIC_SENTRY_DSN,
   NODE_ENV: process.env.NODE_ENV,
});

if (!_env.success) {
   console.error("Invalid environment variables in Owner app:", _env.error.format());
   throw new Error("Invalid environment variables");
}

export const env = _env.data;
