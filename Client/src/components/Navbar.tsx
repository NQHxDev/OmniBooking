"use client";

import { useEffect, useState, useRef } from "react";
import Link from "next/link";
import {
   BedDouble,
   Calendar,
   Globe,
   Heart,
   Bell,
   LogOut,
   User as UserIcon,
   Briefcase,
   Wallet,
   Star,
   ShieldCheck,
   ChevronDown,
} from "lucide-react";
import Image from "next/image";
import { useAuthStore } from "@/store/useAuthStore";
import { authService } from "@/lib/api/services/authService";
import LanguageSwitcher from "./LanguageSwitcher";
import { useTranslations } from "next-intl";

export default function Navbar() {
   const t = useTranslations("Common");

   const [mounted, setMounted] = useState(false);
   const [isMenuOpen, setIsMenuOpen] = useState(false);
   const menuRef = useRef<HTMLDivElement>(null);

   const { user, isLoggedIn, setAuth, logout } = useAuthStore();
   const prevIsLoggedIn = useRef(isLoggedIn);

   useEffect(() => {
      const syncSession = async () => {
         if (isLoggedIn && prevIsLoggedIn.current) {
            try {
               const freshUser = await authService.refresh();
               if (freshUser) setAuth(freshUser);
            } catch (error: unknown) {
               // Silence 401/403 sync errors as they are expected when session expires
               const err = error as { status?: number; message?: string };
               if (err?.status !== 401 && err?.status !== 403) {
                  console.error("[Navbar] Session sync failed:", err.message || err);
               }
            }
         }
         prevIsLoggedIn.current = isLoggedIn;
      };
      syncSession();
   }, [isLoggedIn, setAuth]);

   // Tối ưu check partner để hỗ trợ cả data cũ và mới trong store
   const isPartner = user?.roles?.includes("ROLE_PARTNER");

   useEffect(() => {
      const timer = setTimeout(() => setMounted(true), 0);

      const handleClickOutside = (event: MouseEvent) => {
         if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
            setIsMenuOpen(false);
         }
      };

      document.addEventListener("mousedown", handleClickOutside);
      return () => {
         clearTimeout(timer);
         document.removeEventListener("mousedown", handleClickOutside);
      };
   }, []);

   // Xử lý lấy ký tự đầu cho Avatar
   const getInitials = () => {
      const name = user?.fullName || user?.username || user?.email || "?";
      const parts = name.trim().split(/\s+/);
      const lastPart = parts[parts.length - 1];
      return lastPart.charAt(0).toUpperCase();
   };

   return (
      <header className="bg-[#003580] text-white sticky top-0 z-50 shadow-md">
         <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-2.5 sm:px-6 lg:px-8">
            <div className="flex items-center gap-8">
               <Link
                  href="/"
                  className="text-2xl font-bold tracking-tight transition-opacity hover:opacity-90"
               >
                  OmniBooking.com
               </Link>
               <nav className="hidden space-x-1 text-[13px] font-medium md:flex">
                  <a
                     href="#"
                     className="flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-1.5 hover:bg-white/20 transition-all"
                  >
                     <BedDouble className="h-4 w-4" />
                     {t("stays") || "Lưu trú"}
                  </a>
                  <a
                     href="#"
                     className="flex items-center gap-2 px-4 py-1.5 hover:bg-white/10 rounded-full transition-all"
                  >
                     <Globe className="h-4 w-4" />
                     {t("flights") || "Chuyến bay"}
                  </a>
                  <a
                     href="#"
                     className="flex items-center gap-2 px-4 py-1.5 hover:bg-white/10 rounded-full transition-all"
                  >
                     <Calendar className="h-4 w-4" />
                     {t("carRentals") || "Thuê xe"}
                  </a>
               </nav>
            </div>

            <div className="flex items-center gap-2">
               <div className="flex items-center gap-0.5">
                  <LanguageSwitcher />
                  <button className="rounded-full p-2 hover:bg-white/10 transition-colors relative">
                     <Bell className="h-5 w-5" />
                     <span className="absolute top-2 right-2 h-2 w-2 bg-red-500 rounded-full border-2 border-[#003580]"></span>
                  </button>
                  <button className="rounded-full p-2 hover:bg-white/10 transition-colors">
                     <Heart className="h-5 w-5" />
                  </button>
               </div>

               <div className="h-6 w-px bg-white/20 mx-2 hidden sm:block"></div>

               <div className="flex items-center gap-4">
                  <button className="hidden lg:block text-[13px] font-semibold hover:bg-white/10 px-3 py-1.5 rounded-md transition-colors">
                     {isLoggedIn && isPartner ? (
                        <Link href="/partner/dashboard" className="flex items-center gap-2">
                           <ShieldCheck className="h-4 w-4 text-yellow-400" />
                           {t("manageProperty") || "Quản lý chỗ nghỉ"}
                        </Link>
                     ) : (
                        <Link href="/become-a-host">
                           {t("listProperty") || "Đăng chỗ nghỉ của Quý vị"}
                        </Link>
                     )}
                  </button>

                  {mounted && isLoggedIn ? (
                     <div className="relative" ref={menuRef}>
                        <button
                           onClick={() => setIsMenuOpen(!isMenuOpen)}
                           className="flex items-center gap-2 rounded-full py-1 pl-1 pr-2 hover:bg-white/10 transition-all active:scale-95"
                        >
                           <div className="relative">
                              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-tr from-blue-600 to-indigo-500 border-2 border-white shadow-sm overflow-hidden">
                                 {user?.avatarUrl ? (
                                    <Image
                                       src={user.avatarUrl}
                                       alt={user?.fullName || "Avatar"}
                                       width={36}
                                       height={36}
                                       className="h-full w-full object-cover"
                                       unoptimized
                                    />
                                 ) : (
                                    <span className="text-sm font-bold text-white uppercase">
                                       {getInitials()}
                                    </span>
                                 )}
                              </div>
                              <div className="absolute -bottom-0.5 -right-0.5 h-3.5 w-3.5 rounded-full bg-yellow-400 border-2 border-[#003580] flex items-center justify-center">
                                 <span className="text-[8px] font-bold text-[#003580]">G</span>
                              </div>
                           </div>
                           <div className="hidden md:flex flex-col items-start text-left ml-1">
                              <span className="text-[13px] font-bold leading-tight">
                                 {user?.fullName || user?.username}
                              </span>
                              <span className="text-[11px] font-semibold text-yellow-400">
                                 Genius Cấp 1
                              </span>
                           </div>
                           <ChevronDown
                              className={`ml-1 h-3.5 w-3.5 transition-transform duration-300 ${isMenuOpen ? "rotate-180" : ""}`}
                           />
                        </button>

                        {/* Dropdown Menu */}
                        {isMenuOpen && (
                           <div className="absolute right-0 mt-2.5 w-56 origin-top-right rounded-xl bg-white text-zinc-900 shadow-2xl ring-1 ring-black ring-opacity-5 focus:outline-none animate-in fade-in zoom-in-95 duration-200 overflow-hidden">
                              <div className="py-1.5">
                                 <DropdownItem
                                    icon={<UserIcon className="h-[18px] w-[18px]" />}
                                    label="Quản lý tài khoản"
                                    href="/profile"
                                 />
                                 <DropdownItem
                                    icon={<Briefcase className="h-[18px] w-[18px]" />}
                                    label="Đặt chỗ & Chuyến đi"
                                    href="/bookings"
                                 />
                                 <DropdownItem
                                    icon={<ShieldCheck className="h-[18px] w-[18px]" />}
                                    label="Chương trình Genius"
                                    href="/genius"
                                 />
                                 <DropdownItem
                                    icon={<Wallet className="h-[18px] w-[18px]" />}
                                    label="Tặng thưởng & Ví"
                                    href="/wallet"
                                 />
                                 <DropdownItem
                                    icon={<Star className="h-[18px] w-[18px]" />}
                                    label="Đánh giá"
                                    href="/reviews"
                                 />
                                 <DropdownItem
                                    icon={<Heart className="h-[18px] w-[18px]" />}
                                    label="Đã lưu"
                                    href="/wishlist"
                                 />
                              </div>

                              <div className="border-t border-zinc-100 py-1.5">
                                 <button
                                    onClick={async () => {
                                       await logout();
                                       window.location.reload();
                                    }}
                                    className="flex w-full items-center gap-3 px-4 py-2.5 text-[13px] font-medium text-red-600 hover:bg-red-50 transition-colors"
                                 >
                                    <LogOut className="h-[18px] w-[18px]" />
                                    Đăng xuất
                                 </button>
                              </div>
                           </div>
                        )}
                     </div>
                  ) : (
                     <div className="flex items-center gap-2.5">
                        <Link
                           href="/auth/register"
                           className="rounded-md bg-white px-4 py-1.5 text-[13px] font-bold text-[#003580] hover:bg-zinc-100 transition-all active:scale-95 shadow-sm"
                        >
                           {t("register") || "Đăng ký"}
                        </Link>
                        <Link
                           href="/auth/login"
                           className="rounded-md bg-white px-4 py-1.5 text-[13px] font-bold text-[#003580] hover:bg-zinc-100 transition-all active:scale-95 shadow-sm"
                        >
                           {t("login") || "Đăng nhập"}
                        </Link>
                     </div>
                  )}
               </div>
            </div>
         </div>
      </header>
   );
}

function DropdownItem({
   icon,
   label,
   href,
}: {
   icon: React.ReactNode;
   label: string;
   href: string;
}) {
   return (
      <Link
         href={href}
         className="flex items-center gap-3 px-4 py-2.5 text-[13px] font-medium text-zinc-700 hover:bg-zinc-50 transition-colors"
      >
         <span className="text-zinc-400">{icon}</span>
         {label}
      </Link>
   );
}
