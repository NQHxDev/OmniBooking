"use client";

import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { PropertyDocument } from "@/lib/api/services/propertyService";
import * as React from "react";
import Image from "next/image";

interface MapViewProps {
   properties: PropertyDocument[];
   center?: [number, number];
   zoom?: number;
   showControls?: boolean;
   showAttribution?: boolean;
}

// Component to handle auto-centering when properties change
function ChangeView({ center, zoom }: { center: [number, number]; zoom: number }) {
   const map = useMap();
   React.useEffect(() => {
      map.setView(center, zoom);
   }, [center, zoom, map]);
   return null;
}

export default function MapView({
   properties,
   center = [10.762622, 106.660172],
   zoom = 13,
   showControls = true,
   showAttribution = true,
}: MapViewProps) {
   // Use the first property as center if available
   const mapCenter =
      properties.length > 0 && properties[0].location
         ? ([properties[0].location.lat, properties[0].location.lon] as [number, number])
         : center;

   return (
      <MapContainer
         center={mapCenter}
         zoom={zoom}
         scrollWheelZoom={showControls}
         zoomControl={showControls}
         attributionControl={showAttribution}
         style={{ height: "100%", width: "100%", borderRadius: "inherit" }}
      >
         <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            detectRetina={true}
         />
         {/* Apply a custom filter to the map tiles via CSS in the MapContainer or here */}
         <style
            dangerouslySetInnerHTML={{
               __html: `
            .leaflet-tile-container {
               filter: grayscale(0.1) contrast(0.9) brightness(1.05) saturate(0.9);
            }
         `,
            }}
         />
         <ChangeView center={mapCenter} zoom={zoom} />

         {properties.map((property) => {
            if (!property.location) return null;

            const priceText =
               property.minPrice >= 1000000
                  ? (property.minPrice / 1000000).toFixed(1) + "tr"
                  : (property.minPrice / 1000).toFixed(0) + "k";

            const customIcon = L.divIcon({
               className: "custom-div-icon",
               html: `<div style="background-color: #006ce4; color: white; padding: 4px 8px; border-radius: 12px; font-weight: bold; font-size: 11px; box-shadow: 0 2px 4px rgba(0,0,0,0.2); white-space: nowrap; border: 2px solid white; cursor: pointer;">${priceText}</div>`,
               iconSize: [45, 24],
               iconAnchor: [22, 12],
            });

            return (
               <Marker
                  key={property.id}
                  position={[property.location.lat, property.location.lon]}
                  icon={customIcon}
               >
                  <Popup>
                     <div className="p-1 max-w-[200px]">
                        <div className="relative w-full h-24 mb-2">
                           <Image
                              src={property.mainImageUrl}
                              alt={property.name}
                              fill
                              className="object-cover rounded-md"
                           />
                        </div>
                        <h4 className="font-bold text-sm leading-tight mb-1">{property.name}</h4>
                        <p className="text-[#006ce4] font-bold text-sm">
                           {property.minPrice.toLocaleString("vi-VN")}đ
                        </p>
                     </div>
                  </Popup>
               </Marker>
            );
         })}
      </MapContainer>
   );
}
