"use client";

import { useEffect, useState, useRef } from "react";
import Link from "next/link";
import {
   BedDouble,
   Heart,
   LogOut,
   User as UserIcon,
   Briefcase,
   Wallet,
   Star,
   ShieldCheck,
   ChevronDown,
   Plane,
   Car,
   Compass,
   HelpCircle,
   CarTaxiFront,
} from "lucide-react";
import Image from "next/image";
import { useAuthStore } from "@/store/useAuthStore";
import LanguageSwitcher from "./LanguageSwitcher";
import CurrencySwitcher from "./CurrencySwitcher";
import { useTranslations } from "next-intl";
import { usePathname } from "next/navigation";

export default function Navbar() {
   const t = useTranslations("Common");
   const tProfile = useTranslations("Profile");
   const pathname = usePathname();
   const isProfilePage = pathname ? pathname.includes("/profile") : false;

   const [mounted, setMounted] = useState(false);
   const [isMenuOpen, setIsMenuOpen] = useState(false);
   const menuRef = useRef<HTMLDivElement>(null);

   const { user, isLoggedIn, logout } = useAuthStore();

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
      <header className="bg-[#003580] text-white shadow-md relative z-40">
         {/* Dòng 1: Logo và các nút chức năng */}
         <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3.5 sm:px-6 lg:px-8">
            <Link
               href="/"
               className="text-2xl font-bold tracking-tight transition-opacity hover:opacity-90 shrink-0"
            >
               OmniBooking.com
            </Link>

            <div className="flex items-center gap-2">
               <div className="flex items-center gap-0.5 shrink-0">
                  <CurrencySwitcher />
                  <LanguageSwitcher />
                  <button
                     className="rounded-full p-2 hover:bg-white/10 transition-colors cursor-pointer shrink-0 text-white flex items-center justify-center"
                     title="Trợ giúp"
                  >
                     <HelpCircle className="h-5 w-5" />
                  </button>
                  <button className="rounded-full p-2 hover:bg-white/10 transition-colors cursor-pointer shrink-0">
                     <Heart className="h-5 w-5" />
                  </button>
               </div>

               <div className="h-6 w-px bg-white/20 mx-2 hidden sm:block shrink-0"></div>

               <div
                  className={`flex items-center justify-end gap-4 shrink-0 transition-opacity duration-300 min-w-47.5 lg:min-w-85 ${
                     mounted ? "opacity-100" : "opacity-30"
                  }`}
               >
                  {!mounted ? (
                     // Skeleton giữ chỗ khi đang Hydration để tránh giật Navbar
                     <div className="flex items-center gap-3.5">
                        <div className="h-8 w-35 rounded bg-white/10 animate-pulse hidden lg:block"></div>
                        <div className="h-8 w-20 rounded bg-white/10 animate-pulse hidden md:block"></div>
                        <div className="h-9 w-9 rounded-full bg-white/10 animate-pulse"></div>
                     </div>
                  ) : (
                     <>
                        <button className="hidden lg:block text-[13px] font-semibold hover:bg-white/10 px-3 py-1.5 rounded-md transition-colors min-w-35 text-center shrink-0">
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

                        {isLoggedIn ? (
                           <div className="relative shrink-0" ref={menuRef}>
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
                                       <span className="text-[8px] font-bold text-[#003580]">
                                          G
                                       </span>
                                    </div>
                                 </div>
                                 <div className="hidden md:flex flex-col items-start text-left ml-1 min-w-20">
                                    <span className="text-[13px] font-bold leading-tight truncate max-w-30">
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
                                 <div className="absolute right-0 z-50 mt-2.5 w-56 origin-top-right rounded-xl bg-white text-zinc-900 shadow-2xl ring-1 ring-black ring-opacity-5 focus:outline-none animate-in fade-in zoom-in-95 duration-200 overflow-hidden">
                                    <div className="py-1.5">
                                       <DropdownItem
                                          icon={<UserIcon className="h-4.5 w-4.5" />}
                                          label={tProfile("items.personalInfo")}
                                          href="/profile"
                                       />
                                       <DropdownItem
                                          icon={<Briefcase className="h-4.5 w-4.5" />}
                                          label={tProfile("items.bookings")}
                                          href="/bookings"
                                       />
                                       <DropdownItem
                                          icon={<ShieldCheck className="h-4.5 w-4.5" />}
                                          label={tProfile("items.geniusProgram")}
                                          href="/genius"
                                       />
                                       <DropdownItem
                                          icon={<Wallet className="h-4.5 w-4.5" />}
                                          label={tProfile("items.wallet")}
                                          href="/wallet"
                                       />
                                       <DropdownItem
                                          icon={<Star className="h-4.5 w-4.5" />}
                                          label={tProfile("items.reviews")}
                                          href="/reviews"
                                       />
                                       <DropdownItem
                                          icon={<Heart className="h-4.5 w-4.5" />}
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
                                          <LogOut className="h-4.5 w-4.5" />
                                          {t("logout")}
                                       </button>
                                    </div>
                                 </div>
                              )}
                           </div>
                        ) : (
                           <div className="flex items-center gap-2.5 shrink-0">
                              <Link
                                 href="/auth/register"
                                 className="flex items-center justify-center rounded-md bg-white px-4 py-1.5 text-[13px] font-bold text-[#003580] hover:bg-zinc-100 transition-all active:scale-95 shadow-sm cursor-pointer min-w-22.5"
                              >
                                 {t("register")}
                              </Link>
                              <Link
                                 href="/auth/login"
                                 className="flex items-center justify-center rounded-md bg-white px-4 py-1.5 text-[13px] font-bold text-[#003580] hover:bg-zinc-100 transition-all active:scale-95 shadow-sm cursor-pointer min-w-22.5"
                              >
                                 {t("login")}
                              </Link>
                           </div>
                        )}
                     </>
                  )}
               </div>
            </div>
         </div>

         {/* Dòng 2: Thanh điều hướng các dịch vụ */}
         {!isProfilePage && (
            <div className="border-t border-white/10">
               <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-1.5">
                  <nav className="flex space-x-1.5 overflow-x-auto no-scrollbar text-[13.5px] font-medium py-1 scroll-smooth">
                     <a
                        href="#"
                        className="flex items-center justify-center gap-2 rounded-full border border-white bg-white/10 px-4 py-2 hover:bg-white/20 transition-all shrink-0"
                     >
                        <BedDouble className="h-4.5 w-4.5 shrink-0" />
                        <span>{t("stays")}</span>
                     </a>
                     <a
                        href="#"
                        className="flex items-center justify-center gap-2 px-4 py-2 hover:bg-white/10 rounded-full transition-all shrink-0"
                     >
                        <Plane className="h-4.5 w-4.5 shrink-0" />
                        <span>{t("flights")}</span>
                     </a>
                     <a
                        href="#"
                        className="flex items-center justify-center gap-2 px-4 py-2 hover:bg-white/10 rounded-full transition-all shrink-0"
                     >
                        <Plane className="h-4.5 w-4.5 shrink-0" />
                        <span>{t("flightHotel")}</span>
                     </a>
                     <a
                        href="#"
                        className="flex items-center justify-center gap-2 px-4 py-2 hover:bg-white/10 rounded-full transition-all shrink-0"
                     >
                        <Car className="h-4.5 w-4.5 shrink-0" />
                        <span>{t("carRentals")}</span>
                     </a>
                     <a
                        href="#"
                        className="flex items-center justify-center gap-2 px-4 py-2 hover:bg-white/10 rounded-full transition-all shrink-0"
                     >
                        <Compass className="h-4.5 w-4.5 shrink-0" />
                        <span>{t("attractions")}</span>
                     </a>
                     <a
                        href="#"
                        className="flex items-center justify-center gap-2 px-4 py-2 hover:bg-white/10 rounded-full transition-all shrink-0"
                     >
                        <CarTaxiFront className="h-4.5 w-4.5 shrink-0" />
                        <span>{t("airportTaxis")}</span>
                     </a>
                  </nav>
               </div>
            </div>
         )}
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
