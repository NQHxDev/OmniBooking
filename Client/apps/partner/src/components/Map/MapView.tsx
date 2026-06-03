"use client";

import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { PropertyDocument } from "@/lib/api/services/propertyService";
import { useState, useMemo, useEffect, useRef } from "react";
import Image from "next/image";
import { useSettingStore } from "@/store/useSettingStore";
import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api/apiClient";

// City coordinates lookup for Vietnam
const CITY_COORDINATES: Record<string, [number, number]> = {
   // Vietnamese names
   "thành phố hồ chí minh": [10.762622, 106.660172],
   "hồ chí minh": [10.762622, 106.660172],
   "hà nội": [21.028511, 105.804817],
   "đà nẵng": [16.047079, 108.20623],
   "hội an": [15.87987, 108.335007],
   "phú quốc": [10.227025, 103.967169],
   "quảng ninh": [21.006382, 107.045822],
   "tỉnh quảng ninh": [21.006382, 107.045822],
   "hạ long": [20.959902, 107.042542],
   "nha trang": [12.238791, 109.196749],
   "đà lạt": [11.940419, 108.458313],
   "vũng tàu": [10.34579, 107.08448],
   sapa: [22.33629, 103.84385],
   "sa pa": [22.33629, 103.84385],
   huế: [16.463713, 107.590866],
   "hải phòng": [20.844912, 106.688084],
   "cần thơ": [10.045162, 105.746857],
   "quy nhơn": [13.78268, 109.21965],
   "phan thiết": [10.9334, 108.10022],
   "mũi né": [10.9334, 108.287],
   "ninh bình": [20.25876, 105.97567],
   // English names
   "ho chi minh city": [10.762622, 106.660172],
   "ho chi minh": [10.762622, 106.660172],
   hanoi: [21.028511, 105.804817],
   "da nang": [16.047079, 108.20623],
   "hoi an": [15.87987, 108.335007],
   "phu quoc": [10.227025, 103.967169],
   "quang ninh": [21.006382, 107.045822],
   "ha long": [20.959902, 107.042542],
   "da lat": [11.940419, 108.458313],
   "vung tau": [10.34579, 107.08448],
   hue: [16.463713, 107.590866],
   "hai phong": [20.844912, 106.688084],
   "can tho": [10.045162, 105.746857],
   "quy nhon": [13.78268, 109.21965],
   "phan thiet": [10.9334, 108.10022],
   "mui ne": [10.9334, 108.287],
   "ninh binh": [20.25876, 105.97567],
};

// Default center: Vietnam overview
const VIETNAM_CENTER: [number, number] = [16.047079, 108.20623];

function getCityCenter(cityName?: string): [number, number] | null {
   if (!cityName) return null;
   const normalized = cityName.toLowerCase().trim();
   return CITY_COORDINATES[normalized] || null;
}

// Custom price tag icon generator for Leaflet
const createPriceIcon = (priceText: string, isSelected: boolean) => {
   return L.divIcon({
      html: `<div class="px-2.5 py-1 rounded-full text-xs font-bold shadow-md border transition-all duration-200 ${
         isSelected
            ? "bg-[#006ce4] text-white border-[#006ce4] scale-110 z-50"
            : "bg-white text-gray-900 border-gray-200 hover:scale-105 hover:z-40"
      }">${priceText}</div>`,
      className: "custom-price-tag",
      iconSize: [60, 30],
      iconAnchor: [30, 15],
   });
};

interface MapViewProps {
   properties: PropertyDocument[];
   center?: [number, number];
   zoom?: number;
   showControls?: boolean;
   showAttribution?: boolean;
   searchCity?: string;
}

// Map center tracking helper component
function ChangeMapView({ center, zoom }: { center: [number, number]; zoom: number }) {
   const map = useMap();
   const lastCenterRef = useRef<[number, number] | null>(null);
   const lastZoomRef = useRef<number | null>(null);

   useEffect(() => {
      const isSameCenter =
         lastCenterRef.current &&
         lastCenterRef.current[0] === center[0] &&
         lastCenterRef.current[1] === center[1];
      const isSameZoom = lastZoomRef.current === zoom;

      if (!isSameCenter || !isSameZoom) {
         lastCenterRef.current = center;
         lastZoomRef.current = zoom;
         map.setView(center, zoom);
         map.invalidateSize();

         const timer1 = setTimeout(() => map.invalidateSize(), 100);
         const timer2 = setTimeout(() => map.invalidateSize(), 300);
         const timer3 = setTimeout(() => map.invalidateSize(), 600);
         return () => {
            clearTimeout(timer1);
            clearTimeout(timer2);
            clearTimeout(timer3);
         };
      }
   }, [center, zoom, map]);
   return null;
}

