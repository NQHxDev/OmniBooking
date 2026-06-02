import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export async function GET(request: NextRequest) {
   const forwarded = request.headers.get("x-forwarded-for");
   const realIp = request.headers.get("x-real-ip");
   const clientIp = request.headers.get("x-client-ip");
   const ip = forwarded ? forwarded.split(/, /)[0] : realIp || clientIp || "";

   // On local development, IP will be ::1 or 127.0.0.1.
   // If it is localhost, we query without IP to let the API geolocate the server's public IP (which matches developer's public IP).
   const isLocalIp =
      !ip ||
      ip === "::1" ||
      ip === "127.0.0.1" ||
      ip === "localhost" ||
      ip.startsWith("192.168.") ||
      ip.startsWith("10.");
   const queryIp = isLocalIp ? "" : ip;

   const queryUrl = queryIp ? `https://ipapi.co/${queryIp}/json/` : "https://ipapi.co/json/";
   const fallbackUrl = queryIp
      ? `https://freeipapi.com/api/json/${queryIp}`
      : "https://freeipapi.com/api/json";

   try {
      console.log(
         `[Server Geolocation] Trying primary API (ipapi.co) for IP: ${queryIp || "server-ip"}`
      );
      const res = await fetch(queryUrl, { signal: AbortSignal.timeout(5000) });
      if (res.ok) {
         const data = await res.json();
         if (data && data.country) {
            return NextResponse.json({
               countryCode: data.country.toUpperCase(),
               countryName: data.country_name,
               latitude: data.latitude,
               longitude: data.longitude,
            });
         }
      }
   } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.warn(
         `[Server Geolocation] Primary API failed: ${errorMessage}. Trying fallback API...`
      );
   }

   try {
      console.log(
         `[Server Geolocation] Trying fallback API (freeipapi.com) for IP: ${queryIp || "server-ip"}`
      );
      const res = await fetch(fallbackUrl, { signal: AbortSignal.timeout(5000) });
      if (res.ok) {
         const data = await res.json();
         if (data && data.countryCode) {
            return NextResponse.json({
               countryCode: data.countryCode.toUpperCase(),
               countryName: data.countryName,
               latitude: data.latitude,
               longitude: data.longitude,
            });
         }
      }
   } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error(`[Server Geolocation] Fallback API failed: ${errorMessage}`);
   }

   // Return fallback values for Vietnam if everything fails
   return NextResponse.json({
      countryCode: "VN",
      countryName: "Việt Nam",
      latitude: 14.058324,
      longitude: 108.277199,
   });
}
