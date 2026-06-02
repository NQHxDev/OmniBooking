"use client";

import { Bell, Search, CheckCircle } from "lucide-react";
import { useAuthStore } from "@/store/useAuthStore";
import { useTranslations } from "next-intl";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import CurrencySwitcher from "@/components/CurrencySwitcher";

export default function DashboardHeader() {
   const { user } = useAuthStore();
   const t = useTranslations("Owner.header");

   const getInitials = () => {
      const name = user?.fullName || user?.username || user?.email || "?";
      const parts = name.trim().split(/\s+/);
      const lastPart = parts[parts.length - 1];
      return lastPart.charAt(0).toUpperCase();
   };

   const getRoleLabel = () => {
      if (user?.roles?.includes("ROLE_OWNER")) return t("ownerRole");
      return t("adminRole");
   };

   return (
      <header className="sticky top-0 z-10 flex h-20 items-center justify-between border-b border-zinc-100 bg-white/80 px-8 backdrop-blur-md font-sans">
         <div className="flex items-center gap-4 flex-1">
            <div className="relative w-full max-w-md hidden md:block">
               <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400" />
               <input
                  type="text"
                  placeholder={t("searchPlaceholder")}
                  className="w-full rounded-xl border border-zinc-100 bg-zinc-50/50 py-2.5 pl-10 pr-4 text-sm outline-none focus:border-rose-500 focus:ring-4 focus:ring-rose-50 transition-all"
               />
            </div>
         </div>

         <div className="flex items-center gap-4">
            <div className="flex items-center gap-1">
               <CurrencySwitcher theme="white-bg" />
               <LanguageSwitcher theme="white-bg" />
            </div>

            <button className="relative rounded-xl border border-zinc-100 p-2.5 text-zinc-500 hover:bg-zinc-50 transition-all">
               <Bell className="h-5 w-5" />
               <span className="absolute right-2.5 top-2.5 h-2 w-2 rounded-full bg-red-500 border-2 border-white" />
            </button>

            <div className="flex items-center gap-3 pl-4 border-l border-zinc-100">
               <div className="text-right hidden sm:block">
                  <div className="flex items-center gap-1 justify-end">
                     <p className="text-sm font-bold text-zinc-900 leading-tight">
                        {user?.fullName || user?.username}
                     </p>
                     <CheckCircle className="h-3 w-3 text-rose-500 fill-rose-500" />
                  </div>
                  <div className="flex items-center gap-2 mt-0.5 justify-end">
                     <span className="text-[10px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded bg-rose-100 text-rose-700">
                        {getRoleLabel()}
                     </span>
                  </div>
               </div>
               <div className="h-10 w-10 rounded-full bg-linear-to-tr from-rose-600 to-rose-400 border-2 border-white shadow-sm flex items-center justify-center">
                  <span className="text-white font-bold text-sm">{getInitials()}</span>
               </div>
            </div>
         </div>
      </header>
   );
}
