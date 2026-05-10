import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Middleware để kiểm tra quyền truy cập của người dùng dựa trên Cookies.
 * Chiến thuật:
 * - Guest Guard: Không cho người đã đăng nhập vào trang login/register.
 * - Partner Hub: Cho phép qua để Server Component tự xử lý (ổn định hơn cho F5).
 * - Profile/Settings: Chặn trực tiếp ở đây.
 */
export function middleware(request: NextRequest) {
   const { pathname } = request.nextUrl;

   // Lấy session_id và refresh_token để kiểm tra đăng nhập
   const sessionId = request.cookies.get("session_id")?.value;
   const refreshToken = request.cookies.get("refresh_token")?.value;

   const hasSession = !!(sessionId || refreshToken);

   // 1. GUEST GUARD: Nếu đã login thì không cho vào trang auth
   if (pathname.startsWith("/auth/") && hasSession) {
      return NextResponse.redirect(new URL("/", request.url));
   }

   // 2. PARTNER HUB: Để cho Page (Server Component) tự xử lý vì nó đọc cookie tốt hơn ở Edge Runtime
   if (pathname.startsWith("/partner")) {
      return NextResponse.next();
   }

   // 3. AUTH GUARD: Bảo vệ các trang cá nhân
   const isProtected = ["/profile", "/settings", "/bookings"].some((route) =>
      pathname.startsWith(route)
   );

   if (isProtected && !hasSession) {
      const loginUrl = new URL("/auth/login", request.url);
      loginUrl.searchParams.set("callbackUrl", pathname);
      return NextResponse.redirect(loginUrl);
   }

   return NextResponse.next();
}

// Chỉ chạy middleware trên các đường dẫn cần thiết
export const config = {
   matcher: ["/((?!api|_next/static|_next/image|favicon.ico|.*\\..*).*)"],
};
