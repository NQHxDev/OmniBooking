import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Danh sách các route yêu cầu đăng nhập.
 * Bạn có thể mở rộng danh sách này khi có thêm các tính năng mới.
 */
const protectedRoutes = ["/become-a-host/register", "/partner", "/profile"];

/**
 * Middleware để kiểm tra quyền truy cập của người dùng dựa trên Cookies.
 * Đảm bảo người dùng bị "đá ra ngoài ngay" nếu chưa đăng nhập.
 */
export function middleware(request: NextRequest) {
   const { pathname } = request.nextUrl;

   // Kiểm tra xem route hiện tại có nằm trong danh sách cần bảo vệ không
   const isProtected = protectedRoutes.some((route) => pathname.startsWith(route));

   if (isProtected) {
      const accessToken = request.cookies.get("access_token")?.value;
      const sessionId = request.cookies.get("session_id")?.value;

      // Nếu thiếu token hoặc session id thì coi như chưa đăng nhập
      if (!accessToken || !sessionId) {
         const loginUrl = new URL("/auth/login", request.url);

         // Lưu lại URL hiện tại để sau khi đăng nhập xong có thể quay lại đúng chỗ
         loginUrl.searchParams.set("callbackUrl", pathname);

         return NextResponse.redirect(loginUrl);
      }
   }

   return NextResponse.next();
}

/**
 * Cấu hình matcher để loại bỏ các file tĩnh và API routes khỏi middleware
 * nhằm tối ưu hiệu năng.
 */
export const config = {
   matcher: [
      /*
       * Khớp tất cả các đường dẫn ngoại trừ:
       * - api (các endpoint backend hoặc route handlers)
       * - _next/static (file tĩnh của Next.js)
       * - _next/image (file ảnh tối ưu hóa)
       * - favicon.ico, public files
       */
      "/((?!api|_next/static|_next/image|favicon.ico|.*\\..*).*)",
   ],
};
