"use client";

import * as Sentry from "@sentry/nextjs";
import { useEffect } from "react";

export default function GlobalError({
   error,
   reset,
}: {
   error: Error & { digest?: string };
   reset: () => void;
}) {
   useEffect(() => {
      Sentry.captureException(error);
   }, [error]);

   return (
      <html lang="vi">
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
               <h1 style={{ fontSize: "48px", margin: "0 0 10px 0", color: "#dc3545" }}>
                  Đã xảy ra lỗi hệ thống
               </h1>
               <p
                  style={{
                     fontSize: "16px",
                     color: "#4b5563",
                     lineHeight: "1.6",
                     margin: "0 0 30px 0",
                  }}
               >
                  Đã xảy ra lỗi ngoài ý muốn. Vui lòng thử lại.
               </p>
               <button
                  onClick={() => reset()}
                  style={{
                     display: "inline-block",
                     backgroundColor: "#006ce4",
                     color: "#ffffff",
                     border: "none",
                     padding: "12px 24px",
                     borderRadius: "6px",
                     fontWeight: 600,
                     fontSize: "15px",
                     cursor: "pointer",
                  }}
               >
                  Thử lại
               </button>
            </div>
         </body>
      </html>
   );
}
