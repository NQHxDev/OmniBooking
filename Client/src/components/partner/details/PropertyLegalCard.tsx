"use client";

import { PropertyDetailResponse } from "@/lib/api/propertyService";
import { ShieldAlert, Award, FileText, UserCheck } from "lucide-react";
import { useTranslations } from "next-intl";

interface PropertyLegalCardProps {
   property: PropertyDetailResponse;
}

export default function PropertyLegalCard({ property }: PropertyLegalCardProps) {
   const t = useTranslations("Partner.propertyDetail.legal");

   return (
      <div className="rounded-[2rem] border border-zinc-100 bg-white p-6 shadow-xs">
         <h2 className="text-lg font-bold text-zinc-900 mb-6 flex items-center gap-2">
            <ShieldAlert className="h-5 w-5 text-emerald-500" />
            {t("title")}
         </h2>

         <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
            {/* ĐKKD */}
            <div className="flex items-start gap-3.5 bg-zinc-50 border border-zinc-100 p-5 rounded-2xl">
               <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-500 border border-emerald-100">
                  <Award className="h-5 w-5" />
               </div>
               <div>
                  <span className="text-xs font-bold text-zinc-400 uppercase tracking-wider block mb-1">
                     {t("businessRegistrationNumber")}
                  </span>
                  <span className="text-sm font-bold text-zinc-800">
                     {property.businessRegistrationNumber || "N/A"}
                  </span>
               </div>
            </div>

            {/* MST */}
            <div className="flex items-start gap-3.5 bg-zinc-50 border border-zinc-100 p-5 rounded-2xl">
               <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-500 border border-blue-100">
                  <FileText className="h-5 w-5" />
               </div>
               <div>
                  <span className="text-xs font-bold text-zinc-400 uppercase tracking-wider block mb-1">
                     {t("taxCode")}
                  </span>
                  <span className="text-sm font-bold text-zinc-800">
                     {property.taxCode || "N/A"}
                  </span>
               </div>
            </div>

            {/* Chủ sở hữu */}
            <div className="flex items-start gap-3.5 bg-zinc-50 border border-zinc-100 p-5 rounded-2xl">
               <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-500 border border-indigo-100">
                  <UserCheck className="h-5 w-5" />
               </div>
               <div>
                  <span className="text-xs font-bold text-zinc-400 uppercase tracking-wider block mb-1">
                     {t("legalOwnerName")}
                  </span>
                  <span className="text-sm font-bold text-zinc-800">
                     {property.legalOwnerName || "N/A"}
                  </span>
               </div>
            </div>
         </div>
      </div>
   );
}
