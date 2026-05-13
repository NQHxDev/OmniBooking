"use client";

import { useState, useRef, useEffect } from "react";
import Link from "next/link";
import {
   User as UserIcon,
   Briefcase,
   Wallet,
   Star,
   Heart,
   LogOut,
   ChevronDown,
   ChevronRight,
   ShieldCheck,
} from "lucide-react";
import Image from "next/image";
import { useAuthStore } from "@/store/useAuthStore";
import LanguageSwitcher from "./LanguageSwitcher";
import CurrencySwitcher from "./CurrencySwitcher";
import { useTranslations } from "next-intl";

export default function ProfileNavbar() {
   const t = useTranslations("Common");
   const tProfile = useTranslations("Profile");
   const [mounted, setMounted] = useState(false);
   const [isMenuOpen, setIsMenuOpen] = useState(false);
   const menuRef = useRef<HTMLDivElement>(null);
   const { user, isLoggedIn, logout } = useAuthStore();

   useEffect(() => {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setMounted(true);
   }, []);

   useEffect(() => {
      const handleClickOutside = (event: MouseEvent) => {
         if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
            setIsMenuOpen(false);
         }
      };
      document.addEventListener("mousedown", handleClickOutside);
      return () => document.removeEventListener("mousedown", handleClickOutside);
   }, []);

   const getInitials = () => {
      const name = user?.fullName || user?.username || user?.email || "?";
      const parts = name.trim().split(/\s+/);
      return parts[parts.length - 1].charAt(0).toUpperCase();
   };

   if (!mounted) return null;

   return (
      <header className="sticky top-0 z-50 shadow-md">
         {/* Main Top Bar */}
         <div className="bg-[#003580] text-white">
            <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6 lg:px-8">
               <Link href="/" className="text-2xl font-bold tracking-tight hover:opacity-90">
                  OmniBooking.com
               </Link>

               <div className="flex items-center gap-4">
                  <div className="flex items-center gap-1">
                     <LanguageSwitcher />
                     <CurrencySwitcher />
                  </div>

                  <div className="h-6 w-px bg-white/20 mx-1 hidden sm:block"></div>

                  {isLoggedIn ? (
                     <div className="relative" ref={menuRef}>
                        <button
                           onClick={() => setIsMenuOpen(!isMenuOpen)}
                           className="flex items-center gap-2 rounded-full py-1 pl-1 pr-2 hover:bg-white/10 transition-all"
                        >
                           <div className="h-9 w-9 items-center justify-center rounded-full bg-linear-to-tr from-blue-600 to-indigo-500 border-2 border-white overflow-hidden flex">
                              {user?.avatarUrl ? (
                                 <Image
                                    src={user.avatarUrl}
                                    alt="Avatar"
                                    width={36}
                                    height={36}
                                    className="h-full w-full object-cover"
                                    unoptimized
                                 />
                              ) : (
                                 <span className="text-sm font-bold text-white">
                                    {getInitials()}
                                 </span>
                              )}
                           </div>
                           <div className="hidden md:flex flex-col items-start text-left ml-1">
                              <span className="text-[13px] font-bold leading-tight">
                                 {user?.fullName || user?.username}
                              </span>
                              <span className="text-[11px] font-semibold text-yellow-400">
                                 {tProfile("level1")}
                              </span>
                           </div>
                           <ChevronDown
                              className={`ml-1 h-3.5 w-3.5 transition-transform ${isMenuOpen ? "rotate-180" : ""}`}
                           />
                        </button>

                        {isMenuOpen && (
                           <div className="absolute right-0 mt-2.5 w-56 rounded-xl bg-white text-zinc-900 shadow-2xl ring-1 ring-black/5 overflow-hidden animate-in fade-in zoom-in-95 duration-200 z-50">
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
                                       window.location.href = "/";
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
                     <div className="flex gap-2">
                        <Link
                           href="/auth/login"
                           className="bg-white text-[#003580] px-4 py-1.5 rounded-md text-[13px] font-bold hover:bg-zinc-100"
                        >
                           {t("login")}
                        </Link>
                     </div>
                  )}
               </div>
            </div>
         </div>

         {/* Breadcrumb Bar */}
         <div className="bg-[#003580] text-white pb-3">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
               <nav className="flex items-center gap-2 text-[13px]">
                  <Link
                     href="/profile"
                     className="text-white/80 hover:text-white transition-colors underline underline-offset-4"
                  >
                     Tài khoản
                  </Link>
                  <ChevronRight className="h-3.5 w-3.5 text-white/60" />
                  <span className="font-medium">Thông tin cá nhân</span>
               </nav>
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
