"use client";

import Image from "next/image";
import Link from "next/link";
import { Star, Heart, Check, ChevronRight, Info, ThumbsUp, Plus } from "lucide-react";
import { PropertyDocument } from "@/lib/api/services/propertyService";
import { useTranslations, useLocale } from "next-intl";
import PriceDisplay from "./PriceDisplay";
import { useState } from "react";

interface PropertyCardProps {
   property: PropertyDocument;
   index?: number;
}

export default function PropertyCard({ property, index }: PropertyCardProps) {
   const t = useTranslations("Search");
   const tc = useTranslations("Common");
   const locale = useLocale();
   const isVi = locale === "vi";
   const [isFavorite, setIsFavorite] = useState(false);

   const renderStars = (count: number) => {
      return Array.from({ length: count }).map((_, i) => (
         <Star key={i} className="h-3.5 w-3.5 fill-yellow-400 text-yellow-400" />
      ));
   };

   const getRatingText = (rating: number) => {
      if (rating >= 9) return isVi ? "Xuất sắc" : "Exceptional";
      if (rating >= 8) return isVi ? "Rất tốt" : "Excellent";
      if (rating >= 7) return isVi ? "Tốt" : "Good";
      return isVi ? "Tốt" : "Pleasant";
   };

   // Calculate mock original price (1.5x of minPrice)
   const originalPrice = property.minPrice * 1.5;

   return (
      <div className="bg-white rounded-xl border border-zinc-200 overflow-hidden flex flex-col md:flex-row hover:shadow-md transition-all duration-300 relative p-4 gap-4">
         {/* Image Section */}
         <div className="relative w-full md:w-[240px] h-[200px] shrink-0 rounded-lg overflow-hidden">
            <Image
               src={property.mainImageUrl || "/images/not_found.png"}
               alt={property.name}
               fill
               priority={index !== undefined && index < 2}
               sizes="(max-width: 768px) 100vw, 240px"
               className="object-cover hover:scale-105 transition-transform duration-500"
            />
            {/* Heart Button */}
            <button
               onClick={(e) => {
                  e.stopPropagation();
                  setIsFavorite(!isFavorite);
               }}
               className="absolute top-2.5 right-2.5 h-8 w-8 rounded-full bg-white flex items-center justify-center shadow-md hover:bg-zinc-50 transition-colors z-10 cursor-pointer"
            >
               <Heart
                  className={`h-4.5 w-4.5 transition-colors ${
                     isFavorite
                        ? "fill-red-500 text-red-500 animate-in zoom-in-75 duration-200"
                        : "text-zinc-600 hover:text-red-500"
                  }`}
               />
            </button>
         </div>

         {/* Info Section */}
         <div className="flex-1 flex flex-col justify-between">
            {/* Top row: Title and Ratings */}
            <div className="flex justify-between items-start gap-4">
               <div className="space-y-1">
                  {/* Name and Badges */}
                  <div className="flex flex-wrap items-center gap-x-2 gap-y-1.5">
                     <Link
                        href={`/${locale}/properties/${property.id}`}
                        target="_blank"
                        className="text-lg font-extrabold text-[#006ce4] hover:text-[#005bb8] transition-colors leading-tight cursor-pointer"
                     >
                        {property.name}
                     </Link>
                     <div className="flex items-center gap-0.5">
                        {renderStars(property.starRating || 3)}
                     </div>
                     {/* Thumbs up badge */}
                     <div className="inline-flex items-center gap-0.5 bg-[#ffb700] text-black text-[9px] font-extrabold px-1.5 py-0.5 rounded leading-none">
                        <ThumbsUp className="h-2.5 w-2.5 fill-white text-white" strokeWidth={2.5} />
                        <Plus className="h-2.5 w-2.5 text-white" strokeWidth={4} />
                     </div>
                     {/* Genius Badge */}
                     <div className="inline-flex items-center bg-[#003580] text-white text-[9px] font-extrabold px-1.5 py-0.5 rounded tracking-wide leading-none">
                        Genius
                     </div>
                  </div>

                  {/* Location & Map */}
                  <div className="flex flex-wrap items-center gap-x-1.5 gap-y-0.5 text-xs text-[#006ce4]">
                     <span className="underline cursor-pointer font-semibold">
                        {property.city}, {property.country}
                     </span>
                     <span className="text-zinc-400 select-none">·</span>
                     <span className="underline cursor-pointer font-semibold">
                        {isVi ? "Xem trên bản đồ" : "Show on map"}
                     </span>
                     <span className="text-zinc-400 select-none">·</span>
                     <span className="text-zinc-500">
                        {isVi ? "Cách trung tâm 1.2 km" : "1.2 km from center"}
                     </span>
                  </div>
               </div>

               {/* Rating Badge */}
               <div className="flex items-start gap-2 shrink-0">
                  <div className="text-right">
                     <p className="text-sm font-bold text-zinc-900 leading-tight">
                        {getRatingText(property.averageRating || 7.0)}
                     </p>
                     <p className="text-[10px] text-zinc-500 leading-none mt-1 font-medium">
                        {property.reviewCount || 126} {isVi ? "đánh giá" : "reviews"}
                     </p>
                  </div>
                  <div className="bg-[#003580] text-white font-bold h-8.5 w-8.5 rounded-lg rounded-bl-none flex items-center justify-center text-sm shrink-0 shadow-sm">
                     {(property.averageRating || 7.0).toFixed(1)}
                  </div>
               </div>
            </div>

            {/* Middle row: Badges, Room type & Policies */}
            <div className="mt-2 flex flex-col md:flex-row md:items-end justify-between gap-4">
               <div className="space-y-2.5 flex-1">
                  {/* Promo Badge */}
                  <div>
                     <span className="inline-block bg-[#008009] text-white text-[11px] font-medium px-2 py-0.5 rounded">
                        {isVi ? "Ưu Đãi Mùa Du Lịch" : "Late Escape Deal"}
                     </span>
                  </div>

                  {/* Room Config */}
                  <div className="border-l-2 border-zinc-200/80 pl-3 space-y-1">
                     <p className="font-bold text-xs text-zinc-800">
                        {isVi ? "Phòng Superior Giường Đôi" : "Superior Double Room"}
                     </p>
                     <p className="text-zinc-500 text-[11px] font-medium">
                        {isVi ? "1 giường đôi lớn" : "1 large double bed"}
                     </p>

                     {/* Policies with Green Check */}
                     <div className="space-y-0.5 pt-1">
                        <div className="flex items-center gap-1 text-xs text-[#008009] font-bold">
                           <Check className="h-4 w-4 shrink-0" />
                           <span>{isVi ? "Miễn phí hủy" : "Free cancellation"}</span>
                        </div>
                        <div className="flex items-center gap-1 text-xs text-zinc-600 font-medium">
                           <Check className="h-4 w-4 text-[#008009] shrink-0" />
                           <span className="text-[#008009] font-bold">
                              {isVi ? "Không cần thanh toán trước" : "No prepayment needed"}
                           </span>
                           <span className="text-zinc-500">
                              {isVi ? " - thanh toán tại chỗ nghỉ" : " - pay at the property"}
                           </span>
                        </div>
                     </div>

                     {/* Remaining warning */}
                     <p className="text-red-600 font-bold text-[11px] pt-1">
                        {isVi
                           ? "Chúng tôi còn 2 với giá này"
                           : "Only 2 left at this price on our site"}
                     </p>
                  </div>
               </div>

               {/* Right side: Prices and Call to Action */}
               <div className="flex flex-col items-end shrink-0 text-right">
                  <span className="text-[11px] text-zinc-500 font-medium">
                     1 {t("night")}, 2 {tc("Search.adults" as const)}
                  </span>

                  {/* Original price (strikethrough) */}
                  <span className="text-sm text-red-600 line-through font-medium mt-1">
                     <PriceDisplay
                        amount={originalPrice}
                        size="sm"
                        className="text-sm text-red-600 line-through font-medium"
                     />
                  </span>

                  {/* Final Price */}
                  <div className="flex items-center gap-1 mt-0.5">
                     <PriceDisplay
                        amount={property.minPrice}
                        size="custom"
                        className="text-zinc-600 font-bold text-xl"
                     />
                     <Info className="h-3.5 w-3.5 text-zinc-400 cursor-pointer hover:text-zinc-600 transition-colors" />
                  </div>

                  <span className="text-[10px] text-zinc-500 font-medium">
                     {isVi ? "Đã bao gồm thuế và phí" : "Includes taxes and charges"}
                  </span>

                  <Link
                     href={`/${locale}/properties/${property.id}`}
                     target="_blank"
                     className="mt-3 bg-[#006ce4] hover:bg-[#0057b7] text-white px-4 py-2.5 rounded-md font-bold text-sm transition-all active:scale-[0.98] flex items-center gap-1 cursor-pointer"
                  >
                     <span>{t("viewAvailability")}</span>
                     <ChevronRight className="h-4.5 w-4.5 shrink-0" />
                  </Link>
               </div>
            </div>
         </div>
      </div>
   );
}
