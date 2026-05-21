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
   lat: 14.058324,
   lng: 108.277199,
};

const countriesList = [
   { name: "Việt Nam", code: "VN", lat: 14.058324, lng: 108.277199, zoom: 5 },
   { name: "Singapore", code: "SG", lat: 1.352083, lng: 103.819836, zoom: 11 },
   { name: "Thailand", code: "TH", lat: 15.870032, lng: 100.992541, zoom: 5 },
   { name: "Malaysia", code: "MY", lat: 4.210484, lng: 101.98855, zoom: 6 },
   { name: "Indonesia", code: "ID", lat: -0.789275, lng: 113.921327, zoom: 4 },
   { name: "Japan", code: "JP", lat: 36.204824, lng: 138.252924, zoom: 5 },
   { name: "South Korea", code: "KR", lat: 35.907757, lng: 127.766922, zoom: 6 },
   { name: "United States", code: "US", lat: 37.09024, lng: -95.712891, zoom: 4 },
   { name: "United Kingdom", code: "GB", lat: 55.378051, lng: -3.435973, zoom: 5 },
   { name: "France", code: "FR", lat: 46.227638, lng: 2.213749, zoom: 5 },
];

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
   isGoong?: boolean;
   osmAddress?: Record<string, string>;
}

interface LocationPickerProps {
   initialPosition?: { lat: number; lng: number };
   onLocationChange?: (lat: number, lng: number, address: string) => void;
   onAddressDetailsChange?: (details: { address: string; city: string; country: string }) => void;
   showNavigation?: boolean;
   onBack?: () => void;
   onNext?: () => void;
   className?: string;
}

