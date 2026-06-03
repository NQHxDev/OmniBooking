"use client";

import dynamic from "next/dynamic";
import { Loader2 } from "lucide-react";

export const MapView = dynamic(() => import("./MapView"), {
   ssr: false,
   loading: () => (
      <div className="w-full h-full bg-zinc-100 flex items-center justify-center animate-pulse min-h-[400px]">
         <Loader2 className="h-8 w-8 text-[#006ce4] animate-spin" />
      </div>
   ),
});

export const LocationPicker = dynamic(() => import("./LocationPicker"), {
   ssr: false,
   loading: () => (
      <div className="w-full h-full bg-zinc-100 flex items-center justify-center animate-pulse min-h-[600px] rounded-xl">
         <Loader2 className="h-10 w-10 text-[#006ce4] animate-spin" />
      </div>
   ),
});

export default MapView;
