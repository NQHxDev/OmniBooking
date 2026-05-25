"use client";

import React, { useState } from "react";
import { PropertyResponse } from "@/lib/api/propertyService";
import {
   MapPin,
   Hotel,
   ChevronLeft,
   ChevronRight,
   Eye,
   Edit3,
   Trash2,
   Star,
   Loader2,
} from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useTranslations } from "next-intl";

interface PropertyTableProps {
   properties: PropertyResponse[];
}

const ITEMS_PER_PAGE = 4;

export default function PropertyTable({ properties }: PropertyTableProps) {
   const [currentPage, setCurrentPage] = useState(1);
   const t = useTranslations("Partner.dashboard.properties");

   // Pagination logic
   const totalPages = Math.ceil(properties.length / ITEMS_PER_PAGE);
   const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
   const visibleProperties = properties.slice(startIndex, startIndex + ITEMS_PER_PAGE);

   if (properties.length === 0) {
      return (
         <div className="flex flex-col items-center justify-center rounded-3xl border-2 border-dashed border-zinc-200 bg-white p-20 text-center">
            <div className="flex h-20 w-20 items-center justify-center rounded-full bg-blue-50 text-[#006ce4]">
               <Hotel className="h-10 w-10" />
            </div>
            <h3 className="mt-6 text-xl font-bold text-zinc-900">{t("noProperties")}</h3>
            <p className="mt-2 max-w-xs text-zinc-500 font-medium">{t("noPropertiesDesc")}</p>
            <Link
               href="/partner/properties/new"
               className="mt-8 rounded-2xl bg-[#006ce4] px-8 py-3 text-sm font-bold text-white shadow-lg shadow-blue-100 hover:bg-[#0057b7] transition-all"
            >
               {t("registerNow")}
            </Link>
         </div>
      );
   }

   return (
      <div className="space-y-8">
         {/* Grid View - Smaller and more elegant cards */}
         <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
            {visibleProperties.map((property) => (
               <div
                  key={property.id}
                  className="group relative flex flex-col overflow-hidden rounded-[2rem] bg-white shadow-sm border border-zinc-100 hover:shadow-xl hover:shadow-zinc-200 transition-all duration-500"
               >
                  {/* Image Section - Reduced height */}
                  <div className="relative h-48 w-full overflow-hidden bg-zinc-100">
                     {property.imageUrl ? (
                        <>
                           <Image
                              src={property.imageUrl}
                              alt={property.name}
                              fill
                              sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                              className="object-cover transition-transform duration-700 group-hover:scale-110"
                           />
                           <div className="absolute inset-0 bg-linear-to-t from-black/40 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
                        </>
                     ) : (
                        <div className="absolute inset-0 flex flex-col items-center justify-center bg-linear-to-br from-zinc-800 to-zinc-900 text-white p-4">
                           <Loader2 className="h-6 w-6 animate-spin text-zinc-400 mb-2" />
                           <span className="text-xs font-bold tracking-wide animate-pulse text-zinc-300">
                              {t("imageProcessing")}
                           </span>
                        </div>
                     )}

                     {/* Badge Status - Smaller */}
                     <div className="absolute left-4 top-4">
                        <div className="flex items-center gap-1.5 rounded-full bg-white/90 px-3 py-1 backdrop-blur-md shadow-sm">
                           <div className="h-1.5 w-1.5 rounded-full bg-green-500 animate-pulse" />
                           <span className="text-[10px] font-bold text-zinc-900 uppercase tracking-wider">
                              {t("statusActive")}
                           </span>
                        </div>
                     </div>

                     {/* Quick Actions Overlay - Smaller buttons */}
                     <div className="absolute right-4 top-4 flex flex-col gap-1.5 translate-x-12 opacity-0 group-hover:translate-x-0 group-hover:opacity-100 transition-all duration-500">
                        <button className="flex h-8 w-8 items-center justify-center rounded-full bg-white text-zinc-600 hover:bg-[#006ce4] hover:text-white shadow-lg transition-colors">
                           <Edit3 className="h-3.5 w-3.5" />
                        </button>
                        <button className="flex h-8 w-8 items-center justify-center rounded-full bg-white text-zinc-600 hover:bg-red-500 hover:text-white shadow-lg transition-colors">
                           <Trash2 className="h-3.5 w-3.5" />
                        </button>
                     </div>
                  </div>

                  {/* Content Section - More compact */}
                  <div className="flex flex-1 flex-col p-6">
                     <div className="flex items-start justify-between">
                        <div className="flex-1 min-w-0">
                           <div className="flex items-center gap-1.5 text-[11px] font-bold text-[#006ce4] uppercase tracking-widest mb-1.5">
                              <Hotel className="h-3 w-3" />
                              {t(`type${property.propertyType}`)}
                           </div>
                           <h3 className="text-lg font-bold text-zinc-900 group-hover:text-[#006ce4] transition-colors leading-tight truncate">
                              {property.name}
                           </h3>
                        </div>
                        <div className="flex items-center gap-1 rounded-lg bg-zinc-50 px-2 py-1 border border-zinc-100 ml-2">
                           <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
                           <span className="text-[11px] font-bold text-zinc-900">4.8</span>
                        </div>
                     </div>

                     <div className="mt-4 flex flex-wrap gap-2 text-[12px] font-medium text-zinc-500">
                        <div className="flex items-center gap-1.5 bg-zinc-50 px-3 py-1.5 rounded-xl border border-zinc-100 truncate max-w-full">
                           <MapPin className="h-3.5 w-3.5 text-zinc-400 shrink-0" />
                           <span className="truncate">
                              {property.city}, {property.country}
                           </span>
                        </div>
                     </div>

                     <div className="mt-6 flex items-center justify-between border-t border-zinc-50 pt-5">
                        <div className="flex flex-col">
                           <span className="text-[10px] font-bold uppercase tracking-widest text-zinc-400">
                              {t("inventory")}
                           </span>
                           <span className="text-sm font-bold text-zinc-900">12 {t("rooms")}</span>
                        </div>
                        <Link
                           href={`/partner/properties/${property.id}`}
                           className="flex items-center gap-1.5 rounded-xl bg-zinc-900 px-4 py-2 text-[11px] font-bold text-white hover:bg-zinc-800 transition-all shadow-md shadow-zinc-100 active:scale-[0.98]"
                        >
                           <Eye className="h-3.5 w-3.5" />
                           {t("details")}
                        </Link>
                     </div>
                  </div>
               </div>
            ))}
         </div>

         {/* Pagination Controls */}
         {totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-zinc-100 pt-8">
               <div className="text-sm font-medium text-zinc-500">
                  {t("showing")}{" "}
                  <span className="font-bold text-zinc-900">
                     {startIndex + 1}-
                     {Math.min(startIndex + visibleProperties.length, properties.length)}
                  </span>{" "}
                  {t("of")} <span className="font-bold text-zinc-900">{properties.length}</span>{" "}
                  {t("title").toLowerCase()}
               </div>
               <div className="flex items-center gap-2">
                  <button
                     onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                     disabled={currentPage === 1}
                     className="flex h-12 w-12 items-center justify-center rounded-2xl border border-zinc-200 bg-white text-zinc-600 hover:border-[#006ce4] hover:text-[#006ce4] disabled:opacity-30 disabled:hover:border-zinc-200 disabled:hover:text-zinc-600 transition-all"
                  >
                     <ChevronLeft className="h-5 w-5" />
                  </button>
                  <div className="flex gap-1">
                     {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                        <button
                           key={page}
                           onClick={() => setCurrentPage(page)}
                           className={`h-12 w-12 rounded-2xl text-sm font-bold transition-all ${
                              currentPage === page
                                 ? "bg-[#006ce4] text-white shadow-lg shadow-blue-100"
                                 : "bg-white border border-zinc-200 text-zinc-600 hover:border-zinc-300"
                           }`}
                        >
                           {page}
                        </button>
                     ))}
                  </div>
                  <button
                     onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                     disabled={currentPage === totalPages}
                     className="flex h-12 w-12 items-center justify-center rounded-2xl border border-zinc-200 bg-white text-zinc-600 hover:border-[#006ce4] hover:text-[#006ce4] disabled:opacity-30 disabled:hover:border-zinc-200 disabled:hover:text-zinc-600 transition-all"
                  >
                     <ChevronRight className="h-5 w-5" />
                  </button>
               </div>
            </div>
         )}
      </div>
   );
}
