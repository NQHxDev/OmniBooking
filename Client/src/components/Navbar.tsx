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
import CurrencySwitcher from "./CurrencySwitcher";
import { useTranslations } from "next-intl";

export default function Navbar() {
   const t = useTranslations("Common");
   const tProfile = useTranslations("Profile");

   const [mounted, setMounted] = useState(false);
   const [isMenuOpen, setIsMenuOpen] = useState(false);
   const menuRef = useRef<HTMLDivElement>(null);

   const { user, isLoggedIn, setAuth, logout } = useAuthStore();

   useEffect(() => {
      const syncSession = async () => {
         // Nếu đã đăng nhập, thực hiện refresh ngầm để đồng bộ dữ liệu mới nhất
         if (isLoggedIn) {
            try {
               const freshUser = await authService.refresh();
               if (freshUser) setAuth(freshUser);
            } catch (error: unknown) {
               const err = error as { status?: number; message?: string };
               if (err?.status === 401 || err?.status === 403) {
                  // Session hết hạn
                  logout();
               }
            }
         }
         // Nếu chưa đăng nhập trong store nhưng đã mount, thử refresh xem có session cookie không
         else if (mounted) {
            try {
               const freshUser = await authService.refresh();
               if (freshUser) setAuth(freshUser);
            } catch {
               // Không có session, bỏ qua
            }
         }
      };

      if (mounted) {
         syncSession();
      }
   }, [isLoggedIn, mounted, setAuth, logout]);

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
                     className="flex items-center justify-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-1.5 hover:bg-white/20 transition-all min-w-[100px]"
                  >
                     <BedDouble className="h-4 w-4 shrink-0" />
                     <span className="truncate">{t("stays")}</span>
                  </a>
                  <a
                     href="#"
                     className="flex items-center justify-center gap-2 px-4 py-1.5 hover:bg-white/10 rounded-full transition-all min-w-[100px]"
                  >
                     <Globe className="h-4 w-4 shrink-0" />
                     <span className="truncate">{t("flights")}</span>
                  </a>
                  <a
                     href="#"
                     className="flex items-center justify-center gap-2 px-4 py-1.5 hover:bg-white/10 rounded-full transition-all min-w-[100px]"
                  >
                     <Calendar className="h-4 w-4 shrink-0" />
                     <span className="truncate">{t("carRentals")}</span>
                  </a>
               </nav>
            </div>

            <div className="flex items-center gap-2">
               <div className="flex items-center gap-0.5 shrink-0">
                  <LanguageSwitcher />
                  <CurrencySwitcher />
                  <button className="rounded-full p-2 hover:bg-white/10 transition-colors relative cursor-pointer shrink-0">
                     <Bell className="h-5 w-5" />
                     <span className="absolute top-2 right-2 h-2 w-2 bg-red-500 rounded-full border-2 border-[#003580]"></span>
                  </button>
                  <button className="rounded-full p-2 hover:bg-white/10 transition-colors cursor-pointer shrink-0">
                     <Heart className="h-5 w-5" />
                  </button>
               </div>

               <div className="h-6 w-px bg-white/20 mx-2 hidden sm:block shrink-0"></div>

               <div className="flex items-center gap-4 shrink-0">
                  <button className="hidden lg:block text-[13px] font-semibold hover:bg-white/10 px-3 py-1.5 rounded-md transition-colors min-w-[140px] text-center">
                     {isLoggedIn && isPartner ? (
                        <Link
                           href="/partner/dashboard"
                           className="flex items-center justify-center gap-2"
                        >
                           <ShieldCheck className="h-4 w-4 text-yellow-400 shrink-0" />
                           <span className="truncate">{t("manageProperty")}</span>
                        </Link>
                     ) : (
                        <Link href="/become-a-host" className="block truncate">
                           {t("listProperty")}
                        </Link>
                     )}
                  </button>

                  {mounted && isLoggedIn ? (
                     <div className="relative" ref={menuRef}>
                        <button
                           onClick={() => setIsMenuOpen(!isMenuOpen)}
                           className="flex items-center gap-2 rounded-full py-1 pl-1 pr-2 hover:bg-white/10 transition-all active:scale-95"
                        >
                           <div className="relative shrink-0">
                              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-linear-to-tr from-blue-600 to-indigo-500 border-2 border-white shadow-sm overflow-hidden">
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
                           <div className="hidden md:flex flex-col items-start text-left ml-1 min-w-[80px]">
                              <span className="text-[13px] font-bold leading-tight truncate max-w-[120px]">
                                 {user?.fullName || user?.username || ""}
                              </span>
                              <span className="text-[11px] font-semibold text-yellow-400 truncate">
                                 {tProfile("level1")}
                              </span>
                           </div>
                           <ChevronDown
                              className={`ml-1 h-3.5 w-3.5 shrink-0 transition-transform duration-300 ${isMenuOpen ? "rotate-180" : ""}`}
                           />
                        </button>

                        {/* Dropdown Menu */}
                        {isMenuOpen && (
                           <div className="absolute right-0 mt-2.5 w-56 origin-top-right rounded-xl bg-white text-zinc-900 shadow-2xl ring-1 ring-black ring-opacity-5 focus:outline-none animate-in fade-in zoom-in-95 duration-200 overflow-hidden">
                              <div className="py-1.5">
                                 <DropdownItem
                                    icon={<UserIcon className="h-[18px] w-[18px]" />}
                                    label={tProfile("items.personalInfo")}
                                    href="/profile"
                                 />
                                 <DropdownItem
                                    icon={<Briefcase className="h-[18px] w-[18px]" />}
                                    label={tProfile("items.bookings")}
                                    href="/bookings"
                                 />
                                 <DropdownItem
                                    icon={<ShieldCheck className="h-[18px] w-[18px]" />}
                                    label={tProfile("items.geniusProgram")}
                                    href="/genius"
                                 />
                                 <DropdownItem
                                    icon={<Wallet className="h-[18px] w-[18px]" />}
                                    label={tProfile("items.wallet")}
                                    href="/wallet"
                                 />
                                 <DropdownItem
                                    icon={<Star className="h-[18px] w-[18px]" />}
                                    label={tProfile("items.reviews")}
                                    href="/reviews"
                                 />
                                 <DropdownItem
                                    icon={<Heart className="h-[18px] w-[18px]" />}
                                    label={tProfile("items.wishlist")}
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
                                    {t("logout")}
                                 </button>
                              </div>
                           </div>
                        )}
                     </div>
                  ) : (
                     <div className="flex items-center gap-2.5">
                        <Link
                           href="/auth/register"
                           className="flex items-center justify-center rounded-md bg-white px-4 py-1.5 text-[13px] font-bold text-[#003580] hover:bg-zinc-100 transition-all active:scale-95 shadow-sm cursor-pointer min-w-[90px]"
                        >
                           {t("register")}
                        </Link>
                        <Link
                           href="/auth/login"
                           className="flex items-center justify-center rounded-md bg-white px-4 py-1.5 text-[13px] font-bold text-[#003580] hover:bg-zinc-100 transition-all active:scale-95 shadow-sm cursor-pointer min-w-[90px]"
                        >
                           {t("login")}
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
