"use client";

import { useState, useRef, useEffect } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useRouter } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { motion } from "framer-motion";
import Image from "next/image";
import { DestinationSuggestionResponse } from "@/services/destinationService";

interface DiscoverDestinationsCarouselProps {
   destinations: DestinationSuggestionResponse[];
}

const normalizeKey = (name: string): string => {
   return name
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "") // remove accents
      .replace(/[đĐ]/g, "d")
      .toLowerCase()
      .replace(/city/g, "")
      .replace(/[^a-z0-9]/g, ""); // remove spaces and non-alphanumeric chars
};

const DESTINATION_IMAGES: Record<string, string> = {
   hanoi: "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780019151/HaNoi_z7ahy3.png",
   hochiminh: "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780019498/TP-HCM_z1rn9m.jpg",
   dalat: "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780019811/DaLat_toqzoy.png",
   danang: "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780020710/DaNang_s8vktp.png",
   hoian: "https://images.unsplash.com/photo-1540959733332-eab4deceeaf7?q=80&w=600&auto=format&fit=crop",
   phuquoc:
      "https://images.unsplash.com/photo-1583212292454-1fe6229603b7?q=80&w=600&auto=format&fit=crop",
   quangninh:
      "https://images.unsplash.com/photo-1528127269322-539801943592?q=80&w=600&auto=format&fit=crop",
   halong: "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780020793/HaLong_hzznrp.jpg",
   nhatrang:
      "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=600&auto=format&fit=crop",
   vungtau:
      "https://images.unsplash.com/photo-1519046904884-53103b34b206?q=80&w=600&auto=format&fit=crop",
   sapa: "https://images.unsplash.com/photo-1508873696983-2df519f0397e?q=80&w=600&auto=format&fit=crop",
   hue: "https://images.unsplash.com/photo-1571896349842-33c89424de2d?q=80&w=600&auto=format&fit=crop",
   haiphong:
      "https://images.unsplash.com/photo-1566847438217-76e82d383f84?q=80&w=600&auto=format&fit=crop",
   cantho:
      "https://images.unsplash.com/photo-1543731068-7e0f5beff43a?q=80&w=600&auto=format&fit=crop",

   // International
   paris: "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?q=80&w=600&auto=format&fit=crop",
   london:
      "https://images.unsplash.com/photo-1513635269975-59663e0ca1ad?q=80&w=600&auto=format&fit=crop",
   tokyo: "https://images.unsplash.com/photo-1540959733332-eab4deceeaf7?q=80&w=600&auto=format&fit=crop",
   newyork:
      "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?q=80&w=600&auto=format&fit=crop",
   bangkok:
      "https://images.unsplash.com/photo-1508009603885-50cf7c579365?q=80&w=600&auto=format&fit=crop",
};

const getDestinationImageUrl = (name: string, backendUrl?: string): string => {
   const key = normalizeKey(name);
   return (
      DESTINATION_IMAGES[key] ||
      backendUrl ||
      "https://images.unsplash.com/photo-1528127269322-539801943592?q=80&w=600&auto=format&fit=crop"
   );
};

