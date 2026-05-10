"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";
import { RotateCcw, Home } from "lucide-react";
import Link from "next/link";

export default function Error({
   error,
   reset,
}: {
   error: Error & { digest?: string };
   reset: () => void;
}) {
   useEffect(() => {
      console.error(error);
   }, [error]);

   return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 px-4 text-center">
         <div className="mb-8 rounded-full bg-red-100 p-6">
            <svg
               className="h-16 w-16 text-red-600"
               fill="none"
               viewBox="0 0 24 24"
               stroke="currentColor"
            >
               <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
               />
            </svg>
         </div>

         <h1 className="mb-2 text-3xl font-bold text-slate-900">Đã có lỗi xảy ra!</h1>
         <p className="mb-8 max-w-md text-slate-600">
            Chúng tôi rất tiếc vì sự gián đoạn này. Hệ thống đã ghi nhận lỗi và đang được xử lý.
         </p>

         <div className="flex flex-col gap-4 sm:flex-row">
            <Button
               onClick={() => reset()}
               className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700"
            >
               <RotateCcw className="h-4 w-4" />
               Thử lại
            </Button>
            <Link href="/">
               <Button variant="outline" className="flex items-center gap-2">
                  <Home className="h-4 w-4" />
                  Về trang chủ
               </Button>
            </Link>
         </div>

         {process.env.NODE_ENV === "development" && (
            <div className="mt-12 max-w-2xl rounded-lg bg-white p-4 text-left shadow-sm">
               <p className="mb-2 font-mono text-sm font-bold text-red-500">[Dev Error Details]:</p>
               <pre className="overflow-auto font-mono text-xs text-slate-700">
                  {error.message}
                  {error.stack}
               </pre>
            </div>
         )}
      </div>
   );
}
