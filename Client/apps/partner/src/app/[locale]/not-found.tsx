"use client";

import { Link } from "@/i18n/routing";
import { useTranslations } from "next-intl";
import { motion } from "framer-motion";
import { LayoutDashboard, ArrowLeft } from "lucide-react";

export default function NotFound() {
   const t = useTranslations("Partner.notFound");
   const tCommon = useTranslations("Common");

   return (
      <div className="flex min-h-screen flex-col bg-white font-sans text-zinc-900 selection:bg-blue-100 selection:text-blue-900">
         {/* Sticky Brand Header */}
         <header className="bg-[#003580] text-white py-4 px-6 sticky top-0 z-50 shadow-md">
            <div className="mx-auto max-w-7xl flex items-center justify-between">
               <Link
                  href="/dashboard"
                  className="text-2xl font-bold tracking-tight flex items-center gap-2"
               >
                  OmniBooking.com
               </Link>
            </div>
         </header>

         {/* Main Content Area */}
         <main className="flex flex-1 flex-col items-center justify-center px-6 py-12 text-center">
            {/* SVG Document Search Illustration */}
            <div className="relative mb-8 h-48 w-48 sm:h-56 sm:w-56 animate-in fade-in zoom-in duration-700 flex items-center justify-center">
               <svg viewBox="0 0 200 200" className="w-full h-full">
                  {/* Background soft circle */}
                  <circle cx="100" cy="100" r="80" fill="#f0f5ff" />
                  <circle cx="100" cy="100" r="60" fill="#e0ebff" />

                  {/* Document Icon */}
                  <motion.g
                     animate={{ y: [0, -6, 0] }}
                     transition={{ repeat: Infinity, duration: 3, ease: "easeInOut" }}
                  >
                     <rect
                        x="70"
                        y="50"
                        width="60"
                        height="80"
                        rx="8"
                        fill="#ffffff"
                        stroke="#003580"
                        strokeWidth="4"
                        className="drop-shadow-md"
                     />
                     <line
                        x1="85"
                        y1="75"
                        x2="115"
                        y2="75"
                        stroke="#cbd5e1"
                        strokeWidth="4"
                        strokeLinecap="round"
                     />
                     <line
                        x1="85"
                        y1="90"
                        x2="115"
                        y2="90"
                        stroke="#cbd5e1"
                        strokeWidth="4"
                        strokeLinecap="round"
                     />
                     <line
                        x1="85"
                        y1="105"
                        x2="100"
                        y2="105"
                        stroke="#cbd5e1"
                        strokeWidth="4"
                        strokeLinecap="round"
                     />

                     {/* Folded corner */}
                     <path
                        d="M116 52 L128 64 L116 64 Z"
                        fill="#006ce4"
                        stroke="#006ce4"
                        strokeWidth="2"
                        strokeLinejoin="round"
                     />
                  </motion.g>

                  {/* Magnifying Glass */}
                  <motion.g
                     animate={{ x: [0, 4, 0], y: [0, 4, 0] }}
                     transition={{ repeat: Infinity, duration: 4, ease: "easeInOut" }}
                  >
                     <circle
                        cx="120"
                        cy="120"
                        r="22"
                        fill="#ffffff"
                        stroke="#006ce4"
                        strokeWidth="4.5"
                        className="drop-shadow-lg"
                     />
                     <line
                        x1="135"
                        y1="135"
                        x2="155"
                        y2="155"
                        stroke="#006ce4"
                        strokeWidth="5.5"
                        strokeLinecap="round"
                     />

                     {/* Question mark inside magnifier */}
                     <text
                        x="120"
                        y="125"
                        fill="#003580"
                        fontSize="16"
                        fontWeight="bold"
                        textAnchor="middle"
                     >
                        ?
                     </text>
                  </motion.g>
               </svg>
            </div>

            {/* Error Message Header */}
            <div className="max-w-md animate-in fade-in slide-in-from-bottom-4 duration-1000 delay-200">
               <h1 className="text-4xl font-black tracking-tighter text-[#1a1a1a] sm:text-5xl mb-6 leading-[1.2]">
                  {t("title")} <br />
                  <span className="bg-linear-to-r from-[#006ce4] to-[#003580] bg-clip-text text-transparent">
                     {t("subtitle")}
                  </span>
               </h1>

               <p className="text-base text-zinc-500 mb-10 leading-relaxed">{t("description")}</p>

               {/* Action Buttons */}
               <div className="flex flex-col sm:flex-row items-center justify-center gap-4 w-full">
                  <Link
                     href="/dashboard"
                     className="inline-flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl bg-[#006ce4] px-8 py-4 text-sm font-bold text-white shadow-xl shadow-blue-100 transition-all hover:bg-[#0057b7] hover:shadow-blue-200 active:scale-[0.98]"
                  >
                     <LayoutDashboard className="h-4 w-4" />
                     {t("backToDashboard")}
                  </Link>
                  <button
                     onClick={() => window.history.back()}
                     className="inline-flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl border border-zinc-200 bg-white px-8 py-4 text-sm font-bold text-zinc-900 transition-all hover:bg-zinc-50 active:scale-[0.98]"
                  >
                     <ArrowLeft className="h-4 w-4" />
                     {t("goBack")}
                  </button>
               </div>
            </div>
         </main>

         {/* Footer */}
         <footer className="py-8 text-center text-xs text-zinc-400 border-t border-zinc-100">
            <p>© 1996-2026 OmniBooking.com™. {tCommon("allRightsReserved")}.</p>
         </footer>
      </div>
   );
}
