"use client";

// import { motion } from "framer-motion";

export default function Loading() {
   return (
      <div className="fixed inset-0 z-9999 flex flex-col bg-white overflow-hidden">
         {/* Top Brand Progress Bar */}
         <div className="fixed top-0 left-0 right-0 h-1 bg-zinc-50 overflow-hidden">
            <div className="h-full bg-linear-to-r from-[#003580] via-[#006ce4] to-[#ffb700] w-1/2 animate-shimmer" />
         </div>

         {/* Main Content Area: Mocking the Layout (Skeleton) */}
         <div className="flex flex-col items-center w-full max-w-6xl mx-auto px-8 pt-24 space-y-12">
            {/* Header Skeleton */}
            <div className="w-full flex justify-between items-center mb-8">
               <div className="h-8 w-40 bg-zinc-100 rounded-lg animate-pulse" />
               <div className="flex gap-4">
                  <div className="h-10 w-24 bg-zinc-100 rounded-full animate-pulse" />
                  <div className="h-10 w-32 bg-zinc-100 rounded-full animate-pulse" />
               </div>
            </div>

            {/* Hero Section Skeleton */}
            <div className="w-full grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
               <div className="space-y-6">
                  <div className="h-16 w-3/4 bg-zinc-100 rounded-2xl animate-pulse" />
                  <div className="h-6 w-1/2 bg-zinc-50 rounded-lg animate-pulse" />
                  <div className="h-14 w-full bg-zinc-100 rounded-xl animate-pulse" />
               </div>
               <div className="aspect-4/3 w-full bg-zinc-100 rounded-[2.5rem] animate-pulse relative overflow-hidden">
                  {/* Internal shimmer */}
                  <div className="absolute inset-0 bg-linear-to-r from-transparent via-white/40 to-transparent -translate-x-full animate-[shimmer_2s_infinite]" />
               </div>
            </div>

            {/* Grid Skeleton */}
            <div className="w-full grid grid-cols-1 md:grid-cols-3 gap-8">
               {[1, 2, 3].map((i) => (
                  <div key={i} className="space-y-4">
                     <div className="aspect-video w-full bg-zinc-50 rounded-3xl animate-pulse" />
                     <div className="h-4 w-2/3 bg-zinc-100 rounded-md animate-pulse" />
                     <div className="h-4 w-1/2 bg-zinc-50 rounded-md animate-pulse" />
                  </div>
               ))}
            </div>
         </div>

         {/* Central Branding Overlay (Floats above skeleton) */}
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