export default function MapView({
   properties,
   center,
   zoom = 17,
   showControls = true,
   searchCity,
}: MapViewProps) {
   const [selectedProperty, setSelectedProperty] = useState<PropertyDocument | null>(null);
   const [isRetina, setIsRetina] = useState(false);

   const { currency: targetCurrency } = useSettingStore();
   const { data: rates } = useQuery({
      queryKey: ["currency-rates"],
      queryFn: async () => {
         const response = await apiClient.get<unknown, Record<string, number>>("/currencies/rates");
         return response;
      },
      staleTime: 1000 * 60 * 60, // 1 hour
   });

   const getFormattedPriceTag = (price: number) => {
      const rate = rates?.[targetCurrency] || 1;
      let converted = price * rate;
      if (targetCurrency === "VND") {
         converted = Math.round(converted / 1000) * 1000;
         return converted >= 1000000
            ? (converted / 1000000).toFixed(1) + "tr"
            : (converted / 1000).toFixed(0) + "k";
      } else {
         return "$" + converted.toFixed(0);
      }
   };

   const getFormattedPricePopup = (price: number) => {
      const rate = rates?.[targetCurrency] || 1;
      let converted = price * rate;
      if (targetCurrency === "VND") {
         converted = Math.round(converted / 1000) * 1000;
         const formatter = new Intl.NumberFormat("vi-VN", {
            style: "decimal",
            minimumFractionDigits: 0,
            maximumFractionDigits: 0,
         });
         return `VND ${formatter.format(converted)}`;
      }
      const formatter = new Intl.NumberFormat("en-US", {
         style: "currency",
         currency: targetCurrency,
         minimumFractionDigits: 2,
         maximumFractionDigits: 2,
      });
      return formatter.format(converted);
   };

   useEffect(() => {
      const timer = setTimeout(() => {
         if (typeof window !== "undefined" && window.devicePixelRatio > 1) {
            setIsRetina(true);
         }
      }, 0);
      return () => clearTimeout(timer);
   }, []);

   const mapCenter = useMemo<[number, number]>(() => {
      // 1. Try to use the first property's location
      const propertyWithLocation = properties.find((p) => p.location);
      if (propertyWithLocation?.location) {
         return [propertyWithLocation.location.lat, propertyWithLocation.location.lon];
      }
      // 2. Try to use an explicitly passed center
      if (center) {
         return center;
      }
      // 3. Try to geocode from the search city name
      const cityCenter = getCityCenter(searchCity);
      if (cityCenter) {
         return cityCenter;
      }
      // 4. Fallback to Vietnam center
      return VIETNAM_CENTER;
   }, [properties, center, searchCity]);

   const tileUrl = useMemo(() => {
      return isRetina
         ? "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}&scale=2"
         : "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}";
   }, [isRetina]);

   const attribution = '&copy; <a href="https://maps.google.com">Google Maps</a>';

   return (
      <div className="relative w-full h-full min-h-[400px] rounded-xl overflow-hidden shadow-md border border-gray-100 z-0">
         <link
            rel="stylesheet"
            href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
            integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
            crossOrigin=""
         />
         <MapContainer
            center={mapCenter}
            zoom={zoom}
            minZoom={4}
            maxZoom={20}
            className="w-full h-full"
            zoomControl={showControls}
            attributionControl={false}
         >
            <ChangeMapView center={mapCenter} zoom={zoom} />
            <TileLayer key={tileUrl} url={tileUrl} attribution={attribution} />
            {properties.map((property) => {
               const isSelected = selectedProperty?.id === property.id;

               if (!property.location) return null;

               return (
                  <Marker
                     key={property.id}
                     position={[property.location.lat, property.location.lon]}
                     icon={createPriceIcon(getFormattedPriceTag(property.minPrice), isSelected)}
                     eventHandlers={{
                        popupopen: () => setSelectedProperty(property),
                        popupclose: () => {
                           if (selectedProperty?.id === property.id) {
                              setSelectedProperty(null);
                           }
                        },
                     }}
                  >
                     <Popup className="custom-leaflet-popup">
                        <div className="p-1 w-[200px]">
                           <div className="relative w-full h-24 mb-2">
                              <Image
                                 src={property.mainImageUrl}
                                 alt={property.name}
                                 fill
                                 sizes="200px"
                                 className="object-cover rounded-md"
                              />
                           </div>
                           <h4 className="font-bold text-sm leading-tight mb-1 text-gray-900">
                              {property.name}
                           </h4>
                           <p className="text-[#006ce4] font-bold text-sm">
                              {getFormattedPricePopup(property.minPrice)}
                           </p>
                        </div>
                     </Popup>
                  </Marker>
               );
            })}
         </MapContainer>
      </div>
   );
}
