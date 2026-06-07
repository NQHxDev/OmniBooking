import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { jwtVerify } from "jose";
import { getWebUrl, getPartnerUrl } from "@omnibooking/shared";
import createMiddleware from "next-intl/middleware";
import { routing } from "./i18n/routing";

const intlMiddleware = createMiddleware(routing);

function getBrowserUrl(request: NextRequest): string {
   const host = request.headers.get("x-forwarded-host") || request.headers.get("host") || "";
   const proto = request.headers.get("x-forwarded-proto") || "http";
   return `${proto}://${host}`;
}

function isTokenExpired(token: string): boolean {
   try {
      const parts = token.split(".");
      if (parts.length !== 3) return true;
      const decodedPayload = atob(parts[1].replace(/-/g, "+").replace(/_/g, "/"));
      const payload = JSON.parse(decodedPayload);
      const exp = payload.exp;
      if (!exp) return true;
      return Date.now() >= exp * 1000 - 10000; // 10 seconds buffer
   } catch {
      return true;
   }
}

interface CookieOptions {
   path?: string;
   domain?: string;
   maxAge?: number;
   expires?: Date;
   secure?: boolean;
   httpOnly?: boolean;
   sameSite?: boolean | "lax" | "strict" | "none";
}

interface ParsedCookie {
   name: string;
   value: string;
   options: CookieOptions;
}

function parseSetCookie(setCookieStr: string): ParsedCookie | null {
   const parts = setCookieStr.split(";").map((p) => p.trim());
   const nameValue = parts[0];
   const eqIdx = nameValue.indexOf("=");
   if (eqIdx === -1) return null;
   const name = nameValue.substring(0, eqIdx);
   const value = nameValue.substring(eqIdx + 1);

   const cookieOpt: CookieOptions = {};
   parts.slice(1).forEach((opt) => {
      const eqSign = opt.indexOf("=");
      let key = opt;
      let val = "";
      if (eqSign !== -1) {
         key = opt.substring(0, eqSign).trim();
         val = opt.substring(eqSign + 1).trim();
      }
      const lowerKey = key.toLowerCase();
      if (lowerKey === "path") cookieOpt.path = val;
      else if (lowerKey === "domain") cookieOpt.domain = val;
      else if (lowerKey === "max-age") cookieOpt.maxAge = parseInt(val, 10);
      else if (lowerKey === "expires") cookieOpt.expires = new Date(val);
      else if (lowerKey === "secure") cookieOpt.secure = true;
      else if (lowerKey === "httponly") cookieOpt.httpOnly = true;
      else if (lowerKey === "samesite") {
         const s = val.toLowerCase();
         if (s === "lax" || s === "strict" || s === "none") {
            cookieOpt.sameSite = s;
         } else if (s === "true" || s === "false") {
            cookieOpt.sameSite = s === "true";
         }
      }
   });

   return { name, value, options: cookieOpt };
}

