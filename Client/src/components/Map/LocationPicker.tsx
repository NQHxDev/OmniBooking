"use client";

import { useState, useCallback, useRef, useEffect, useMemo } from "react";
import { MapContainer, TileLayer, Marker, useMap, useMapEvents } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { Search, Info, X, ChevronLeft, Loader2 } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { useTranslations } from "next-intl";
import { env } from "@/env";
import axios from "axios";

// Default coordinates centered on Vietnam
const defaultCenter = {
   lat: 10.762622,
   lng: 106.660172,
};

// Premium custom animated marker pin for Leaflet
const pickerMarkerIcon = L.divIcon({
   html: `<div class="relative flex items-center justify-center">
      <div class="absolute w-8 h-8 bg-blue-500/30 rounded-full animate-ping"></div>
      <svg class="h-10 w-10 text-[#006ce4] drop-shadow-lg filter" viewBox="0 0 24 24" fill="currentColor" stroke="white" stroke-width="1.5">
         <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
      </svg>
   </div>`,
   className: "custom-picker-pin",
   iconSize: [40, 40],
   iconAnchor: [20, 40],
});

interface SuggestionItem {
   name: string;
   display?: string;
   address?: string;
   ref_id?: string;
   refid?: string;
   lat?: number;
   lng?: number;
   isOsm?: boolean;
   osmAddress?: Record<string, string>;
}

interface LocationPickerProps {
   initialPosition?: { lat: number; lng: number };
   onLocationChange?: (lat: number, lng: number, address: string) => void;
}

// Map center tracking helper component
function ChangeMapView({ center, zoom }: { center: [number, number]; zoom: number }) {
   const map = useMap();
   useEffect(() => {
      map.setView(center, zoom);
   }, [center, zoom, map]);
   return null;
}

// Map events handler
function MapEventsHelper({ onClick }: { onClick: (lat: number, lng: number) => void }) {
   useMapEvents({
      click(e) {
         onClick(e.latlng.lat, e.latlng.lng);
      },
   });
   return null;
}

