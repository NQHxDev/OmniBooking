"use client";

import Image from "next/image";
import { MapPin, Clock, CheckCircle } from "lucide-react";
import { PropertyDetailResponse } from "@/lib/api/propertyService";
import { useTranslations } from "next-intl";

interface PropertyInfoCardProps {
   property: PropertyDetailResponse;
}

export default function PropertyInfoCard({ property }: PropertyInfoCardProps) {
   const t = useTranslations("Partner.propertyDetail.basicInfo");

   return (
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-3 mb-8">
         {/* Bên trái: Hình ảnh & Mô tả */}
         <div className="lg:col-span-2 space-y-6">
            <div className="overflow-hidden rounded-[2rem] border border-zinc-100 bg-white p-4 shadow-xs">
               <h2 className="text-lg font-bold text-zinc-900 mb-4 px-2">{t("title")}</h2>

               {/* Ảnh đại diện */}
               <div className="relative h-96 w-full overflow-hidden rounded-2xl bg-zinc-50 border border-zinc-100">
                  {property.imageUrl ? (
                     <Image
                        src={property.imageUrl}
                        alt={property.name}
                        fill
                        className="object-cover"
                        sizes="(max-width: 1024px) 100vw, 66vw"
                        priority
                     />
                  ) : (
                     <div className="flex h-full w-full items-center justify-center bg-linear-to-br from-zinc-800 to-zinc-900 text-white">
                        <span className="text-sm font-bold tracking-wide animate-pulse">
                           Đang xử lý hình ảnh...
                        </span>
                     </div>
                  )}
               </div>

               {/* Mô tả chi tiết */}
               <div className="mt-6 px-2">
                  <h3 className="text-sm font-bold text-zinc-400 uppercase tracking-wider mb-2">
                     {t("description")}
                  </h3>
                  <p className="text-sm leading-relaxed text-zinc-600 font-medium whitespace-pre-line">
                     {property.description}
                  </p>
               </div>
            </div>
         </div>

         {/* Bên phải: Vị trí, Thời gian & Tiện ích */}
         <div className="space-y-6">
            {/* Vị trí & Giờ giấc */}
            <div className="rounded-[2rem] border border-zinc-100 bg-white p-6 shadow-xs space-y-5">
               <div>
                  <h3 className="text-sm font-bold text-zinc-400 uppercase tracking-wider mb-2">
                     {t("location")}
                  </h3>
                  <div className="flex items-start gap-2.5 text-sm text-zinc-700 font-semibold">
                     <MapPin className="h-5 w-5 text-zinc-400 shrink-0 mt-0.5" />
                     <span>
                        {property.address}, {property.city}, {property.country}
                     </span>
                  </div>
               </div>

               <hr className="border-zinc-50" />

               <div>
                  <div className="grid grid-cols-2 gap-4">
                     <div>
                        <span className="text-xs font-bold text-zinc-400 uppercase tracking-wider block mb-1">
                           {t("checkIn")}
                        </span>
                        <div className="flex items-center gap-1.5 text-sm text-zinc-700 font-bold">
                           <Clock className="h-4 w-4 text-[#006ce4]" />
                           <span>{property.checkInTime || "14:00"}</span>
                        </div>
                     </div>
                     <div>
                        <span className="text-xs font-bold text-zinc-400 uppercase tracking-wider block mb-1">
                           {t("checkOut")}
                        </span>
                        <div className="flex items-center gap-1.5 text-sm text-zinc-700 font-bold">
                           <Clock className="h-4 w-4 text-[#006ce4]" />
                           <span>{property.checkOutTime || "12:00"}</span>
                        </div>
                     </div>
                  </div>
               </div>
            </div>

            {/* Danh sách tiện ích */}
            <div className="rounded-[2rem] border border-zinc-100 bg-white p-6 shadow-xs">
               <h3 className="text-sm font-bold text-zinc-400 uppercase tracking-wider mb-4">
                  {t("amenities")}
               </h3>
               {property.amenities && property.amenities.length > 0 ? (
                  <ul className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-1">
                     {property.amenities.map((amenity, idx) => (
                        <li
                           key={idx}
                           className="flex items-center gap-2.5 text-sm text-zinc-700 font-semibold"
                        >
                           <CheckCircle className="h-4 w-4 text-emerald-500 shrink-0" />
                           <span className="truncate">{amenity}</span>
                        </li>
                     ))}
                  </ul>
               ) : (
                  <span className="text-sm font-bold text-zinc-400">Không có tiện ích nào</span>
               )}
            </div>
         </div>
      </div>
   );
}
