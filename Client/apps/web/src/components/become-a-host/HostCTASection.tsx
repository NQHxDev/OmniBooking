"use client";

import { Check, ArrowRight } from "lucide-react";
import Link from "next/link";
import { useAuthStore } from "@/store/useAuthStore";
import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";

export default function HostCTASection() {
   const t = useTranslations("BecomeAHost.cta");
   const [mounted, setMounted] = useState(false);
   const user = useAuthStore((state) => state.user);

   useEffect(() => {
      // Use setTimeout to avoid synchronous setState warning in effect
      const timer = setTimeout(() => setMounted(true), 0);
      return () => clearTimeout(timer);
   }, []);

   if (!mounted) {
      return <div className="min-h-[400px] w-full animate-pulse bg-zinc-100 rounded-3xl" />;
   }

   return (
      <div className="lg:col-span-5">
         <div className="relative">
            <div className="absolute -inset-1 rounded-[2rem] bg-linear-to-r from-blue-400 to-blue-600 opacity-20 blur-xl"></div>
            <div className="relative rounded-3xl bg-white p-8 shadow-2xl lg:p-10">
               <h3 className="text-2xl font-bold text-[#1a1a1a]">{t("title")}</h3>
               <p className="mt-2 text-zinc-500">{t("subtitle")}</p>

               <ul className="mt-8 space-y-4">
                  <li className="flex items-start gap-3">
                     <Check className="h-5 w-5 text-[#008009] shrink-0 mt-0.5" />
                     <span className="text-sm font-medium text-zinc-700">{t("benefit1")}</span>
                  </li>
                  <li className="flex items-start gap-3">
                     <Check className="h-5 w-5 text-[#008009] shrink-0 mt-0.5" />
                     <span className="text-sm font-medium text-zinc-700">{t("benefit2")}</span>
                  </li>
                  <li className="flex items-start gap-3">
                     <Check className="h-5 w-5 text-[#008009] shrink-0 mt-0.5" />
                     <span className="text-sm font-medium text-zinc-700">{t("benefit3")}</span>
                  </li>
               </ul>

               <div className="mt-8 pt-6 border-t border-zinc-100">
                  <Link
                     href={user ? "/become-a-host/register" : "/auth/login"}
                     className="flex w-full items-center justify-center gap-2 rounded-md bg-[#006ce4] py-4 text-lg font-bold text-white hover:bg-[#0057b7] transition-all active:scale-[0.98] shadow-lg shadow-blue-100 group text-center"
                  >
                     {t("startButton")}
                     <ArrowRight className="h-5 w-5 transition-transform group-hover:translate-x-1" />
                  </Link>
                  <p className="mt-4 text-center text-xs text-zinc-400">{t("footerText")}</p>
               </div>
            </div>
         </div>
      </div>
   );
}
