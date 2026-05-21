"use client";

import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { PropertyDocument } from "@/lib/api/services/propertyService";
import { useState, useMemo, useEffect, useRef } from "react";
import Image from "next/image";

// Custom price tag icon generator for Leaflet
const createPriceIcon = (price: number, isSelected: boolean) => {
   const text =
      price >= 1000000 ? (price / 1000000).toFixed(1) + "tr" : (price / 1000).toFixed(0) + "k";

   return L.divIcon({
      html: `<div class="px-2.5 py-1 rounded-full text-xs font-bold shadow-md border transition-all duration-200 ${
         isSelected
            ? "bg-[#006ce4] text-white border-[#006ce4] scale-110 z-50"
            : "bg-white text-gray-900 border-gray-200 hover:scale-105 hover:z-40"
      }">${text}</div>`,
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
   center = [10.762622, 106.660172],
   zoom = 17,
   showControls = true,
}: MapViewProps) {
   const [selectedProperty, setSelectedProperty] = useState<PropertyDocument | null>(null);
   const [isRetina, setIsRetina] = useState(false);

   useEffect(() => {
      const timer = setTimeout(() => {
         if (typeof window !== "undefined" && window.devicePixelRatio > 1) {
            setIsRetina(true);
         }
      }, 0);
      return () => clearTimeout(timer);
   }, []);

   const mapCenter = useMemo<[number, number]>(() => {
      if (properties.length > 0 && properties[0].location) {
         return [properties[0].location.lat, properties[0].location.lon];
      }
      return center;
   }, [properties, center]);

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
                     icon={createPriceIcon(property.minPrice, isSelected)}
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
                              {property.minPrice.toLocaleString("vi-VN")}đ
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
