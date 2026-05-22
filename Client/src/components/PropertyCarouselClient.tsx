"use client";

import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import PropertyCardClient from "./PropertyCardClient";
import { PropertyResponse } from "@/services/propertyService";

interface PropertyCarouselClientProps {
   properties: PropertyResponse[];
}

export default function PropertyCarouselClient({ properties }: PropertyCarouselClientProps) {
   const [currentPage, setCurrentPage] = useState(0);

   // Giới hạn hiển thị tối đa 4 chỗ nghỉ trên một trang đối với màn hình lớn
   // Nếu số lượng chỗ nghỉ ít hơn 4, tự động điều chỉnh số lượng hiển thị bằng số lượng chỗ nghỉ hiện có để card to và cân đối
   const itemsPerPage = Math.min(properties.length, 4);
   const totalPages = Math.ceil(properties.length / itemsPerPage);

   const handleNext = () => {
      if (currentPage < totalPages - 1) {
         setCurrentPage(currentPage + 1);
      }
   };

   const handlePrev = () => {
      if (currentPage > 0) {
         setCurrentPage(currentPage - 1);
      }
   };

   const startIndex = currentPage * itemsPerPage;
   const visibleProperties = properties.slice(startIndex, startIndex + itemsPerPage);

   const getGridColsClass = (count: number) => {
      switch (count) {
         case 1:
            return "grid-cols-1 max-w-md mx-auto";
         case 2:
            return "grid-cols-1 sm:grid-cols-2 max-w-3xl mx-auto";
         case 3:
            return "grid-cols-1 sm:grid-cols-2 lg:grid-cols-3";
         default:
            return "grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4";
      }
   };

   return (
      <div className="relative mt-8">
         {/* Navigation Buttons */}
         {totalPages > 1 && (
            <div className="absolute right-0 -top-16 flex items-center gap-2">
               <button
                  onClick={handlePrev}
                  disabled={currentPage === 0}
                  className="p-2.5 rounded-full bg-white border border-zinc-200 shadow-sm text-zinc-600 cursor-pointer transition-all hover:bg-zinc-50 hover:text-black hover:scale-105 active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100"
                  aria-label="Previous page"
               >
                  <ChevronLeft className="h-5 w-5 stroke-[2.5]" />
               </button>
               <button
                  onClick={handleNext}
                  disabled={currentPage === totalPages - 1}
                  className="p-2.5 rounded-full bg-white border border-zinc-200 shadow-sm text-zinc-600 cursor-pointer transition-all hover:bg-zinc-50 hover:text-black hover:scale-105 active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100"
                  aria-label="Next page"
               >
                  <ChevronRight className="h-5 w-5 stroke-[2.5]" />
               </button>
            </div>
         )}

         {/* Properties Grid Container */}
         <div className="overflow-hidden">
            <div key={currentPage} className={`grid gap-6 ${getGridColsClass(itemsPerPage)}`}>
               {visibleProperties.map((property, idx) => (
                  <PropertyCardClient key={property.id} property={property} index={idx} />
               ))}
            </div>
         </div>

         {/* Page Indicators */}
         {totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-8">
               {Array.from({ length: totalPages }).map((_, idx) => (
                  <button
                     key={idx}
                     onClick={() => setCurrentPage(idx)}
                     className={`h-2 rounded-full cursor-pointer transition-all duration-300 ${
                        currentPage === idx
                           ? "w-6 bg-[#006ce4]"
                           : "w-2 bg-zinc-200 hover:bg-zinc-300"
                     }`}
                     aria-label={`Go to page ${idx + 1}`}
                  />
               ))}
            </div>
         )}
      </div>
   );
}
