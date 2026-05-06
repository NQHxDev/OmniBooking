"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { BedDouble, Calendar, Globe, Heart, Bell, LogOut } from "lucide-react";
import { useAuthStore } from "@/store/useAuthStore";

export default function Navbar() {
   const [mounted, setMounted] = useState(false);
   const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
   const user = useAuthStore((state) => state.user);
   const logout = useAuthStore((state) => state.logout);

   useEffect(() => {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setMounted(true);
   }, []);

   return (
      <header className="bg-[#003580] text-white">
         <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
            <div className="flex items-center gap-8">
               <Link href="/" className="text-2xl font-bold tracking-tight">
                  OmniBooking.com
               </Link>
               <nav className="hidden space-x-6 text-sm font-medium md:flex">
                  <a
                     href="#"
                     className="flex items-center gap-2 rounded-full border border-white/30 bg-white/10 px-4 py-2"
                  >
                     <BedDouble className="h-5 w-5" />
                     Lưu trú
                  </a>
                  <a
                     href="#"
                     className="flex items-center gap-2 px-4 py-2 hover:bg-white/10 rounded-full transition-colors"
                  >
                     <Globe className="h-5 w-5" />
                     Chuyến bay
                  </a>
                  <a
                     href="#"
                     className="flex items-center gap-2 px-4 py-2 hover:bg-white/10 rounded-full transition-colors"
                  >
                     <Calendar className="h-5 w-5" />
                     Thuê xe
                  </a>
               </nav>
            </div>
            <div className="flex items-center gap-4">
               <button className="rounded-full p-2 hover:bg-white/10 transition-colors">
                  <Bell className="h-6 w-6" />
               </button>
               <button className="rounded-full p-2 hover:bg-white/10 transition-colors">
                  <Heart className="h-6 w-6" />
               </button>
               <div className="hidden items-center gap-4 sm:flex">
                  <button className="text-sm font-semibold hover:underline">
                     Đăng chỗ nghỉ của Quý vị
                  </button>
                  {mounted && isLoggedIn ? (
                     <div className="flex items-center gap-4 animate-in fade-in duration-300">
                        <div className="flex items-center gap-2">
                           <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-500 font-bold text-white uppercase">
                              {user?.fullName?.charAt(0) || user?.username?.charAt(0)}
                           </div>
                           <span className="text-sm font-bold">
                              {user?.fullName || user?.username}
                           </span>
                        </div>
                        <button
                           onClick={async () => {
                              await logout();
                              window.location.reload();
                           }}
                           className="flex items-center gap-1 text-sm font-medium text-red-300 hover:text-red-200"
                        >
                           <LogOut className="h-4 w-4" />
                           Thoát
                        </button>
                     </div>
                  ) : (
                     <div className="flex items-center gap-2">
                        <Link
                           href="/auth/register"
                           className="rounded-sm bg-white px-4 py-1 text-sm font-bold text-[#003580] hover:bg-zinc-100"
                        >
                           Đăng ký
                        </Link>
                        <Link
                           href="/auth/login"
                           className="rounded-sm bg-white px-4 py-1 text-sm font-bold text-[#003580] hover:bg-zinc-100"
                        >
                           Đăng nhập
                        </Link>
                     </div>
                  )}
               </div>
            </div>
         </div>
      </header>
   );
}
