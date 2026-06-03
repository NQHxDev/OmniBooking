"use client";

import { useEffect, useState } from "react";

export default function GlobalLoadingOverlay({ isVisible }: { isVisible: boolean }) {
   const [render, setRender] = useState(isVisible);
   const [fade, setFade] = useState(false);

   useEffect(() => {
      if (isVisible) {
         const timer = setTimeout(() => {
            setRender(true);
            setFade(false);
         }, 0);
         return () => clearTimeout(timer);
      } else {
         const timer1 = setTimeout(() => {
            setFade(true);
         }, 0);
         const timer2 = setTimeout(() => {
            setRender(false);
         }, 300); // 300ms transition duration
         return () => {
            clearTimeout(timer1);
            clearTimeout(timer2);
         };
      }
   }, [isVisible]);

   if (!render) return null;

   return (
      <div
         className={`fixed inset-0 z-[9999] flex flex-col bg-white overflow-hidden transition-opacity duration-300 ${
            fade ? "opacity-0 pointer-events-none" : "opacity-100"
         }`}
      >
         {/* Top Brand Progress Bar */}
         <div className="fixed top-0 left-0 right-0 h-1 bg-zinc-50 overflow-hidden">
            <div className="h-full bg-linear-to-r from-[#003580] via-[#006ce4] to-[#ffb700] w-1/2 animate-shimmer" />
         </div>

         {/* Central Branding Overlay */}
         <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div className="flex flex-col items-center">
               <div className="bg-white/80 backdrop-blur-xl p-8 rounded-3xl border border-zinc-100 shadow-[0_32px_64px_-12px_rgba(0,0,0,0.05)] flex flex-col items-center gap-4">
                  {/* Animated Logo Mark */}
                  <div className="relative h-16 w-16 flex items-center justify-center">
                     <div className="absolute inset-0 border-4 border-[#006ce4]/10 rounded-full" />
                     <div className="absolute inset-0 border-4 border-[#006ce4] rounded-full border-t-transparent animate-spin" />
                     <span className="text-2xl font-black text-[#003580]">O</span>
                  </div>

                  <div className="text-center">
                     <h2 className="text-xl font-black tracking-tighter text-[#003580]">
                        OmniBooking<span className="text-[#006ce4]">.</span>
                     </h2>
                  </div>
               </div>
            </div>
         </div>
      </div>
   );
}
