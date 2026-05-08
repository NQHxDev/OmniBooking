"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { CheckCircle, XCircle, Loader2, ArrowRight, ShieldCheck } from "lucide-react";
import Link from "next/link";
import apiClient from "@/lib/api/apiClient";

export default function VerifyPage() {
   const searchParams = useSearchParams();
   const token = searchParams.get("token");

   const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
   const [message, setMessage] = useState("");

   useEffect(() => {
      if (!token) {
         const timer = setTimeout(() => {
            setStatus("error");
            setMessage("Mã xác nhận không hợp lệ hoặc đã hết hạn.");
         }, 0);
         return () => clearTimeout(timer);
      }

      const verifyEmail = async () => {
         try {
            await apiClient.get(`/auth/verify?token=${token}`);
            setStatus("success");
            setMessage("Tài khoản của bạn đã được xác thực thành công!");
         } catch (err: unknown) {
            setStatus("error");
            const errorMessage = (err as { response?: { data?: { message?: string } } }).response?.data?.message 
               || "Xác thực thất bại. Vui lòng thử lại sau.";
            setMessage(errorMessage);
         }
      };

      verifyEmail();
   }, [token]);

   return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-[#f8fafc] px-4 font-sans">
         <div className="w-full max-w-md rounded-3xl bg-white p-12 text-center shadow-2xl shadow-blue-100 border border-blue-50">
            <div className="mb-8 flex justify-center">
               <div className="flex h-20 w-20 items-center justify-center rounded-2xl bg-blue-50 text-[#006ce4]">
                  <ShieldCheck className="h-10 w-10" />
               </div>
            </div>

            <h1 className="mb-4 text-2xl font-bold tracking-tight text-[#1a1a1a]">
               Xác thực tài khoản
            </h1>

            {status === "loading" && (
               <div className="flex flex-col items-center gap-4 py-8">
                  <Loader2 className="h-10 w-10 animate-spin text-blue-500" />
                  <p className="text-zinc-500 animate-pulse">Đang xử lý dữ liệu...</p>
               </div>
            )}

            {status === "success" && (
               <div className="animate-in fade-in zoom-in duration-500">
                  <div className="mb-6 flex justify-center text-green-500">
                     <CheckCircle className="h-16 w-16" />
                  </div>
                  <p className="mb-8 text-zinc-600 leading-relaxed">
                     {message} <br />
                     Chào mừng bạn đến với cộng đồng <strong>OmniBooking</strong>.
                  </p>
                  <Link
                     href="/"
                     className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#006ce4] py-4 text-sm font-bold text-white shadow-lg shadow-blue-200 transition-all hover:bg-[#0057b7] active:scale-[0.98]"
                  >
                     Bắt đầu khám phá ngay
                     <ArrowRight className="h-4 w-4" />
                  </Link>
               </div>
            )}

            {status === "error" && (
               <div className="animate-in fade-in zoom-in duration-500">
                  <div className="mb-6 flex justify-center text-red-500">
                     <XCircle className="h-16 w-16" />
                  </div>
                  <p className="mb-8 text-zinc-600 leading-relaxed">
                     {message}
                  </p>
                  <div className="space-y-3">
                     <Link
                        href="/auth/login"
                        className="flex w-full items-center justify-center gap-2 rounded-xl bg-zinc-100 py-4 text-sm font-bold text-zinc-700 transition-all hover:bg-zinc-200 active:scale-[0.98]"
                     >
                        Quay lại Đăng nhập
                     </Link>
                     <p className="text-xs text-zinc-400">
                        Bạn cần hỗ trợ? <a href="#" className="text-blue-500 hover:underline">Liên hệ chúng tôi</a>
                     </p>
                  </div>
               </div>
            )}
         </div>

         {/* Branding footer */}
         <div className="mt-12 flex items-center gap-2 text-zinc-400">
            <span className="text-sm font-bold tracking-tighter text-zinc-300">OmniBooking.</span>
            <div className="h-1 w-1 rounded-full bg-zinc-300" />
            <span className="text-xs">© 2026 Toàn bộ quyền được bảo lưu</span>
         </div>
      </div>
   );
}
