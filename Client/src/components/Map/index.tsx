"use client";

import dynamic from "next/dynamic";
import { Loader2 } from "lucide-react";

const MapView = dynamic(() => import("./MapView"), {
   ssr: false,
   loading: () => (
      <div className="w-full h-full bg-zinc-100 flex items-center justify-center animate-pulse">
         <Loader2 className="h-8 w-8 text-[#006ce4] animate-spin" />
      </div>
   ),
});

export default MapView;
