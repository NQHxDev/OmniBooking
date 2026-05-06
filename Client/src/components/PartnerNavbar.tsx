"use client";

import Link from "next/link";
import { HelpCircle, User } from "lucide-react";

export default function PartnerNavbar() {
   return (
      <header className="bg-[#003580] text-white border-b border-white/10 sticky top-0 z-50">
         <div className="mx-auto flex max-w-[1100px] items-center justify-between px-4 py-4 sm:px-6">
            <div className="flex items-center gap-4">
               <Link href="/" className="text-2xl font-bold tracking-tight">
                  OmniBooking.com
               </Link>
            </div>

            <div className="flex items-center gap-6">
               <div className="hidden sm:flex items-center gap-2 text-sm font-medium text-blue-100 hover:text-white transition-colors cursor-pointer">
                  <HelpCircle className="h-5 w-5" />
                  <span>Trợ giúp</span>
               </div>

               <div className="h-4 w-px bg-white/20 hidden sm:block"></div>

               <Link
                  href="/auth/login"
                  className="flex items-center gap-2 text-sm font-bold text-white hover:bg-white/10 px-4 py-2 rounded-md transition-all border border-white/20"
               >
                  <User className="h-4 w-4" />
                  Đăng nhập
               </Link>
            </div>
         </div>
      </header>
   );
}
