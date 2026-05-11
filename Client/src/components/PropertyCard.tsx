"use client";

import Image from "next/image";
import { Star, MapPin, Wifi, Car, Waves, Utensils } from "lucide-react";
import { PropertyDocument } from "@/lib/api/services/propertyService";
import { useTranslations } from "next-intl";

interface PropertyCardProps {
   property: PropertyDocument;
}

export default function PropertyCard({ property }: PropertyCardProps) {
   const t = useTranslations("Search");

   const renderStars = (count: number) => {
      return Array.from({ length: count }).map((_, i) => (
         <Star key={i} className="h-3 w-3 fill-yellow-400 text-yellow-400" />
      ));
   };

   const getRatingText = (rating: number) => {
      if (rating >= 9) return t("rating.exceptional");
      if (rating >= 8) return t("rating.excellent");
      return t("rating.good");
   };

   return (
      <div className="bg-white rounded-2xl border border-zinc-200 overflow-hidden flex flex-col md:flex-row hover:shadow-xl transition-all duration-300 group cursor-pointer">
         {/* Image Section */}
         <div className="relative w-full md:w-72 h-48 md:h-auto overflow-hidden">
            <Image
               src={
                  property.mainImageUrl ||
                  "https://images.unsplash.com/photo-1566073771259-6a8506099945?q=80&w=2070&auto=format&fit=crop"
               }
               alt={property.name}
               fill
               className="object-cover group-hover:scale-105 transition-transform duration-500"
            />
            <div className="absolute top-3 left-3 bg-white/90 backdrop-blur px-2 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider text-[#003580]">
               {property.propertyType}
            </div>
         </div>

         {/* Content Section */}
         <div className="flex-1 p-5 flex flex-col justify-between">
            <div>
               <div className="flex justify-between items-start mb-1">
                  <div className="flex flex-col gap-1">
                     <h3 className="text-lg font-bold text-[#006ce4] group-hover:underline">
                        {property.name}
                     </h3>
                     <div className="flex items-center gap-0.5">
                        {renderStars(property.starRating || 0)}
                     </div>
                  </div>
                  <div className="flex flex-col items-end">
                     <div className="flex items-center gap-2">
                        <div className="flex flex-col items-end">
                           <span className="text-sm font-bold text-zinc-900">
                              {getRatingText(property.averageRating)}
                           </span>
                           <span className="text-[11px] text-zinc-500">
                              {property.reviewCount} {t("rating.reviews")}
                           </span>
                        </div>
                        <div className="bg-[#003580] text-white font-bold h-8 w-8 rounded-lg rounded-bl-none flex items-center justify-center text-sm">
                           {property.averageRating.toFixed(1)}
                        </div>
                     </div>
                  </div>
               </div>

               <div className="flex items-center gap-1 text-xs text-zinc-500 mb-3 hover:text-black transition-colors">
                  <MapPin className="h-3 w-3" />
                  <span className="underline">
                     {property.city}, {property.country}
                  </span>
                  <span className="mx-1">·</span>
                  <span>{t("distanceFromCenter", { distance: "1.2km" })}</span>
               </div>

               <div className="flex flex-wrap gap-2 mb-4">
                  {property.amenities.slice(0, 4).map((amenity) => (
                     <div
                        key={amenity}
                        className="flex items-center gap-1 text-[11px] font-medium text-zinc-600 bg-zinc-50 px-2 py-1 rounded-md border border-zinc-100"
                     >
                        {amenity.toLowerCase().includes("wifi") && <Wifi className="h-3 w-3" />}
                        {amenity.toLowerCase().includes("đỗ xe") && <Car className="h-3 w-3" />}
                        {amenity.toLowerCase().includes("hồ bơi") && <Waves className="h-3 w-3" />}
                        {amenity.toLowerCase().includes("nhà hàng") && (
                           <Utensils className="h-3 w-3" />
                        )}
                        {amenity}
                     </div>
                  ))}
               </div>
            </div>

            <div className="flex justify-end items-end gap-4 border-t border-zinc-100 pt-4">
               <div className="flex flex-col items-end">
                  <span className="text-[11px] text-zinc-500">
                     1 {t("night")}, 2 {t("Search.adults" as const)}
                  </span>
                  <div className="flex flex-col items-end">
                     <span className="text-xl font-bold text-zinc-900">
                        VND {property.minPrice.toLocaleString()}
                     </span>
                     <span className="text-[10px] text-zinc-400">{t("taxesIncluded")}</span>
                  </div>
               </div>
               <button className="bg-[#006ce4] hover:bg-[#0057b7] text-white px-6 py-2.5 rounded-lg font-bold text-sm transition-all active:scale-95 shadow-lg shadow-blue-100">
                  {t("viewAvailability")}
               </button>
            </div>
         </div>
      </div>
   );
}
