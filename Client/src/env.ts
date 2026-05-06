import { z } from "zod";

const envSchema = z.object({
   NEXT_PUBLIC_API_URL: z.string().url().default("http://localhost:8080/api/v1"),
   NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
});

const _env = envSchema.safeParse({
   NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL,
   NODE_ENV: process.env.NODE_ENV,
});

if (!_env.success) {
   console.error("Invalid environment variables:", _env.error.format());
   throw new Error("Invalid environment variables");
}

export const env = _env.data;
