"use client";

import { GoogleMap, useJsApiLoader, MarkerF, InfoWindow } from "@react-google-maps/api";
import { PropertyDocument } from "@/lib/api/services/propertyService";
import { useState, useCallback } from "react";
import Image from "next/image";
import { Loader2 } from "lucide-react";

const containerStyle = {
   width: "100%",
   height: "100%",
};

const defaultCenter = {
   lat: 10.762622,
   lng: 106.660172,
};

interface MapViewProps {
   properties: PropertyDocument[];
   center?: [number, number];
   zoom?: number;
   showControls?: boolean;
   showAttribution?: boolean;
}

export default function MapView({
   properties,
   center = [10.762622, 106.660172],
   zoom = 13,
}: MapViewProps) {
   const { isLoaded } = useJsApiLoader({
      id: "google-map-script",
      googleMapsApiKey: process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY || "",
   });

   const [selectedProperty, setSelectedProperty] = useState<PropertyDocument | null>(null);

   const mapCenter =
      properties.length > 0 && properties[0].location
         ? { lat: properties[0].location.lat, lng: properties[0].location.lon }
         : { lat: center[0], lng: center[1] };

   if (!isLoaded) {
      return (
         <div className="w-full h-full min-h-[400px] bg-gray-50 flex items-center justify-center rounded-xl">
            <Loader2 className="h-8 w-8 text-[#006ce4] animate-spin" />
         </div>
      );
   }

   return (
      <GoogleMap
         mapContainerStyle={containerStyle}
         center={mapCenter}
         zoom={zoom}
         options={{
            disableDefaultUI: false,
            zoomControl: true,
            mapTypeControl: false,
            streetViewControl: false,
            fullscreenControl: true,
         }}
      >
         {properties.map((property) => {
            if (!property.location) return null;

            return (
               <MarkerF
                  key={property.id}
                  position={{ lat: property.location.lat, lng: property.location.lon }}
                  onClick={() => setSelectedProperty(property)}
                  // You can add a custom label or icon here to show price
                  label={{
                     text:
                        property.minPrice >= 1000000
                           ? (property.minPrice / 1000000).toFixed(1) + "tr"
                           : (property.minPrice / 1000).toFixed(0) + "k",
                     className:
                        "bg-[#006ce4] text-white px-2 py-1 rounded-full text-[10px] font-bold border-2 border-white",
                  }}
               />
            );
         })}

         {selectedProperty && selectedProperty.location && (
            <InfoWindow
               position={{ lat: selectedProperty.location.lat, lng: selectedProperty.location.lon }}
               onCloseClick={() => setSelectedProperty(null)}
            >
               <div className="p-1 max-w-[200px]">
                  <div className="relative w-full h-24 mb-2">
                     <Image
                        src={selectedProperty.mainImageUrl}
                        alt={selectedProperty.name}
                        fill
                        className="object-cover rounded-md"
                     />
                  </div>
                  <h4 className="font-bold text-sm leading-tight mb-1">{selectedProperty.name}</h4>
                  <p className="text-[#006ce4] font-bold text-sm">
                     {selectedProperty.minPrice.toLocaleString("vi-VN")}đ
                  </p>
               </div>
            </InfoWindow>
         )}
      </GoogleMap>
   );
}
