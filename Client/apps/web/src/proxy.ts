import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import createMiddleware from "next-intl/middleware";
import { routing } from "./i18n/routing";
import { getPartnerUrl } from "@omnibooking/shared";

function getBrowserUrl(request: NextRequest): string {
   const host = request.headers.get("x-forwarded-host") || request.headers.get("host") || "";
   const proto = request.headers.get("x-forwarded-proto") || "http";
   return `${proto}://${host}`;
}

function getRootDomain(host: string): string {
   const hostname = host.split(":")[0];
   if (hostname === "localhost" || hostname === "127.0.0.1" || hostname.startsWith("192.168.")) {
      return hostname;
   }
   return hostname.split(".").slice(-2).join(".");
}

function isValidCallbackUrl(urlStr: string, allowedRootDomain: string): boolean {
   if (!urlStr) return false;
   if (urlStr.startsWith("/")) return true;
   try {
      const url = new URL(urlStr);
      const hostname = url.hostname.toLowerCase();
      return hostname === allowedRootDomain || hostname.endsWith(`.${allowedRootDomain}`);
   } catch {
      return false;
   }
}

const intlMiddleware = createMiddleware(routing);

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

   // Redirect legacy partner routes on the web app to the partner subdomain
   const cleanPath = pathname.replace(/^\/(vi|en)/, "");
   if (cleanPath.startsWith("/partner")) {
      const browserUrl = getBrowserUrl(request);
      const partnerUrl = getPartnerUrl(browserUrl);

      let subPath = cleanPath.replace(/^\/partner/, "");
      if (!subPath.startsWith("/")) subPath = "/" + subPath;

      const targetUrl = new URL(`${partnerUrl}${subPath}${request.nextUrl.search}`);
      return NextResponse.redirect(targetUrl);
   }

   // Lấy access_token, session_id và refresh_token để kiểm tra đăng nhập
   let accessToken = request.cookies.get("access_token")?.value;
   let sessionId = request.cookies.get("session_id")?.value;
   const refreshToken = request.cookies.get("refresh_token")?.value;

   // Xác định ngôn ngữ (locale) hiện tại
   const segments = pathname.split("/");
   const locale = routing.locales.includes(segments[1] as "vi" | "en")
      ? segments[1]
      : routing.defaultLocale;

   const getLocalePrefix = (loc: string) => (loc === routing.defaultLocale ? "" : `/${loc}`);

   const refreshedCookies: ParsedCookie[] = [];
   let isRefreshSuccess = false;

   // Nếu access token (accessToken) đã hết hạn hoặc không tồn tại, nhưng có refresh token
   if ((!accessToken || isTokenExpired(accessToken)) && refreshToken) {
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
                        accessToken = parsed.value;
                     } else if (parsed.name === "session_id") {
                        sessionId = parsed.value;
                     }
                  }
               });
               isRefreshSuccess = true;
            }
         } else {
            // Refresh thất bại (ví dụ refresh token cũng hết hạn) -> Xóa cookie và đẩy về trang đăng nhập
            const browserUrl = getBrowserUrl(request);
            const loginUrl = new URL(`${getLocalePrefix(locale)}/auth/login`, browserUrl);
            loginUrl.searchParams.set("callbackUrl", pathname);
            const response = NextResponse.redirect(loginUrl);
            response.cookies.delete("access_token");
            response.cookies.delete("session_id");
            response.cookies.delete("refresh_token");
            return response;
         }
      } catch (err) {
         console.error("Silent token refresh in middleware failed:", err);
      }
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

   const hasSession = !!(sessionId || refreshToken);

   // GUEST GUARD: Nếu đã đăng nhập thì không cho vào trang đăng nhập/đăng ký
   const isAuthPage = /^\/(?:[a-z]{2}\/)?auth\/(?!verify)/.test(pathname);
   if (isAuthPage && hasSession) {
      const browserUrl = getBrowserUrl(request);
      const browserHost =
         request.headers.get("x-forwarded-host") || request.headers.get("host") || "";
      const rootDomain = getRootDomain(browserHost);
      const callbackUrl = request.nextUrl.searchParams.get("callbackUrl");

      let targetUrl = getLocalePrefix(locale) || "/";
      if (callbackUrl && isValidCallbackUrl(callbackUrl, rootDomain)) {
         targetUrl = callbackUrl;
      }

      const redirectResponse = NextResponse.redirect(new URL(targetUrl, browserUrl));
      if (isRefreshSuccess && refreshedCookies.length > 0) {
         refreshedCookies.forEach((c) => {
            redirectResponse.cookies.set(c.name, c.value, c.options);
         });
      }
      return redirectResponse;
   }

   // AUTH GUARD: Bảo vệ các trang cá nhân
   const protectedRoutes = ["/profile", "/settings", "/bookings"];
   const isProtected = protectedRoutes.some((route) => pathname.includes(route));

   if (isProtected && !hasSession) {
      const browserUrl = getBrowserUrl(request);
      const loginUrl = new URL(`${getLocalePrefix(locale)}/auth/login`, browserUrl);
      loginUrl.searchParams.set("callbackUrl", pathname);
      return NextResponse.redirect(loginUrl);
   }

   return response;
}

export const config = {
   // Matcher cho tất cả các trang trừ file tĩnh và api
   matcher: ["/((?!api|_next/static|_next/image|favicon.ico|.*\\..*).*)"],
};
