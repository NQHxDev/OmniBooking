"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useAuthStore } from "@/store/useAuthStore";
import { useTranslations } from "next-intl";

interface GeniusBannerProps {
   isLoggedIn?: boolean;
   userName?: string;
   userAvatar?: string;
}

export default function GeniusBanner({
   isLoggedIn: propIsLoggedIn,
   userName: propUserName,
   userAvatar,
}: GeniusBannerProps) {
   const [mounted, setMounted] = useState(false);
   const storeIsLoggedIn = useAuthStore((state) => state.isLoggedIn);
   const storeUser = useAuthStore((state) => state.user);
   const t = useTranslations("Genius");

   useEffect(() => {
      const timer = setTimeout(() => setMounted(true), 0);
      return () => clearTimeout(timer);
   }, []);

   const isLoggedIn = propIsLoggedIn ?? (mounted ? storeIsLoggedIn : false);
   const userName =
      propUserName ?? (mounted ? storeUser?.fullName || storeUser?.username || "" : "");

   return (
      <div className="mt-16 flex flex-col md:flex-row items-center gap-8 rounded-2xl border border-zinc-200 p-8 shadow-[0_4px_20px_rgba(0,0,0,0.03)] bg-white transition-all hover:shadow-[0_8px_30px_rgba(0,0,0,0.06)] overflow-hidden relative group">
         {/* Decorative Background Element */}
         <div className="absolute -right-12 -top-12 h-32 w-32 rounded-full bg-blue-50 opacity-0 group-hover:opacity-100 transition-opacity duration-500 blur-2xl" />

         {/* Genius Icon/Logo */}
         <div className="h-24 w-24 shrink-0 bg-linear-to-br from-[#003580] to-[#0057b7] rounded-2xl flex items-center justify-center shadow-lg shadow-blue-900/10">
            <span className="text-white text-4xl font-black italic select-none">G</span>
         </div>

         <div className="flex-1 text-center md:text-left z-10">
            {isLoggedIn ? (
               <div className="flex flex-col md:flex-row items-center gap-4 animate-in fade-in slide-in-from-left-4 duration-500">
                  <div className="relative h-16 w-16 overflow-hidden rounded-full border-2 border-[#006ce4] p-0.5">
                     <Image
                        src={userAvatar || "https://i.pravatar.cc/150?u=gen"}
                        alt={userName || "User"}
                        width={64}
                        height={64}
                        className="rounded-full object-cover"
                     />
                  </div>
                  <div>
                     <h3 className="text-2xl font-bold text-[#1a1a1a]">
                        {t("welcomeBack", { name: userName })}
                     </h3>
                     <p className="mt-1 text-zinc-600">
                        {t("memberStatus")}{" "}
                        <span className="font-bold text-[#006ce4]">{t("level1")}</span>{" "}
                        {t("keepBooking")}
                     </p>
                     <div className="mt-3 flex items-center gap-2">
                        <span className="inline-flex items-center rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-bold text-[#006ce4] border border-blue-100">
                           {t("tenPercentDiscount")}
                        </span>
                        <Link
                           href="/profile"
                           className="text-sm font-bold text-[#006ce4] hover:underline ml-2"
                        >
                           {t("viewDetails")}
                        </Link>
                     </div>
                  </div>
               </div>
            ) : (
               <div className="animate-in fade-in duration-500">
                  <h3 className="text-2xl font-bold text-[#1a1a1a]">{t("saveTenPercent")}</h3>
                  <p className="mt-2 text-zinc-600">{t("geniusDescription")}</p>
                  <div className="mt-6 flex flex-wrap justify-center md:justify-start gap-4">
                     <Link
                        href="/auth/login"
                        className="rounded-xl bg-[#006ce4] px-8 py-2.5 text-sm font-bold text-white shadow-lg shadow-blue-200 hover:bg-[#0057b7] hover:shadow-blue-300 transition-all active:scale-[0.98]"
                     >
                        {t("login")}
                     </Link>
                     <Link
                        href="/auth/register"
                        className="rounded-xl border border-zinc-200 px-8 py-2.5 text-sm font-bold text-zinc-700 hover:bg-zinc-50 hover:border-zinc-300 transition-all active:scale-[0.98]"
                     >
                        {t("register")}
                     </Link>
                  </div>
               </div>
            )}
         </div>
      </div>
   );
}
