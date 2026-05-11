import { env } from "@/env";

export const getBaseURL = () => {
   const url = env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1/";
   return url.endsWith("/api/v1/") ? url : `${url.replace(/\/$/, "")}/api/v1/`;
};