export default function LocationPicker({
   initialPosition = defaultCenter,
   onLocationChange,
}: LocationPickerProps) {
   const t = useTranslations("Map");

   const [position, setPosition] = useState(initialPosition);
   const [searchQuery, setSearchQuery] = useState("");
   const [address, setAddress] = useState("151 Bến Vân Đồn");
   const [city, setCity] = useState("Thành phố Hồ Chí Minh");
   const [zipCode, setZipCode] = useState("700000");
   const [autoUpdate, setAutoUpdate] = useState(true);
   const [showHint, setShowHint] = useState(true);

   // Autocomplete & Search state
   const [suggestions, setSuggestions] = useState<SuggestionItem[]>([]);
   const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
   const [showSuggestions, setShowSuggestions] = useState(false);

   const markerRef = useRef<L.Marker | null>(null);
   const suggestionsRef = useRef<HTMLDivElement | null>(null);

   const tileUrl = useMemo(() => {
      const key = env.NEXT_PUBLIC_VIETMAP_API_KEY;
      const isGoogleKey = key?.startsWith("AIzaSy");
      const isPlaceholder = !key || key.includes("your_") || key.includes("api_key");
      if (isGoogleKey || isPlaceholder) {
         return "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}";
      }
      return `https://maps.vietmap.vn/maps/tiles/{z}/{x}/{y}.png?apikey=${key}`;
   }, []);

   const attribution = useMemo(() => {
      const key = env.NEXT_PUBLIC_VIETMAP_API_KEY;
      const isGoogleKey = key?.startsWith("AIzaSy");
      const isPlaceholder = !key || key.includes("your_") || key.includes("api_key");
      if (isGoogleKey || isPlaceholder) {
         return '&copy; <a href="https://maps.google.com">Google Maps</a>';
      }
      return '&copy; <a href="https://vietmap.vn" target="_blank" rel="noopener noreferrer">VietMap</a>';
   }, []);

   // Close suggestions when clicking outside
   useEffect(() => {
      const handleClickOutside = (event: MouseEvent) => {
         if (suggestionsRef.current && !suggestionsRef.current.contains(event.target as Node)) {
            setShowSuggestions(false);
         }
      };
      document.addEventListener("mousedown", handleClickOutside);
      return () => document.removeEventListener("mousedown", handleClickOutside);
   }, []);

   // Fetch suggestions with debounce (250ms)
   useEffect(() => {
      if (!searchQuery || searchQuery.length < 2) {
         const timer = setTimeout(() => {
            setSuggestions([]);
         }, 0);
         return () => clearTimeout(timer);
      }

      const delayDebounce = setTimeout(async () => {
         setIsLoadingSuggestions(true);
         try {
            const key = env.NEXT_PUBLIC_VIETMAP_API_KEY;
            const isGoogleKey = key?.startsWith("AIzaSy");
            const isPlaceholder = !key || key.includes("your_") || key.includes("api_key");

            if (isGoogleKey || isPlaceholder) {
               // Robust, 100% free high-quality Vietnamese address search via OSM Nominatim API
               const fetchAddress = async (query: string) => {
                  const res = await axios.get("https://nominatim.openstreetmap.org/search", {
                     params: {
                        format: "json",
                        q: query,
                        "accept-language": "vi",
                        limit: 5,
                        addressdetails: 1,
                     },
                  });
                  return res.data || [];
               };

               let results = await fetchAddress(searchQuery);

               // Graceful UX Fallback: If 0 results for a specific house number search (e.g., "201/2A Lê Văn Việt"),
               // strip the house number prefix and search for the main street ("Lê Văn Việt")
               if (results.length === 0 && searchQuery.includes(" ")) {
                  const parts = searchQuery.split(" ");
                  if (/\d/.test(parts[0])) {
                     const fallbackQuery = parts.slice(1).join(" ");
                     results = await fetchAddress(fallbackQuery);
                  }
               }

               const items = results.map(
                  (item: {
                     display_name: string;
                     place_id: number;
                     lat: string;
                     lon: string;
                     address?: Record<string, string>;
                  }) => ({
                     name: item.display_name,
                     display: item.display_name,
                     ref_id: `osm-${item.place_id}`,
                     lat: parseFloat(item.lat),
                     lng: parseFloat(item.lon),
                     isOsm: true,
                     osmAddress: item.address,
                  })
               );
               setSuggestions(items);
            } else {
               const response = await axios.get("https://maps.vietmap.vn/api/autocomplete/v4", {
                  params: {
                     apikey: key,
                     text: searchQuery,
                     focus: `${position.lat},${position.lng}`,
                     display_type: 5,
                  },
               });
               setSuggestions(response.data || []);
            }
         } catch (error) {
            console.error("Autocomplete search error:", error);
         } finally {
            setIsLoadingSuggestions(false);
         }
      }, 250);

      return () => clearTimeout(delayDebounce);
   }, [searchQuery, position]);

   // Handle click on map to position pin
   const handleMapClick = useCallback(
      (lat: number, lng: number) => {
         const newPos = { lat, lng };
         setPosition(newPos);
         if (onLocationChange) onLocationChange(newPos.lat, newPos.lng, address);
      },
      [address, onLocationChange]
   );

   // Handle dragging of pin
   const eventHandlers = useMemo(
      () => ({
         dragend() {
            const marker = markerRef.current;
            if (marker != null) {
               const newPos = marker.getLatLng();
               setPosition({ lat: newPos.lat, lng: newPos.lng });
               if (onLocationChange) onLocationChange(newPos.lat, newPos.lng, address);
            }
         },
      }),
      [address, onLocationChange]
   );

   // Handle choosing a suggestion
   const handleSelectSuggestion = async (item: SuggestionItem) => {
      setSearchQuery(item.display || item.name);
      setAddress(item.display || item.name);
      setShowSuggestions(false);

      if (item.isOsm && typeof item.lat === "number" && typeof item.lng === "number") {
         const newPos = { lat: item.lat, lng: item.lng };
         setPosition(newPos);

         const addr = item.osmAddress || {};
         const houseNumber = addr.house_number || "";
         const road = addr.road || "";
         let streetAddress = "";

         if (houseNumber && road) {
            streetAddress = `${houseNumber} ${road}`;
         } else if (road) {
            streetAddress = road;
         } else {
            streetAddress = item.display || item.name;
         }

         setAddress(streetAddress);

         const cityVal =
            addr.city || addr.town || addr.municipality || addr.province || addr.state || "";
         if (cityVal) setCity(cityVal);

         const postalVal = addr.postcode || "700000";
         if (postalVal) setZipCode(postalVal);

         if (onLocationChange) {
            onLocationChange(newPos.lat, newPos.lng, streetAddress);
         }
         return;
      }

      const refId = item.ref_id || item.refid;
      if (!refId) return;

      try {
         const response = await axios.get("https://maps.vietmap.vn/api/place/v4", {
            params: {
               apikey: env.NEXT_PUBLIC_VIETMAP_API_KEY,
               refid: refId,
            },
         });

         const data = response.data;
         if (data && typeof data.lat === "number" && typeof data.lng === "number") {
            const newPos = { lat: data.lat, lng: data.lng };
            setPosition(newPos);

            // Populate other fields if possible
            if (data.city) setCity(data.city);
            if (data.hs_num && data.street) {
               setAddress(`${data.hs_num} ${data.street}`);
            } else if (data.display) {
               setAddress(data.display);
            }

            if (onLocationChange) {
               onLocationChange(data.lat, data.lng, data.display || item.display || item.name);
            }
         }
      } catch (error) {
         console.error("VietMap place details error:", error);
      }
   };

   return (
      <div className="relative w-full h-[600px] rounded-xl overflow-hidden shadow-2xl border border-gray-200 bg-gray-50 z-0">
         {/* VietMap Leaflet Map */}
         <MapContainer
            center={[position.lat, position.lng]}
            zoom={17}
            className="w-full h-full"
            style={{ width: "100%", height: "100%", zIndex: 1 }}
            zoomControl={false}
            attributionControl={false}
         >
            <ChangeMapView center={[position.lat, position.lng]} zoom={17} />
            <TileLayer url={tileUrl} attribution={attribution} />
            <MapEventsHelper onClick={handleMapClick} />
            <Marker
               position={[position.lat, position.lng]}
               draggable={true}
               icon={pickerMarkerIcon}
               ref={markerRef}
               eventHandlers={eventHandlers}
            />
         </MapContainer>

         {/* Overlay Form */}
         <div className="absolute top-6 left-6 z-999 w-full max-w-md pointer-events-none">
            <motion.div
               initial={{ opacity: 0, x: -20 }}
               animate={{ opacity: 1, x: 0 }}
               className="bg-white/95 backdrop-blur-md p-8 rounded-2xl shadow-[0_20px_50px_rgba(0,0,0,0.15)] border border-white/20 pointer-events-auto"
            >
               <h2 className="text-2xl font-bold text-gray-900 mb-6">{t("title")}</h2>

               <div className="space-y-4">
                  {/* Autocomplete Address Search using VietMap */}
                  <div className="relative" ref={suggestionsRef}>
                     <label className="block text-sm font-bold text-gray-700 mb-1">
                        {t("searchLabel")}
                     </label>
                     <div className="relative">
                        <input
                           type="text"
                           value={searchQuery}
                           onChange={(e) => {
                              setSearchQuery(e.target.value);
                              setShowSuggestions(true);
                           }}
                           onFocus={() => setShowSuggestions(true)}
                           className="w-full pl-4 pr-12 py-3 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-[#006ce4] focus:border-transparent outline-none transition-all duration-200 text-gray-800"
                           placeholder={t("searchPlaceholder")}
                        />
                        <Search className="absolute right-4 top-3.5 h-5 w-5 text-gray-400" />
                     </div>

                     {/* Autocomplete dropdown */}
                     <AnimatePresence>
                        {showSuggestions && searchQuery.length >= 2 && (
                           <motion.div
                              initial={{ opacity: 0, y: 10 }}
                              animate={{ opacity: 1, y: 0 }}
                              exit={{ opacity: 0, y: 10 }}
                              className="absolute left-0 right-0 top-full mt-1 bg-white/95 backdrop-blur-md border border-gray-150 shadow-[0_10px_35px_rgba(0,0,0,0.15)] rounded-xl max-h-60 overflow-y-auto z-1000"
                           >
                              {isLoadingSuggestions ? (
                                 <div className="flex items-center justify-center p-4">
                                    <Loader2 className="h-5 w-5 text-[#006ce4] animate-spin" />
                                 </div>
                              ) : suggestions.length === 0 ? (
                                 <div className="p-4 text-xs text-gray-400 text-center">
                                    Không tìm thấy địa điểm nào
                                 </div>
                              ) : (
                                 suggestions.map((item, idx) => (
                                    <div
                                       key={item.ref_id || item.refid || idx}
                                       onClick={() => handleSelectSuggestion(item)}
                                       className="px-4 py-2.5 text-sm text-gray-700 hover:bg-blue-50/80 cursor-pointer transition-colors border-b border-gray-50 last:border-b-0 flex flex-col gap-0.5"
                                    >
                                       <span className="font-bold text-gray-900 text-xs truncate">
                                          {item.name}
                                       </span>
                                       <span className="text-[10px] text-gray-400 truncate">
                                          {item.address || item.display}
                                       </span>
                                    </div>
                                 ))
                              )}
                           </motion.div>
                        )}
                     </AnimatePresence>
                  </div>

                  <div>
                     <label className="block text-sm font-bold text-gray-700 mb-1">
                        {t("unitLabel")}
                     </label>
                     <input
                        type="text"
                        className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-[#006ce4] focus:border-transparent transition-all duration-200 text-sm text-gray-800"
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
                           className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-[#006ce4] focus:border-transparent transition-all duration-200 text-sm text-gray-800"
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
                           className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-[#006ce4] focus:border-transparent transition-all duration-200 text-sm text-gray-800"
                        />
                     </div>
                  </div>

                  <div className="flex items-center gap-2 py-1">
                     <input
                        type="checkbox"
                        id="auto-update"
                        checked={autoUpdate}
                        onChange={(e) => setAutoUpdate(e.target.checked)}
                        className="w-4 h-4 text-[#006ce4] border-gray-300 rounded focus:ring-[#006ce4] cursor-pointer"
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
                              <p className="text-xs text-gray-600 leading-relaxed pr-4">
                                 {t("hint")}
                              </p>
                           </div>
                        </motion.div>
                     )}
                  </AnimatePresence>

                  <div className="flex gap-4 pt-4">
                     <button className="flex items-center justify-center w-14 h-14 border-2 border-[#006ce4] rounded-xl text-[#006ce4] hover:bg-blue-50 transition-all duration-200 active:scale-90 cursor-pointer">
                        <ChevronLeft className="h-7 w-7" />
                     </button>
                     <button className="flex-1 bg-[#006ce4] text-white font-bold text-lg rounded-xl hover:bg-[#005bb8] transition-all shadow-[0_10px_20px_rgba(0,108,228,0.3)] active:scale-95 cursor-pointer">
                        {t("continue")}
                     </button>
                  </div>
               </div>
            </motion.div>
         </div>

         {/* VietMap Badge */}
         <div className="absolute bottom-6 right-6 z-999 bg-white px-4 py-2 rounded-full shadow-lg border border-gray-200 flex items-center gap-2 hover:scale-105 transition-transform duration-200 select-none">
            <span className="w-2.5 h-2.5 bg-red-500 rounded-full animate-pulse"></span>
            <span className="text-[10px] font-extrabold text-gray-700 uppercase tracking-widest">
               Bản đồ VietMap
            </span>
         </div>
      </div>
   );
}
