"use client";

import Image from "next/image";
import Link from "next/link";
import { Star, MapPin, Heart, Check } from "lucide-react";
import { motion } from "framer-motion";
import { PropertyResponse } from "@/services/propertyService";
import { useLocale } from "next-intl";
import PriceDisplay from "./PriceDisplay";
import * as React from "react";
import { useSearchParams } from "next/navigation";

export default function PropertyCardClient({
   property,
   index,
}: {
   property: PropertyResponse;
   index: number;
}) {
   const locale = useLocale();
   const isVi = locale === "vi";
   const [isFavorite, setIsFavorite] = React.useState(false);
   const searchParams = useSearchParams();
   const queryStr = searchParams.toString();
   const detailUrl = `/${locale}/properties/${property.id}${queryStr ? `?${queryStr}` : ""}`;

   const checkinParam = searchParams.get("checkin");
   const checkoutParam = searchParams.get("checkout");
   let nights = 1;
   if (checkinParam && checkoutParam) {
      try {
         const checkinDate = new Date(checkinParam);
         const checkoutDate = new Date(checkoutParam);
         const timeDiff = checkoutDate.getTime() - checkinDate.getTime();
         nights = Math.max(1, Math.round(timeDiff / (1000 * 60 * 60 * 24)));
      } catch (e) {
         nights = 1;
      }
   }

   const getRatingText = (rating: number) => {
      if (rating >= 9) return isVi ? "Xuất sắc" : "Exceptional";
      if (rating >= 8) return isVi ? "Rất tốt" : "Excellent";
      if (rating >= 7) return isVi ? "Tốt" : "Good";
      return isVi ? "Tốt" : "Pleasant";
   };

   const formatPropertyType = (type: string) => {
      if (!type) return "";
      const lower = type.toLowerCase();
      if (lower === "hotel") return isVi ? "Khách sạn" : "Hotel";
      if (lower === "apartment") return isVi ? "Căn hộ" : "Apartment";
      if (lower === "resort") return isVi ? "Khu nghỉ dưỡng" : "Resort";
      if (lower === "villa") return isVi ? "Biệt thự" : "Villa";
      return type.charAt(0).toUpperCase() + lower.slice(1);
   };

   // Deterministic rating, stars, & review count based on name length to make each card look unique
   const rating = property.rating || 8.0 + (property.name.length % 15) / 10;
   const starCount = 3 + (property.name.length % 3); // 3 to 5 stars
   const reviewCount = 45 + ((property.name.length * 3) % 250);

   const basePrice = property.price || 50;
   const originalPrice = basePrice * 1.5;

   return (
      <motion.div
         initial={{ opacity: 0, y: 20 }}
         whileInView={{ opacity: 1, y: 0 }}
         transition={{ duration: 0.5, delay: index * 0.1 }}
         viewport={{ once: true }}
         className="group bg-white rounded-2xl overflow-hidden border border-zinc-100/80 shadow-[0_4px_20px_rgba(0,0,0,0.03)] hover:shadow-[0_20px_40px_rgba(0,0,0,0.08)] transition-all duration-300 hover:-translate-y-1 relative"
      >
         <Link href={detailUrl} target="_blank" className="block h-full cursor-pointer select-none">
            {/* Image Section */}
            <div className="relative aspect-4/3 w-full">
               <Image
                  src={property.imageUrl || "/images/not_found.png"}
                  alt={property.name}
                  fill
                  priority={index < 2}
                  sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw"
                  className="object-cover group-hover:scale-110 transition-transform duration-700"
               />
               {/* Heart Wishlist Button */}
               <button
                  onClick={(e) => {
                     e.preventDefault();
                     e.stopPropagation();
                     setIsFavorite(!isFavorite);
                  }}
                  className="absolute top-4 right-4 h-9 w-9 rounded-full bg-white/90 backdrop-blur-sm flex items-center justify-center shadow-md hover:bg-white hover:scale-105 active:scale-95 transition-all z-10 cursor-pointer"
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

            {/* Details Section */}
            <div className="p-5">
               {/* Name */}
               <h4 className="text-base font-bold text-black group-hover:text-[#006ce4] transition-colors line-clamp-1 leading-snug">
                  {property.name}
               </h4>

               {/* Location */}
               <div className="flex items-center gap-1.5 text-zinc-500 mt-1">
                  <MapPin className="h-3.5 w-3.5 text-[#006ce4] shrink-0" />
                  <span className="text-xs font-medium text-zinc-600 hover:text-[#006ce4] hover:underline cursor-pointer transition-colors truncate">
                     {property.city}, {property.country}
                  </span>
               </div>

               {/* Property Type & Stars */}
               <div className="flex items-center gap-2 mt-1.5">
                  <span className="inline-block bg-zinc-50 border border-zinc-200/80 text-zinc-500 text-[10px] font-bold px-2 py-0.5 rounded-full tracking-wide">
                     {formatPropertyType(property.propertyType)}
                  </span>
                  <div className="flex items-center gap-0.5 text-yellow-500">
                     {Array.from({ length: starCount }).map((_, i) => (
                        <Star key={i} className="h-3 w-3 fill-current" />
                     ))}
                  </div>
               </div>

               {/* Rating & Reviews */}
               <div className="flex items-center gap-2 mt-2.5">
                  <div className="bg-[#003580] text-white font-bold h-5.5 w-5.5 rounded text-[10px] flex items-center justify-center shadow-sm shrink-0 leading-none">
                     {rating.toFixed(1)}
                  </div>
                  <span className="text-xs font-bold text-zinc-800">{getRatingText(rating)}</span>
                  <span className="text-zinc-300 select-none">·</span>
                  <span className="text-[10px] text-zinc-500 font-medium">
                     {reviewCount} {isVi ? "đánh giá" : "reviews"}
                  </span>
               </div>

               {/* Premium Badges & Policies */}
               <div className="flex flex-wrap items-center gap-x-2 gap-y-1 mt-2.5">
                  <span className="inline-flex items-center bg-[#003580] text-white text-[9px] font-extrabold px-1.5 py-0.5 rounded tracking-wide leading-none">
                     Genius
                  </span>
                  <span className="inline-flex items-center text-[#008009] text-[10px] font-bold leading-none">
                     <Check className="h-3.5 w-3.5 mr-0.5 shrink-0" />
                     {isVi ? "Miễn phí hủy" : "Free cancellation"}
                  </span>
               </div>

               {/* Pricing Section */}
               <div className="mt-4 pt-3 border-t border-zinc-100/80 flex flex-col gap-1">
                  <div className="flex items-baseline gap-2">
                     {/* Original Price (pale red strike-through) */}
                     <PriceDisplay
                        amount={originalPrice}
                        size="sm"
                        className="text-xs text-red-500/70 line-through font-medium leading-none"
                     />
                     {/* Final Price (modern bold grayish black) */}
                     <PriceDisplay
                        amount={basePrice}
                        size="custom"
                        className="text-zinc-700 font-bold text-lg tracking-tight leading-none"
                     />
                  </div>
                  {/* Stay duration & Taxes info */}
                  <span className="text-[10px] text-zinc-400 font-medium leading-none mt-0.5">
                     {nights} {isVi ? "đêm" : nights > 1 ? "nights" : "night"} ·{" "}
                     {isVi ? "Đã gồm thuế & phí" : "Includes taxes & fees"}
                  </span>
               </div>
            </div>
         </Link>
      </motion.div>
   );
}
