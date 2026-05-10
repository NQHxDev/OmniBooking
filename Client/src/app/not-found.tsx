"use client";

import NextLink from "next/link";
import Image from "next/image";
import { ArrowLeft, Home } from "lucide-react";

export default function NotFound() {
   return (
      <div className="flex min-h-screen flex-col bg-white font-sans">
         <header className="bg-[#003580] text-white py-4 px-6 sticky top-0 z-50 shadow-md">
            <div className="mx-auto max-w-7xl">
               <NextLink href="/" className="text-2xl font-bold tracking-tight">
                  OmniBooking.com
               </NextLink>
            </div>
         </header>

         <main className="flex flex-1 flex-col items-center justify-center px-6 py-12 text-center">
            <div className="relative mb-8 h-64 w-64 sm:h-80 sm:w-80 animate-in fade-in zoom-in duration-700">
               <Image
                  src="/images/not_found.png"
                  alt="Trang không tìm thấy"
                  fill
                  className="object-contain"
                  priority
               />
            </div>

            <div className="max-w-md animate-in fade-in slide-in-from-bottom-4 duration-1000 delay-200">
               <h1 className="text-4xl font-black tracking-tighter text-[#1a1a1a] sm:text-6xl mb-6 leading-[1.1]">
                  Ui chao! <br />
                  <span className="bg-gradient-to-r from-[#006ce4] to-[#003580] bg-clip-text text-transparent">
                     Bạn bị lạc rồi...
                  </span>
               </h1>
               <p className="text-lg text-zinc-500 mb-10 leading-relaxed">
                  Có vẻ như địa chỉ bạn đang tìm kiếm không tồn tại hoặc đã được di chuyển sang một
                  hành trình khác.
               </p>

               <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
                  <NextLink
                     href="/"
                     className="inline-flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl bg-[#006ce4] px-8 py-4 text-sm font-bold text-white shadow-xl shadow-blue-100 transition-all hover:bg-[#0057b7] hover:shadow-blue-200 active:scale-[0.98]"
                  >
                     <Home className="h-4 w-4" />
                     Về trang chủ
                  </NextLink>
                  <button
                     onClick={() => window.history.back()}
                     className="inline-flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl border border-zinc-200 bg-white px-8 py-4 text-sm font-bold text-zinc-900 transition-all hover:bg-zinc-50 active:scale-[0.98]"
                  >
                     <ArrowLeft className="h-4 w-4" />
                     Quay lại
                  </button>
               </div>
            </div>
         </main>

         <footer className="py-8 text-center text-xs text-zinc-400">
            <p>© 2026 OmniBooking.com. Bảo lưu mọi quyền.</p>
         </footer>
      </div>
   );
}