export default function DiscoverDestinationsCarousel({
   destinations,
}: DiscoverDestinationsCarouselProps) {
   const scrollerRef = useRef<HTMLDivElement>(null);
   const [canScrollLeft, setCanScrollLeft] = useState(false);
   const [canScrollRight, setCanScrollRight] = useState(false);
   const router = useRouter();
   const locale = useLocale();
   const t = useTranslations("Home");

   const handleScroll = () => {
      if (scrollerRef.current) {
         const { scrollLeft, scrollWidth, clientWidth } = scrollerRef.current;
         setCanScrollLeft(scrollLeft > 2);
         setCanScrollRight(scrollLeft < scrollWidth - clientWidth - 2);
      }
   };

   useEffect(() => {
      const scroller = scrollerRef.current;
      if (scroller) {
         handleScroll();
         scroller.addEventListener("scroll", handleScroll);
         window.addEventListener("resize", handleScroll);
         return () => {
            scroller.removeEventListener("scroll", handleScroll);
            window.removeEventListener("resize", handleScroll);
         };
      }
   }, [destinations]);

   const handleNext = () => {
      if (scrollerRef.current) {
         // Scroll by 2/3 of the container width to scroll a few cards at a time smoothly
         scrollerRef.current.scrollBy({
            left: scrollerRef.current.clientWidth * 0.7,
            behavior: "smooth",
         });
      }
   };

   const handlePrev = () => {
      if (scrollerRef.current) {
         scrollerRef.current.scrollBy({
            left: -scrollerRef.current.clientWidth * 0.7,
            behavior: "smooth",
         });
      }
   };

   const handleCardClick = (name: string) => {
      const params = new URLSearchParams();
      params.set("ss", name);
      // Fallback guest configuration matching the SearchBar defaults
      params.set("group_adults", "2");
      params.set("group_children", "0");
      params.set("no_rooms", "1");
      router.push(`/${locale}/search?${params.toString()}`);
   };

   return (
      <div className="relative mt-2 group/carousel">
         {/* Navigation Buttons overlayed on sides */}
         <button
            onClick={handlePrev}
            disabled={!canScrollLeft}
            className="absolute left-0 top-[40%] -translate-y-1/2 -ml-4 p-2.5 rounded-full bg-white border border-zinc-200 shadow-lg text-zinc-600 cursor-pointer transition-all hover:bg-zinc-50 hover:text-black hover:scale-105 active:scale-95 disabled:opacity-0 disabled:pointer-events-none z-10"
            aria-label="Previous page"
         >
            <ChevronLeft className="h-5 w-5 stroke-[2.5]" />
         </button>

         <button
            onClick={handleNext}
            disabled={!canScrollRight}
            className="absolute right-0 top-[40%] -translate-y-1/2 -mr-4 p-2.5 rounded-full bg-white border border-zinc-200 shadow-lg text-zinc-600 cursor-pointer transition-all hover:bg-zinc-50 hover:text-black hover:scale-105 active:scale-95 disabled:opacity-0 disabled:pointer-events-none z-10"
            aria-label="Next page"
         >
            <ChevronRight className="h-5 w-5 stroke-[2.5]" />
         </button>

         {/* Horizontal Scroller Container */}
         <div
            ref={scrollerRef}
            className="flex gap-4 overflow-x-auto overflow-y-hidden touch-pan-y scroll-smooth snap-x snap-mandatory scrollbar-none [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none] pt-4 pb-2"
         >
            {destinations.map((dest, idx) => {
               const imageUrl = getDestinationImageUrl(dest.name, dest.imageUrl);
               const propertyCount = dest.propertyCount || 0;
               const formattedCount = new Intl.NumberFormat(locale).format(propertyCount);

               return (
                  <motion.div
                     key={dest.id}
                     initial={{ opacity: 0, y: 15 }}
                     whileInView={{ opacity: 1, y: 0 }}
                     transition={{ duration: 0.4, delay: idx * 0.05 }}
                     viewport={{ once: true }}
                     onClick={() => handleCardClick(dest.name)}
                     className="w-[140px] xs:w-[160px] sm:w-[185px] md:w-[210px] lg:w-[220px] xl:w-[230px] shrink-0 snap-start group cursor-pointer select-none hover:-translate-y-1 transition-all duration-300"
                  >
                     {/* Image Section */}
                     <div className="relative aspect-4/3 w-full overflow-hidden rounded-xl bg-zinc-100 shadow-[0_4px_15px_rgba(0,0,0,0.04)] border border-zinc-100 group-hover:shadow-[0_12px_25px_rgba(0,0,0,0.08)] transition-all duration-300">
                        <Image
                           src={imageUrl}
                           alt={dest.name}
                           fill
                           sizes="(max-width: 640px) 140px, (max-width: 1024px) 185px, 230px"
                           className="object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                     </div>

                     {/* Info Section */}
                     <div className="mt-3">
                        <h4 className="text-base font-bold text-zinc-900 group-hover:text-[#006ce4] transition-colors leading-tight">
                           {dest.name}
                        </h4>
                        <p className="mt-1 text-xs text-zinc-500 font-medium">
                           {t("propertiesCount", { count: formattedCount })}
                        </p>
                     </div>
                  </motion.div>
               );
            })}
         </div>
      </div>
   );
}
