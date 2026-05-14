"use client";

import { useState, useCallback, useRef } from "react";
import { GoogleMap, useJsApiLoader, MarkerF, Autocomplete } from "@react-google-maps/api";
import { Search, Info, X, ChevronLeft, Loader2 } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { useTranslations } from "next-intl";
import { env } from "@/env";
import Image from "next/image";

const containerStyle = {
   width: "100%",
   height: "100%",
};

const defaultCenter = {
   lat: 10.762622,
   lng: 106.660172,
};

// Map styling for a cleaner look (optional)
const mapOptions = {
   disableDefaultUI: true,
   zoomControl: false,
   clickableIcons: false,
   styles: [
      {
         featureType: "poi",
         elementType: "labels",
         stylers: [{ visibility: "off" }],
      },
   ],
};

interface LocationPickerProps {
   initialPosition?: { lat: number; lng: number };
   onLocationChange?: (lat: number, lng: number, address: string) => void;
}

export default function LocationPicker({
   initialPosition = defaultCenter,
   onLocationChange,
}: LocationPickerProps) {
   const t = useTranslations("Map");
   const { isLoaded } = useJsApiLoader({
      id: "google-map-script",
      googleMapsApiKey: env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY,
      libraries: ["places"],
   });

   const [position, setPosition] = useState(initialPosition);
   const [address, setAddress] = useState("151 Bến Vân Đồn");
   const [city, setCity] = useState("Thành phố Hồ Chí Minh");
   const [zipCode, setZipCode] = useState("700000");
   const [autoUpdate, setAutoUpdate] = useState(true);
   const [showHint, setShowHint] = useState(true);

   const autocompleteRef = useRef<google.maps.places.Autocomplete | null>(null);

   const onMapClick = useCallback(
      (e: google.maps.MapMouseEvent) => {
         if (e.latLng) {
            const newPos = { lat: e.latLng.lat(), lng: e.latLng.lng() };
            setPosition(newPos);
            if (onLocationChange) onLocationChange(newPos.lat, newPos.lng, address);
         }
      },
      [address, onLocationChange]
   );

   const onMarkerDragEnd = useCallback(
      (e: google.maps.MapMouseEvent) => {
         if (e.latLng) {
            const newPos = { lat: e.latLng.lat(), lng: e.latLng.lng() };
            setPosition(newPos);
            if (onLocationChange) onLocationChange(newPos.lat, newPos.lng, address);
         }
      },
      [address, onLocationChange]
   );

   const onPlaceChanged = () => {
      if (autocompleteRef.current !== null) {
         const place = autocompleteRef.current.getPlace();
         if (place.geometry && place.geometry.location) {
            const newPos = {
               lat: place.geometry.location.lat(),
               lng: place.geometry.location.lng(),
            };
            setPosition(newPos);
            setAddress(place.formatted_address || "");
            if (onLocationChange)
               onLocationChange(newPos.lat, newPos.lng, place.formatted_address || "");
         }
      }
   };

   if (!isLoaded) {
      return (
         <div className="w-full h-[600px] bg-gray-100 flex flex-col items-center justify-center rounded-xl gap-4">
            <Loader2 className="h-10 w-10 text-[#006ce4] animate-spin" />
            <p className="text-gray-500 font-medium">{t("loading")}</p>
         </div>
      );
   }

   return (
      <div className="relative w-full h-[600px] rounded-xl overflow-hidden shadow-2xl border border-gray-200 bg-gray-50">
         {/* Google Map */}
         <GoogleMap
            mapContainerStyle={containerStyle}
            center={position}
            zoom={15}
            onClick={onMapClick}
            options={mapOptions}
         >
            <MarkerF
               position={position}
               draggable={true}
               onDragEnd={onMarkerDragEnd}
               animation={google.maps.Animation.DROP}
            />
         </GoogleMap>

         {/* Overlay Form */}
         <div className="absolute top-6 left-6 z-10 w-full max-w-md pointer-events-none">
            <motion.div
               initial={{ opacity: 0, x: -20 }}
               animate={{ opacity: 1, x: 0 }}
               className="bg-white/95 backdrop-blur-md p-8 rounded-2xl shadow-[0_20px_50px_rgba(0,0,0,0.15)] border border-white/20 pointer-events-auto"
            >
               <h2 className="text-2xl font-bold text-gray-900 mb-6">{t("title")}</h2>

               <div className="space-y-4">
                  {/* Autocomplete Address Search */}
                  <div>
                     <label className="block text-sm font-bold text-gray-700 mb-1">
                        {t("searchLabel")}
                     </label>
                     <div className="relative">
                        <Autocomplete
                           onLoad={(autocomplete) => (autocompleteRef.current = autocomplete)}
                           onPlaceChanged={onPlaceChanged}
                        >
                           <input
                              type="text"
                              value={address}
                              onChange={(e) => setAddress(e.target.value)}
                              className="w-full pl-4 pr-12 py-3 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-[#006ce4] focus:border-transparent outline-none transition-all duration-200 text-gray-800"
                              placeholder={t("searchPlaceholder")}
                           />
                        </Autocomplete>
                        <Search className="absolute right-4 top-3.5 h-5 w-5 text-gray-400" />
                     </div>
                  </div>

                  <div>
                     <label className="block text-sm font-bold text-gray-700 mb-1">
                        {t("unitLabel")}
                     </label>
                     <input
                        type="text"
                        className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-[#006ce4] focus:border-transparent transition-all duration-200"
                        placeholder={t("unitPlaceholder")}
                     />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                     <div>
                        <label className="block text-sm font-bold text-gray-700 mb-1">
                           {t("cityLabel")}
                        </label>
                        <input
                           type="text"
                           value={city}
                           onChange={(e) => setCity(e.target.value)}
                           className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-[#006ce4] focus:border-transparent transition-all duration-200"
                        />
                     </div>
                     <div>
                        <label className="block text-sm font-bold text-gray-700 mb-1">
                           {t("zipLabel")}
                        </label>
                        <input
                           type="text"
                           value={zipCode}
                           onChange={(e) => setZipCode(e.target.value)}
                           className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-[#006ce4] focus:border-transparent transition-all duration-200"
                        />
                     </div>
                  </div>

                  <div className="flex items-center gap-2 py-1">
                     <input
                        type="checkbox"
                        id="auto-update"
                        checked={autoUpdate}
                        onChange={(e) => setAutoUpdate(e.target.checked)}
                        className="w-4 h-4 text-[#006ce4] border-gray-300 rounded focus:ring-[#006ce4]"
                     />
                     <label
                        htmlFor="auto-update"
                        className="text-sm font-medium text-gray-700 cursor-pointer"
                     >
                        {t("autoUpdate")}
                     </label>
                  </div>

                  <AnimatePresence>
                     {showHint && (
                        <motion.div
                           initial={{ opacity: 0, height: 0 }}
                           animate={{ opacity: 1, height: "auto" }}
                           exit={{ opacity: 0, height: 0 }}
                           className="bg-blue-50 border border-blue-100 rounded-md p-4 relative"
                        >
                           <button
                              onClick={() => setShowHint(false)}
                              className="absolute top-2 right-2 text-blue-400 hover:text-blue-600"
                           >
                              <X className="h-4 w-4" />
                           </button>
                           <div className="flex gap-3">
                              <Info className="h-5 w-5 text-[#006ce4] shrink-0 mt-0.5" />
                              <p className="text-sm text-gray-600 leading-relaxed pr-4">
                                 {t("hint")}
                              </p>
                           </div>
                        </motion.div>
                     )}
                  </AnimatePresence>

                  <div className="flex gap-4 pt-4">
                     <button className="flex items-center justify-center w-14 h-14 border-2 border-[#006ce4] rounded-xl text-[#006ce4] hover:bg-blue-50 transition-all duration-200 active:scale-90">
                        <ChevronLeft className="h-7 w-7" />
                     </button>
                     <button className="flex-1 bg-[#006ce4] text-white font-bold text-lg rounded-xl hover:bg-[#005bb8] transition-all shadow-[0_10px_20px_rgba(0,108,228,0.3)] active:scale-95">
                        {t("continue")}
                     </button>
                  </div>
               </div>
            </motion.div>
         </div>

         {/* Google Badge */}
         <div className="absolute bottom-6 right-6 z-10 bg-white px-3 py-1.5 rounded-full shadow-lg border border-gray-200 flex items-center gap-2">
            <Image
               src="https://upload.wikimedia.org/wikipedia/commons/thumb/c/c1/Google_Maps_icon_%282020%29.svg/1024px-Google_Maps_icon_%282020%29.svg.png"
               className="w-4 h-4"
               alt="Google Maps"
               width={16}
               height={16}
            />
            <span className="text-[10px] font-bold text-gray-600 uppercase tracking-widest">
               {t("poweredBy")}
            </span>
         </div>
      </div>
   );
}
