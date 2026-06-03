"use client";
import Link from "next/link";

export default function NotFound() {
   return (
      <html lang="vi">
         <head>
            <title>404 - Không tìm thấy trang</title>
         </head>
         <body
            style={{
               fontFamily:
                  'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif',
               textAlign: "center",
               padding: "80px 20px",
               backgroundColor: "#f5f7fa",
               color: "#1a1a1a",
               margin: 0,
            }}
         >
            <div
               style={{
                  maxWidth: "500px",
                  margin: "0 auto",
                  padding: "40px 20px",
                  backgroundColor: "#ffffff",
                  borderRadius: "12px",
                  boxShadow: "0 4px 20px rgba(0, 0, 0, 0.05)",
               }}
            >
               <h1 style={{ fontSize: "72px", margin: "0 0 10px 0", color: "#dc3545" }}>404</h1>
               <h2 style={{ fontSize: "24px", margin: "0 0 20px 0", fontWeight: 600 }}>
                  Không tìm thấy trang
               </h2>
               <p
                  style={{
                     fontSize: "16px",
                     color: "#4b5563",
                     lineHeight: "1.6",
                     margin: "0 0 30px 0",
                  }}
               >
                  Đường dẫn bạn truy cập không tồn tại hoặc đã bị di chuyển trong hệ thống đối tác
                  OmniBooking.
               </p>
               <Link
                  href="/vi/dashboard"
                  style={{
                     display: "inline-block",
                     backgroundColor: "#006ce4",
                     color: "#ffffff",
                     padding: "12px 24px",
                     borderRadius: "6px",
                     textDecoration: "none",
                     fontWeight: 600,
                     fontSize: "15px",
                     transition: "background-color 0.2s",
                  }}
               >
                  Quay lại Dashboard
               </Link>
            </div>
         </body>
      </html>
   );
}
