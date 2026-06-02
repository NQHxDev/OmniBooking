import Link from "next/link";
import { useTranslations, useLocale } from "next-intl";

export default function Footer() {
   const t = useTranslations("Footer");
   const locale = useLocale();

   return (
      <footer
         className="w-full bg-[#f5f5f5] text-zinc-700 mt-2 border-t border-zinc-200 pt-10 pb-8"
         aria-label="Footer"
      >
         <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            {/* Links Grid with Semantic Navigation */}
            <nav
               className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-8 text-xs leading-normal"
               aria-label="Footer navigation links"
            >
               {/* Column 1: Support */}
               <div className="flex flex-col gap-2.5">
                  <h4 className="font-bold text-zinc-900 text-sm mb-1">{t("support")}</h4>
                  <ul className="flex flex-col gap-2.5">
                     <li>
                        <Link
                           href={`/${locale}/profile`}
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("supportItems.manageTrips")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href={`/${locale}/customer-service`}
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("supportItems.customerService")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href={`/${locale}/safety-center`}
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("supportItems.safetyCenter")}
                        </Link>
                     </li>
                  </ul>
               </div>

               {/* Column 2: Discover More */}
               <div className="flex flex-col gap-2.5">
                  <h4 className="font-bold text-zinc-900 text-sm mb-1">{t("explore")}</h4>
                  <ul className="flex flex-col gap-2.5">
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("exploreItems.genius")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("exploreItems.deals")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("exploreItems.articles")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("exploreItems.business")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("exploreItems.awards")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("exploreItems.carRentals")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("exploreItems.flights")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("exploreItems.restaurants")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("exploreItems.agents")}
                        </Link>
                     </li>
                  </ul>
               </div>

               {/* Column 3: Terms and Settings */}
               <div className="flex flex-col gap-2.5">
                  <h4 className="font-bold text-zinc-900 text-sm mb-1">{t("terms")}</h4>
                  <ul className="flex flex-col gap-2.5">
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("termsItems.privacy")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("termsItems.terms")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("termsItems.accessibility")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("termsItems.disputes")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("termsItems.slavery")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("termsItems.humanRights")}
                        </Link>
                     </li>
                  </ul>
               </div>

               {/* Column 4: Partners */}
               <div className="flex flex-col gap-2.5">
                  <h4 className="font-bold text-zinc-900 text-sm mb-1">{t("partners")}</h4>
                  <ul className="flex flex-col gap-2.5">
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("partnersItems.extranet")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("partnersItems.help")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href={`/${locale}/become-a-host`}
                           className="hover:underline hover:text-[#006ce4] transition-colors font-semibold"
                        >
                           {t("partnersItems.listProperty")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("partnersItems.affiliate")}
                        </Link>
                     </li>
                  </ul>
               </div>

               {/* Column 5: About Us */}
               <div className="flex flex-col gap-2.5">
                  <h4 className="font-bold text-zinc-900 text-sm mb-1">{t("about")}</h4>
                  <ul className="flex flex-col gap-2.5">
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("aboutItems.aboutCompany")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("aboutItems.howWeWork")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("aboutItems.sustainability")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("aboutItems.press")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("aboutItems.careers")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("aboutItems.investors")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("aboutItems.contact")}
                        </Link>
                     </li>
                     <li>
                        <Link
                           href="#"
                           rel="nofollow"
                           className="hover:underline hover:text-[#006ce4] transition-colors"
                        >
                           {t("aboutItems.contentGuidelines")}
                        </Link>
                     </li>
                  </ul>
               </div>
            </nav>

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
