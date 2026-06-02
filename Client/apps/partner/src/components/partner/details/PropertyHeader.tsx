"use client";

import { Link } from "@/i18n/routing";
import { ArrowLeft, Edit3, Hotel, Star } from "lucide-react";
import { PropertyDetailResponse } from "@/lib/api/propertyService";
import { useTranslations } from "next-intl";

interface PropertyHeaderProps {
   property: PropertyDetailResponse;
}

export default function PropertyHeader({ property }: PropertyHeaderProps) {
   const t = useTranslations("Partner.propertyDetail");
   const tCommon = useTranslations("Partner.dashboard.properties");

   return (
      <header className="mb-8 flex flex-col justify-between gap-4 md:flex-row md:items-center">
         <div>
            {/* Quay lại Dashboard */}
            <Link
               href="/dashboard"
               className="inline-flex items-center gap-2 text-sm font-bold text-zinc-500 hover:text-[#006ce4] mb-3 transition-colors"
            >
               <ArrowLeft className="h-4 w-4" />
               {t("backToDashboard")}
            </Link>

            <div className="flex flex-wrap items-center gap-3">
               <h1 className="text-3xl font-bold tracking-tight text-zinc-900">{property.name}</h1>
               <div className="flex items-center gap-1.5 rounded-full bg-green-50 px-3 py-1 border border-green-100">
                  <div className="h-1.5 w-1.5 rounded-full bg-green-500 animate-pulse" />
                  <span className="text-[10px] font-bold text-green-700 uppercase tracking-wider">
                     {t("statusActive")}
                  </span>
               </div>
            </div>

            <div className="mt-2 flex flex-wrap items-center gap-4 text-sm text-zinc-500 font-medium">
               <div className="flex items-center gap-1">
                  <Hotel className="h-4 w-4 text-[#006ce4]" />
                  <span>{tCommon(`type${property.propertyType}`)}</span>
               </div>
               {property.starRating && property.starRating > 0 && (
                  <div className="flex items-center gap-1">
                     <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                     <span>{property.starRating} sao</span>
                  </div>
               )}
            </div>
         </div>

         <div className="flex items-center gap-3 shrink-0">
            <button className="flex items-center justify-center gap-2 rounded-2xl border border-zinc-200 bg-white px-5 py-3 text-sm font-bold text-zinc-700 hover:border-zinc-300 hover:bg-zinc-50 active:scale-[0.98] transition-all">
               <Edit3 className="h-4 w-4" />
               {t("editProperty")}
            </button>
         </div>
      </header>
   );
}