// Map center tracking helper component
function ChangeMapView({ center, zoom }: { center: [number, number]; zoom: number }) {
   const map = useMap();
   useEffect(() => {
      const container = map.getContainer();
      console.log("ChangeMapView -> Map Container Dimensions:", {
         width: container?.clientWidth,
         height: container?.clientHeight,
         offsetWidth: container?.offsetWidth,
         offsetHeight: container?.offsetHeight,
      });
      map.setView(center, zoom);
      map.invalidateSize();
      const timer1 = setTimeout(() => {
         console.log("ChangeMapView (100ms) -> Dimensions:", {
            width: container?.clientWidth,
            height: container?.clientHeight,
         });
         map.invalidateSize();
      }, 100);
      const timer2 = setTimeout(() => {
         console.log("ChangeMapView (300ms) -> Dimensions:", {
            width: container?.clientWidth,
            height: container?.clientHeight,
         });
         map.invalidateSize();
      }, 300);
      const timer3 = setTimeout(() => {
         console.log("ChangeMapView (600ms) -> Dimensions:", {
            width: container?.clientWidth,
            height: container?.clientHeight,
         });
         map.invalidateSize();
      }, 600);
      return () => {
         clearTimeout(timer1);
         clearTimeout(timer2);
         clearTimeout(timer3);
      };
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
   onAddressDetailsChange,
   showNavigation = true,
   onBack,
   onNext,
   className = "h-[600px] rounded-xl shadow-2xl border border-gray-200",
}: LocationPickerProps) {
   const t = useTranslations("Map");

   // Check if the initial position is the default center of Vietnam
   const isDefaultVietnam = initialPosition.lat === 14.058324 && initialPosition.lng === 108.277199;

   const [position, setPosition] = useState(initialPosition);
   const [zoom, setZoom] = useState(isDefaultVietnam ? 5 : 17);
   const [hasSelectedLocation, setHasSelectedLocation] = useState(!isDefaultVietnam);
   const [searchQuery, setSearchQuery] = useState("");
   const [address, setAddress] = useState("");
   const [city, setCity] = useState("");
   const [country, setCountry] = useState("Việt Nam");
   const [zipCode, setZipCode] = useState("");
   const [autoUpdate, setAutoUpdate] = useState(true);
   const [showHint, setShowHint] = useState(true);
   const [isRetina, setIsRetina] = useState(false);

   useEffect(() => {
      const timer = setTimeout(() => {
         if (typeof window !== "undefined" && window.devicePixelRatio > 1) {
            setIsRetina(true);
         }
      }, 0);
      return () => clearTimeout(timer);
   }, []);

   // IP-based Country Geolocation detection
   useEffect(() => {
      // Only run geolocation detection if starting with the default Vietnam center
      if (!isDefaultVietnam) return;

      const detectCountryAndLocate = async () => {
         try {
            // Call our internal backend API to bypass client-side CORS and ad blockers
            const res = await axios.get("/api/geolocation");
            if (res.data && res.data.countryCode) {
               const countryCode = res.data.countryCode;
               const countryName = res.data.countryName;
               const latitude = res.data.latitude;
               const longitude = res.data.longitude;

               const matched = countriesList.find(
                  (c) =>
                     c.code === countryCode || c.name.toLowerCase() === countryName.toLowerCase()
               );

               if (matched) {
                  setCountry(matched.name);
                  setPosition({ lat: matched.lat, lng: matched.lng });
                  setZoom(matched.zoom);
               } else {
                  setCountry(countryName || "Việt Nam");
                  if (typeof latitude === "number" && typeof longitude === "number") {
                     setPosition({ lat: latitude, lng: longitude });
                     setZoom(5);
                  }
               }
            }
         } catch (error) {
            console.error("IP Geolocation via server failed, falling back to default:", error);
            // Explicitly fallback to default country and coordinates (Vietnam)
            const defaultMatched = countriesList.find((c) => c.code === "VN") || {
               name: "Việt Nam",
               lat: 14.058324,
               lng: 108.277199,
               zoom: 5,
            };
            setCountry(defaultMatched.name);
            setPosition({ lat: defaultMatched.lat, lng: defaultMatched.lng });
            setZoom(defaultMatched.zoom);
         }
      };
      detectCountryAndLocate();
   }, [isDefaultVietnam]);

   // Sync address details to parent
   const onAddressDetailsChangeRef = useRef(onAddressDetailsChange);
   useEffect(() => {
      onAddressDetailsChangeRef.current = onAddressDetailsChange;
   }, [onAddressDetailsChange]);

   useEffect(() => {
      if (onAddressDetailsChangeRef.current) {
         onAddressDetailsChangeRef.current({ address, city, country });
      }
   }, [address, city, country]);

   const handleCountryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
      const selectedName = e.target.value;
      setCountry(selectedName);

      const matched = countriesList.find((c) => c.name === selectedName);
      if (matched) {
         setPosition({ lat: matched.lat, lng: matched.lng });
         setZoom(matched.zoom);
      }
   };

   // Autocomplete & Search state
   const [suggestions, setSuggestions] = useState<SuggestionItem[]>([]);
   const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
   const [showSuggestions, setShowSuggestions] = useState(false);

   const markerRef = useRef<L.Marker | null>(null);
   const suggestionsRef = useRef<HTMLDivElement | null>(null);

   const tileUrl = useMemo(() => {
      return isRetina
         ? "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}&scale=2"
         : "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}";
   }, [isRetina]);

   const attribution = '&copy; <a href="https://maps.google.com">Google Maps</a>';

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
            const goongKey = env.NEXT_PUBLIC_GOONG_API_KEY;
            const key = env.NEXT_PUBLIC_VIETMAP_API_KEY;
            const isGoogleKey = key?.startsWith("AIzaSy");
            const isPlaceholder = !key || key.includes("your_") || key.includes("api_key");

            if (key && !isPlaceholder && !isGoogleKey) {
               const response = await axios.get("https://maps.vietmap.vn/api/autocomplete/v4", {
                  params: {
                     apikey: key,
                     text: searchQuery,
                     focus: `${position.lat},${position.lng}`,
                     display_type: 5,
                  },
               });
               setSuggestions(response.data || []);
            } else if (goongKey && goongKey.length > 5) {
               // Use Goong Autocomplete API
               const response = await axios.get("https://rsapi.goong.io/Place/AutoComplete", {
                  params: {
                     api_key: goongKey,
                     input: searchQuery,
                     location: `${position.lat},${position.lng}`,
                     limit: 5,
                  },
               });
               const predictions = response.data.predictions || [];
               const items = predictions.map((pred: { description: string; place_id: string }) => ({
                  name: pred.description,
                  display: pred.description,
                  ref_id: pred.place_id,
                  isGoong: true,
               }));
               setSuggestions(items);
            } else {
               // Robust, 100% free high-quality Vietnamese address search via OSM Nominatim API
               const fetchAddress = async (query: string) => {
                  const res = await axios.get("https://nominatim.openstreetmap.org/search", {
                     params: {
                        format: "json",
                        q: query,
                        "accept-language": "vi",
                        limit: 5,
                        addressdetails: 1,
                        countrycodes: "vn", // Restrict results to Vietnam
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
         setHasSelectedLocation(true);
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
               setHasSelectedLocation(true);
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
      setHasSelectedLocation(true);
      setSearchQuery(item.display || item.name);
      setAddress(item.display || item.name);
      setShowSuggestions(false);

      if (item.isOsm && typeof item.lat === "number" && typeof item.lng === "number") {
         const newPos = { lat: item.lat, lng: item.lng };
         setPosition(newPos);
         setZoom(18);

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

         const countryVal = addr.country || "Việt Nam";
         setCountry(countryVal);

         if (onLocationChange) {
            onLocationChange(newPos.lat, newPos.lng, streetAddress);
         }
         return;
      }

      const refId = item.ref_id || item.refid;
      if (!refId) return;

      if (item.isGoong) {
         try {
            const response = await axios.get("https://rsapi.goong.io/Place/Detail", {
               params: {
                  api_key: env.NEXT_PUBLIC_GOONG_API_KEY,
                  place_id: refId,
               },
            });
            const result = response.data.result;
            if (result && result.geometry && result.geometry.location) {
               const lat = result.geometry.location.lat;
               const lng = result.geometry.location.lng;
               const newPos = { lat, lng };
               setPosition(newPos);
               setZoom(18);

               const display = result.formatted_address || result.name;
               setAddress(display);

               const compound = result.compound || {};
               if (compound.province) setCity(compound.province);
               setCountry("Việt Nam");

               if (onLocationChange) {
                  onLocationChange(lat, lng, display);
               }
            }
         } catch (error) {
            console.error("Goong place details error:", error);
         }
         return;
      }

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
            setZoom(18);

            // Populate other fields if possible
            if (data.city) setCity(data.city);
            if (data.country) setCountry(data.country);
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
      <div className={`relative w-full overflow-hidden bg-gray-50 z-0 ${className}`}>
         <link
            rel="stylesheet"
            href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
            integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
            crossOrigin=""
         />
         {/* VietMap Leaflet Map */}
         <MapContainer
            center={[position.lat, position.lng]}
            zoom={zoom}
            minZoom={4}
            maxZoom={20}
            className="w-full h-full"
            style={{ width: "100%", height: "100%", zIndex: 1 }}
            zoomControl={false}
            attributionControl={false}
         >
            <ChangeMapView center={[position.lat, position.lng]} zoom={zoom} />
            <TileLayer key={tileUrl} url={tileUrl} attribution={attribution} />
            <MapEventsHelper onClick={handleMapClick} />
            {hasSelectedLocation && (
               <Marker
                  position={[position.lat, position.lng]}
                  draggable={true}
                  icon={pickerMarkerIcon}
                  ref={markerRef}
                  eventHandlers={eventHandlers}
               />
            )}
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
                     <div className="col-span-2">
                        <label className="block text-sm font-bold text-gray-700 mb-1">
                           {t("cityLabel")}
                        </label>
                        <input
                           type="text"
                           value={city}
                           onChange={(e) => setCity(e.target.value)}
                           placeholder={t("cityPlaceholder")}
                           className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-[#006ce4] focus:border-transparent transition-all duration-200 text-sm text-gray-800"
                        />
                     </div>
                     <div>
                        <label className="block text-sm font-bold text-gray-700 mb-1">
                           Quốc gia
                        </label>
                        <select
                           value={country}
                           onChange={handleCountryChange}
                           className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-[#006ce4] focus:border-transparent transition-all duration-200 text-sm text-gray-800 font-medium cursor-pointer"
                        >
                           {countriesList.map((c) => (
                              <option key={c.code} value={c.name}>
                                 {c.name}
                              </option>
                           ))}
                        </select>
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

                  {showNavigation && (
                     <div className="flex gap-4 pt-4">
                        <button
                           type="button"
                           onClick={onBack}
                           className="flex items-center justify-center w-14 h-14 border-2 border-[#006ce4] rounded-xl text-[#006ce4] hover:bg-blue-50 transition-all duration-200 active:scale-90 cursor-pointer"
                        >
                           <ChevronLeft className="h-7 w-7" />
                        </button>
                        <button
                           type="button"
                           onClick={onNext}
                           className="flex-1 bg-[#006ce4] text-white font-bold text-lg rounded-xl hover:bg-[#005bb8] transition-all shadow-[0_10px_20px_rgba(0,108,228,0.3)] active:scale-95 cursor-pointer"
                        >
                           {t("continue")}
                        </button>
                     </div>
                  )}
               </div>
            </motion.div>
         </div>
      </div>
   );
}
