"use client";

import Link from "next/link";
import { useTranslations, useLocale } from "next-intl";

export default function Footer() {
   const t = useTranslations("Footer");
   const locale = useLocale();
   const isVi = locale === "vi";

   // Vector flags for premium rendering
   const VietnamFlag = (
      <svg
         viewBox="0 0 30 20"
         className="h-4.5 w-6 rounded-xs shadow-sm inline-block shrink-0 border border-zinc-200"
      >
         <rect width="30" height="20" fill="#da251d" />
         <polygon
            points="15,4 16.2,8.5 21,8.5 17.1,11.3 18.6,15.8 15,13 11.4,15.8 12.9,11.3 9,8.5 13.8,8.5"
            fill="#ffff00"
         />
      </svg>
   );

   const UKFlag = (
      <svg
         viewBox="0 0 60 30"
         className="h-4.5 w-6 rounded-xs shadow-sm inline-block shrink-0 border border-zinc-200"
      >
         <clipPath id="s">
            <path d="M0,0 L60,0 L60,30 L0,30 Z" />
         </clipPath>
         <path d="M0,0 L60,30 M60,0 L0,30" stroke="#fff" strokeWidth="6" />
         <path d="M0,0 L60,30 M60,0 L0,30" stroke="#012169" strokeWidth="4" />
         <path d="M0,0 L60,30 M60,0 L0,30" stroke="#c8102e" strokeWidth="2" clipPath="url(#s)" />
         <path d="M30,0 L30,30 M0,15 L60,15" stroke="#fff" strokeWidth="10" />
         <path d="M30,0 L30,30 M0,15 L60,15" stroke="#c8102e" strokeWidth="6" />
      </svg>
   );

   return (
      <footer className="w-full bg-[#f5f5f5] text-zinc-700 mt-2 border-t border-zinc-200 pt-10 pb-8">
         <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            {/* Links Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-8 text-xs leading-normal">
               {/* Column 1: Support */}
               <div className="flex flex-col gap-2.5">
                  <h4 className="font-bold text-zinc-900 text-sm mb-1">{t("support")}</h4>
                  <Link
                     href={`/${locale}/profile`}
                     className="hover:underline hover:text-[#006ce4] transition-colors"
                  >
                     {t("supportItems.manageTrips")}
                  </Link>
                  <Link
                     href={`/${locale}/customer-service`}
                     className="hover:underline hover:text-[#006ce4] transition-colors"
                  >
                     {t("supportItems.customerService")}
                  </Link>
                  <Link
                     href={`/${locale}/safety-center`}
                     className="hover:underline hover:text-[#006ce4] transition-colors"
                  >
                     {t("supportItems.safetyCenter")}
                  </Link>
               </div>

               {/* Column 2: Discover More */}
               <div className="flex flex-col gap-2.5">
                  <h4 className="font-bold text-zinc-900 text-sm mb-1">{t("explore")}</h4>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("exploreItems.genius")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("exploreItems.deals")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("exploreItems.articles")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("exploreItems.business")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("exploreItems.awards")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("exploreItems.carRentals")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("exploreItems.flights")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("exploreItems.restaurants")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("exploreItems.agents")}
                  </Link>
               </div>

               {/* Column 3: Terms and Settings */}
               <div className="flex flex-col gap-2.5">
                  <h4 className="font-bold text-zinc-900 text-sm mb-1">{t("terms")}</h4>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("termsItems.privacy")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("termsItems.terms")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("termsItems.accessibility")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("termsItems.disputes")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("termsItems.slavery")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("termsItems.humanRights")}
                  </Link>
               </div>

               {/* Column 4: Partners */}
               <div className="flex flex-col gap-2.5">
                  <h4 className="font-bold text-zinc-900 text-sm mb-1">{t("partners")}</h4>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("partnersItems.extranet")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("partnersItems.help")}
                  </Link>
                  <Link
                     href={`/${locale}/become-a-host`}
                     className="hover:underline hover:text-[#006ce4] transition-colors font-semibold"
                  >
                     {t("partnersItems.listProperty")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("partnersItems.affiliate")}
                  </Link>
               </div>

               {/* Column 5: About Us */}
               <div className="flex flex-col gap-2.5">
                  <h4 className="font-bold text-zinc-900 text-sm mb-1">{t("about")}</h4>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("aboutItems.aboutCompany")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("aboutItems.howWeWork")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("aboutItems.sustainability")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("aboutItems.press")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("aboutItems.careers")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("aboutItems.investors")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("aboutItems.contact")}
                  </Link>
                  <Link href="#" className="hover:underline hover:text-[#006ce4] transition-colors">
                     {t("aboutItems.contentGuidelines")}
                  </Link>
               </div>
            </div>

            {/* Language & Currency Indicator */}
            <div className="mt-10 flex flex-wrap items-center gap-6 text-sm font-semibold text-zinc-800">
               <div className="flex items-center gap-2 cursor-pointer hover:bg-zinc-200/50 px-2.5 py-1.5 rounded-md transition-colors select-none">
                  {isVi ? VietnamFlag : UKFlag}
                  <span>{isVi ? "Tiếng Việt" : "English (US)"}</span>
               </div>
               <div className="flex items-center gap-1.5 cursor-pointer hover:bg-zinc-200/50 px-2.5 py-1.5 rounded-md transition-colors select-none">
                  <span className="font-bold text-zinc-700">{isVi ? "₫" : "$"}</span>
                  <span>{isVi ? "VND" : "USD"}</span>
               </div>
            </div>

            {/* Separator line */}
            <div className="my-8 border-t border-zinc-200" />

            {/* Corporate Description & Copyright */}
            <div className="text-center text-xs text-zinc-500 max-w-4xl mx-auto flex flex-col gap-2">
               <p className="leading-relaxed">{t("holdingsDesc")}</p>
               <p className="font-medium text-zinc-600">{t("copyright")}</p>
            </div>

            {/* Sister Company / Holdings Brand Logos Row */}
            <div className="mt-8 flex flex-wrap items-center justify-center gap-x-10 gap-y-4 pt-4 select-none">
               {/* Brand 1: OmniBooking.com */}
               <span className="text-zinc-800 font-extrabold text-lg tracking-tight">
                  Omni<span className="text-[#006ce4]">Booking.com</span>
               </span>

               {/* Brand 2: priceline */}
               <span className="text-[#0077c5] font-semibold italic text-base tracking-tighter">
                  priceline
               </span>

               {/* Brand 3: KAYAK */}
               <span className="bg-[#FF690F] text-white font-black px-2 py-0.5 rounded-sm tracking-wider text-[11px] uppercase">
                  K A Y A K
               </span>

               {/* Brand 4: agoda */}
               <span className="relative flex flex-col items-center group cursor-pointer">
                  <span className="text-zinc-700 font-bold text-base tracking-tight leading-none">
                     agoda
                  </span>
                  <span className="flex gap-[3px] mt-[3px]">
                     <span className="h-[4px] w-[4px] rounded-full bg-green-500"></span>
                     <span className="h-[4px] w-[4px] rounded-full bg-purple-500"></span>
                     <span className="h-[4px] w-[4px] rounded-full bg-yellow-400"></span>
                     <span className="h-[4px] w-[4px] rounded-full bg-orange-400"></span>
                     <span className="h-[4px] w-[4px] rounded-full bg-blue-500"></span>
                  </span>
               </span>

               {/* Brand 5: OpenTable */}
               <span className="flex items-center gap-1 text-zinc-700 font-bold text-sm tracking-tight">
                  <span className="h-3 w-3 rounded-full bg-[#E11936]"></span>
                  OpenTable
               </span>
            </div>
         </div>
      </footer>
   );
}
