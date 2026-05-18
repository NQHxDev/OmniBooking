"use client";

import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { PropertyDocument } from "@/lib/api/services/propertyService";
import { useState, useMemo, useEffect } from "react";
import Image from "next/image";
import { env } from "@/env";

// Custom price tag icon generator for Leaflet
const createPriceIcon = (price: number, isSelected: boolean) => {
   const text =
      price >= 1000000 ? (price / 1000000).toFixed(1) + "tr" : (price / 1000).toFixed(0) + "k";

   return L.divIcon({
      html: `<div class="transition-all duration-200 cursor-pointer shadow-lg font-bold border-2 border-white rounded-full text-[10px] px-2.5 py-1.5 flex items-center justify-center whitespace-nowrap ${
         isSelected
            ? "bg-[#003580] text-white scale-110"
            : "bg-[#006ce4] text-white hover:bg-[#003580] hover:scale-105"
      }">${text}</div>`,
      className: "custom-price-tag",
      iconSize: [46, 28],
      iconAnchor: [23, 14],
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
   useEffect(() => {
      map.setView(center, zoom);
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

   const mapCenter = useMemo<[number, number]>(() => {
      if (properties.length > 0 && properties[0].location) {
         return [properties[0].location.lat, properties[0].location.lon];
      }
      return center;
   }, [properties, center]);

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

   return (
      <div className="w-full h-full min-h-[400px] rounded-xl overflow-hidden shadow-md border border-gray-100">
         <MapContainer
            center={mapCenter}
            zoom={zoom}
            className="w-full h-full"
            style={{ width: "100%", height: "100%", zIndex: 1 }}
            zoomControl={showControls}
            attributionControl={false}
         >
            <ChangeMapView center={mapCenter} zoom={zoom} />
            <TileLayer url={tileUrl} attribution={attribution} />

            {properties.map((property) => {
               if (!property.location) return null;
               const isSelected = selectedProperty?.id === property.id;

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