export async function proxy(request: NextRequest) {
   const { pathname } = request.nextUrl;

   // 1. Exclude static assets and api routes
   if (
      pathname.startsWith("/_next") ||
      pathname.startsWith("/favicon.ico") ||
      pathname.startsWith("/images") ||
      pathname.startsWith("/api") ||
      pathname.startsWith("/monitoring")
   ) {
      return NextResponse.next();
   }

   // 1.5. Check for session tokens in URL query parameters (Token Exchange)
   const urlToken = request.nextUrl.searchParams.get("token");
   const urlRefresh = request.nextUrl.searchParams.get("refresh");
   const urlSession = request.nextUrl.searchParams.get("session");

   if (urlToken || urlRefresh || urlSession) {
      const cleanUrl = new URL(request.url);
      cleanUrl.searchParams.delete("token");
      cleanUrl.searchParams.delete("refresh");
      cleanUrl.searchParams.delete("session");

      const response = NextResponse.redirect(cleanUrl);
      const host = request.headers.get("x-forwarded-host") || request.headers.get("host") || "";
      const hostname = host.split(":")[0].toLowerCase();
      let rootDomain = undefined;
      if (
         hostname !== "localhost" &&
         hostname !== "127.0.0.1" &&
         !hostname.startsWith("192.168.")
      ) {
         rootDomain = hostname.split(".").slice(-2).join(".");
      }

      const cookieOpts = {
         path: "/",
         domain: rootDomain ? `.${rootDomain}` : undefined,
         secure: hostname !== "localhost" && hostname !== "127.0.0.1",
         httpOnly: true,
         sameSite: "lax" as const,
      };

      if (urlToken) response.cookies.set("access_token", urlToken, cookieOpts);
      if (urlSession) response.cookies.set("session_id", urlSession, cookieOpts);
      if (urlRefresh) response.cookies.set("refresh_token", urlRefresh, cookieOpts);

      return response;
   }

   // 2. Read the cookies
   let token = request.cookies.get("access_token")?.value;
   const sessionId = request.cookies.get("session_id")?.value;
   const refreshToken = request.cookies.get("refresh_token")?.value;

   const browserUrl = getBrowserUrl(request);
   const webUrl = getWebUrl(browserUrl);
   const partnerUrl = getPartnerUrl(browserUrl);
   const callbackUrl = `${partnerUrl}${pathname}`;

   const refreshedCookies: ParsedCookie[] = [];
   let isRefreshSuccess = false;

   // If token is missing or expired, but we have a refresh token
   if ((!token || isTokenExpired(token)) && refreshToken) {
      try {
         const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://127.0.0.1:8080/api/v1/";
         const refreshUrl = apiUrl.endsWith("/api/v1/")
            ? `${apiUrl}auth/refresh`
            : `${apiUrl.replace(/\/$/, "")}/api/v1/auth/refresh`;

         const userAgent = request.headers.get("user-agent");
         const headers: Record<string, string> = {
            "Content-Type": "application/json",
         };
         if (userAgent) {
            headers["User-Agent"] = userAgent;
         }
         const cookiesToSend: string[] = [];
         if (sessionId) cookiesToSend.push(`session_id=${sessionId}`);
         if (refreshToken) cookiesToSend.push(`refresh_token=${refreshToken}`);
         const xFgp = request.cookies.get("x_fgp")?.value;
         if (xFgp) {
            cookiesToSend.push(`x_fgp=${xFgp}`);
            headers["x-fgp"] = xFgp;
         }
         const builtCookieHeader = cookiesToSend.join("; ");
         if (builtCookieHeader) {
            headers["Cookie"] = builtCookieHeader;
         }

         const res = await fetch(refreshUrl, {
            method: "POST",
            headers,
         });

         if (res.ok) {
            const rawSetCookies = res.headers.getSetCookie();
            if (rawSetCookies && rawSetCookies.length > 0) {
               rawSetCookies.forEach((sc) => {
                  const parsed = parseSetCookie(sc);
                  if (parsed) {
                     refreshedCookies.push(parsed);
                     if (parsed.name === "access_token") {
                        token = parsed.value;
                     }
                  }
               });
               isRefreshSuccess = true;
            }
         }
      } catch (err) {
         console.error("Silent token refresh in partner middleware failed:", err);
      }
   }

   // If we still don't have a token, redirect to login
   if (!token) {
      const loginUrl = new URL(`${webUrl}/auth/login`);
      loginUrl.searchParams.set("callbackUrl", callbackUrl);
      const redirectResponse = NextResponse.redirect(loginUrl);
      // Clear expired cookies to prevent infinite loops
      redirectResponse.cookies.delete("access_token");
      redirectResponse.cookies.delete("session_id");
      redirectResponse.cookies.delete("refresh_token");
      return redirectResponse;
   }

   // Cập nhật lại cookies của request để các bước xử lý và các Server Components phía sau nhận được token mới
   if (isRefreshSuccess && refreshedCookies.length > 0) {
      refreshedCookies.forEach((c) => {
         request.cookies.set(c.name, c.value);
      });
      const updatedCookiesStr = request.cookies
         .getAll()
         .map((c) => `${c.name}=${c.value}`)
         .join("; ");
      request.headers.set("Cookie", updatedCookiesStr);
   }

   try {
      // 3. Cryptographic decode and verify at the Edge
      const secret =
         process.env.JWT_SECRET ||
         "250881f8a4fe0d887488ac76c247f82c0563a545f11129483f5417c157ff603d";
      const secretKey = new TextEncoder().encode(secret);
      const { payload } = await jwtVerify(token, secretKey);

      const roles = payload.roles as string[] | undefined;
      const isPartner = roles && roles.includes("ROLE_PARTNER");

      if (!isPartner) {
         // Redirect to consumer web app with access denied
         const loginUrl = new URL(`${webUrl}/auth/login`);
         loginUrl.searchParams.set("callbackUrl", callbackUrl);
         loginUrl.searchParams.set("error", "unauthorized_role");
         return NextResponse.redirect(loginUrl);
      }

      // Chạy next-intl middleware để xử lý ngôn ngữ và chuyển hướng
      const response = intlMiddleware(request);

      // Nếu đã refresh thành công, gán các cookie mới vào response trả về cho trình duyệt lưu lại
      if (isRefreshSuccess && refreshedCookies.length > 0) {
         if (response) {
            refreshedCookies.forEach((c) => {
               response.cookies.set(c.name, c.value, c.options);
            });
         }
      }
      return response;
   } catch (error) {
      console.error("JWT verification failed at the Edge middleware:", error);
      // Redirect to login page on expiration/invalid token
      const loginUrl = new URL(`${webUrl}/auth/login`);
      loginUrl.searchParams.set("callbackUrl", callbackUrl);
      const redirectResponse = NextResponse.redirect(loginUrl);
      redirectResponse.cookies.delete("access_token");
      redirectResponse.cookies.delete("session_id");
      redirectResponse.cookies.delete("refresh_token");
      return redirectResponse;
   }
}

export const config = {
   matcher: [
      /*
       * Match all request paths except for the ones starting with:
       * - api (API routes)
       * - _next/static (static files)
       * - _next/image (image optimization files)
       * - favicon.ico (favicon file)
       * - images (static public images)
       */
      "/((?!api|_next/static|_next/image|favicon.ico|images).*)",
   ],
};
