"use client";

import Link from "next/link";
import { HelpCircle } from "lucide-react";
import { useTranslations } from "next-intl";

export default function PartnerNavbar() {
   const t = useTranslations("Common");

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
                  <span>{t("support")}</span>
               </div>
            </div>
         </div>
      </header>
   );
}
