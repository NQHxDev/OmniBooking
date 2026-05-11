import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import createMiddleware from "next-intl/middleware";
import { routing } from "./i18n/routing";

const intlMiddleware = createMiddleware(routing);

export function middleware(request: NextRequest) {
   const { pathname } = request.nextUrl;

   // 1. Chạy intlMiddleware để xử lý locale và redirect tự động
   const response = intlMiddleware(request);

   // Lấy session_id và refresh_token để kiểm tra đăng nhập
   const sessionId = request.cookies.get("session_id")?.value;
   const refreshToken = request.cookies.get("refresh_token")?.value;
   const hasSession = !!(sessionId || refreshToken);

   // 2. Auth & Guest Guards
   // Kiểm tra xem pathname có bắt đầu bằng locale hợp lệ không (vi|en)
   const segments = pathname.split("/");
   const locale = routing.locales.includes(segments[1] as "vi" | "en")
      ? segments[1]
      : routing.defaultLocale;

   // GUEST GUARD: Nếu đã login thì không cho vào trang auth (trừ verify)
   const isAuthPage = /^\/([a-z]{2})\/auth\/(?!verify)/.test(pathname);
   if (isAuthPage && hasSession) {
      return NextResponse.redirect(new URL(`/${locale}`, request.url));
   }

   // AUTH GUARD: Bảo vệ các trang cá nhân
   const protectedRoutes = ["/profile", "/settings", "/bookings", "/partner"];
   const isProtected = protectedRoutes.some((route) => pathname.includes(route));

   if (isProtected && !hasSession) {
      const loginUrl = new URL(`/${locale}/auth/login`, request.url);
      loginUrl.searchParams.set("callbackUrl", pathname);
      return NextResponse.redirect(loginUrl);
   }

   return response;
}

export const config = {
   // Matcher cho tất cả các trang trừ file tĩnh và api
   matcher: ["/((?!api|_next/static|_next/image|favicon.ico|.*\\..*).*)"],
};
