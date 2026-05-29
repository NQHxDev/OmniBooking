"use client";

import { useState, useRef, useEffect } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import PropertyCardClient from "./PropertyCardClient";
import { PropertyResponse } from "@/services/propertyService";

interface PropertyCarouselClientProps {
   properties: PropertyResponse[];
}

export default function PropertyCarouselClient({ properties }: PropertyCarouselClientProps) {
   const scrollerRef = useRef<HTMLDivElement>(null);
   const innerRef = useRef<HTMLDivElement>(null);
   const [activeDot, setActiveDot] = useState(0);
   const [canScrollLeft, setCanScrollLeft] = useState(false);
   const [canScrollRight, setCanScrollRight] = useState(false);

   // Calculate pages/dots: on desktop we show 4 items at a time
   const itemsPerPage = 4;
   const totalPages = Math.ceil(properties.length / itemsPerPage);

   const handleScroll = () => {
      if (scrollerRef.current) {
         const { scrollLeft, scrollWidth, clientWidth } = scrollerRef.current;
         setCanScrollLeft(scrollLeft > 2);
         setCanScrollRight(scrollLeft < scrollWidth - clientWidth - 2);

         // Sync the active dot indicators based on scroll progress ratio
         const scrollRange = scrollWidth - clientWidth;
         if (scrollRange > 0 && totalPages > 1) {
            const ratio = scrollLeft / scrollRange;
            setActiveDot(Math.min(Math.round(ratio * (totalPages - 1)), totalPages - 1));
         }
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
   }, [properties, totalPages]);

   // Custom overscroll rubber-band bounce effect that prevents browser history jumps
   useEffect(() => {
      const scroller = scrollerRef.current;
      const inner = innerRef.current;
      if (!scroller || !inner) return;

      let bounceOffset = 0;
      let isDragging = false;
      let touchStartX = 0;
      let touchStartY = 0;
      let wheelTimeout: NodeJS.Timeout | null = null;

      const applyTransform = (offset: number) => {
         const resistance = 0.28;
         const maxBounce = 70; // Maximum stretch in pixels
         const elasticOffset =
            Math.sign(offset) * Math.min(maxBounce, Math.abs(offset) * resistance);

         inner.style.transition = "none";
         inner.style.transform = `translateX(${elasticOffset}px)`;
      };

      const resetTransform = () => {
         inner.style.transition = "transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275)";
         inner.style.transform = "translateX(0px)";
         bounceOffset = 0;
      };

      const handleWheel = (e: WheelEvent) => {
         const { deltaX, deltaY } = e;

         // Only intercept if gesture is primarily horizontal
         if (Math.abs(deltaX) > Math.abs(deltaY)) {
            const { scrollLeft, scrollWidth, clientWidth } = scroller;
            const atLeft = scrollLeft <= 2;
            const atRight = scrollLeft >= scrollWidth - clientWidth - 2;

            if ((deltaX < 0 && atLeft) || (deltaX > 0 && atRight)) {
               // Prevent default browser history back/forward navigation
               e.preventDefault();

               bounceOffset -= deltaX;
               applyTransform(bounceOffset);

               if (wheelTimeout) clearTimeout(wheelTimeout);
               wheelTimeout = setTimeout(resetTransform, 80);
            }
         }
      };

      const handleTouchStart = (e: TouchEvent) => {
         if (e.touches.length === 1) {
            touchStartX = e.touches[0].clientX;
            touchStartY = e.touches[0].clientY;
            isDragging = true;
            inner.style.transition = "none";
         }
      };

      const handleTouchMove = (e: TouchEvent) => {
         if (!isDragging || e.touches.length !== 1) return;

         const currentX = e.touches[0].clientX;
         const currentY = e.touches[0].clientY;
         const diffX = currentX - touchStartX;
         const diffY = currentY - touchStartY;

         if (Math.abs(diffX) > Math.abs(diffY)) {
            const { scrollLeft, scrollWidth, clientWidth } = scroller;
            const atLeft = scrollLeft <= 2;
            const atRight = scrollLeft >= scrollWidth - clientWidth - 2;

            if ((diffX > 0 && atLeft) || (diffX < 0 && atRight)) {
               e.preventDefault();
               bounceOffset = diffX;
               applyTransform(bounceOffset);
            } else {
               if (bounceOffset !== 0) {
                  bounceOffset = 0;
                  inner.style.transform = "translateX(0px)";
               }
            }
         }
      };

      const handleTouchEnd = () => {
         if (isDragging) {
            isDragging = false;
            resetTransform();
         }
      };

      scroller.addEventListener("wheel", handleWheel, { passive: false });
      scroller.addEventListener("touchstart", handleTouchStart, { passive: true });
      scroller.addEventListener("touchmove", handleTouchMove, { passive: false });
      scroller.addEventListener("touchend", handleTouchEnd, { passive: true });
      scroller.addEventListener("touchcancel", handleTouchEnd, { passive: true });

      return () => {
         scroller.removeEventListener("wheel", handleWheel);
         scroller.removeEventListener("touchstart", handleTouchStart);
         scroller.removeEventListener("touchmove", handleTouchMove);
         scroller.removeEventListener("touchend", handleTouchEnd);
         scroller.removeEventListener("touchcancel", handleTouchEnd);
         if (wheelTimeout) clearTimeout(wheelTimeout);
      };
   }, [properties]);

   const handleNext = () => {
      if (scrollerRef.current) {
         scrollerRef.current.scrollBy({
            left: scrollerRef.current.clientWidth,
            behavior: "smooth",
         });
      }
   };

   const handlePrev = () => {
      if (scrollerRef.current) {
         scrollerRef.current.scrollBy({
            left: -scrollerRef.current.clientWidth,
            behavior: "smooth",
         });
      }
   };

   const scrollToPage = (idx: number) => {
      if (scrollerRef.current) {
         const { scrollWidth, clientWidth } = scrollerRef.current;
         const scrollRange = scrollWidth - clientWidth;
         if (scrollRange > 0 && totalPages > 1) {
            const targetScrollLeft = (idx / (totalPages - 1)) * scrollRange;
            scrollerRef.current.scrollTo({ left: targetScrollLeft, behavior: "smooth" });
            setActiveDot(idx);
         }
      }
   };

   return (
      <div className="relative">
         {/* Navigation Buttons */}
         {properties.length > 4 && (
            <div className="absolute right-0 -top-12 flex items-center gap-2">
               <button
                  onClick={handlePrev}
                  disabled={!canScrollLeft}
                  className="p-2.5 rounded-full bg-white border border-zinc-200 shadow-sm text-zinc-600 cursor-pointer transition-all hover:bg-zinc-50 hover:text-black hover:scale-105 active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100"
                  aria-label="Previous page"
               >
                  <ChevronLeft className="h-5 w-5 stroke-[2.5]" />
               </button>
               <button
                  onClick={handleNext}
                  disabled={!canScrollRight}
                  className="p-2.5 rounded-full bg-white border border-zinc-200 shadow-sm text-zinc-600 cursor-pointer transition-all hover:bg-zinc-50 hover:text-black hover:scale-105 active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100"
                  aria-label="Next page"
               >
                  <ChevronRight className="h-5 w-5 stroke-[2.5]" />
               </button>
            </div>
         )}

         {/* Properties Horizontal Scroller Container */}
         <div
            ref={scrollerRef}
            className="overflow-x-auto overflow-y-hidden overscroll-x-contain touch-pan-y scroll-smooth snap-x snap-mandatory scrollbar-none [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none] pt-4 pb-2"
         >
            <div ref={innerRef} className="flex gap-4 w-max min-w-full">
               {properties.map((property, idx) => (
                  <div
                     key={property.id}
                     className="w-[88vw] sm:w-[calc(50%-8px)] lg:w-[calc(25%-12px)] shrink-0 snap-start"
                  >
                     <PropertyCardClient property={property} index={idx} />
                  </div>
               ))}
            </div>
         </div>

         {/* Page Indicators */}
         {totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-5">
               {Array.from({ length: totalPages }).map((_, idx) => (
                  <button
                     key={idx}
                     onClick={() => scrollToPage(idx)}
                     className={`h-2 rounded-full cursor-pointer transition-all duration-300 ${
                        activeDot === idx ? "w-6 bg-[#006ce4]" : "w-2 bg-zinc-200 hover:bg-zinc-300"
                     }`}
                     aria-label={`Go to page ${idx + 1}`}
                  />
               ))}
            </div>
         )}
      </div>
   );
}
