"use client";

import Image from "next/image";
import { Link } from "@/i18n/routing";
import { useTranslations } from "next-intl";

export default function AuthBranding() {
   const t = useTranslations("Auth");
   const tc = useTranslations("Common");

   return (
      <div className="relative hidden w-1/2 overflow-hidden lg:block">
         <Image
            src="/images/hero_banner.png"
            alt="Auth Background"
            fill
            className="object-cover transition-transform duration-10000 hover:scale-110"
            priority
         />
         <div className="absolute inset-0 bg-linear-to-br from-[#003580]/80 via-[#003580]/40 to-transparent" />

         <div className="absolute inset-0 flex flex-col justify-between p-16 text-white">
            <Link href="/" className="flex items-center gap-2 text-2xl font-black tracking-tighter">
               <span className="tracking-tight">
                  OmniBooking<span className="text-blue-400">.</span>
               </span>
            </Link>

            <div className="max-w-xl">
               <div className="mb-6 inline-flex items-center rounded-full bg-white/10 px-4 py-1.5 text-xs font-bold uppercase tracking-widest backdrop-blur-md border border-white/10">
                  {t("heroBadge")}
               </div>
               <h2 className="text-4xl lg:text-5xl font-bold leading-tight tracking-tight">
                  {t.rich("heroTitle", {
                     highlight: (chunks) => <span className="text-blue-400">{chunks}</span>,
                     newline: () => <br />,
                  })}
               </h2>
               <p className="mt-6 text-lg text-white/70 leading-relaxed">{t("heroSub")}</p>
            </div>

            <div className="flex items-center gap-6 text-xs font-medium text-white/40">
               <span>© 2026 OmniBooking™</span>
               <div className="h-1 w-1 rounded-full bg-white/20" />
               <span>{tc("privacy")}</span>
               <div className="h-1 w-1 rounded-full bg-white/20" />
               <span>{tc("terms")}</span>
            </div>
         </div>
      </div>
   );
}
