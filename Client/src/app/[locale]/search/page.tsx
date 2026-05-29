"use client";

import * as React from "react";
import { useTranslations, useLocale } from "next-intl";
import { useSearchParams } from "next/navigation";
import Navbar from "@/components/Navbar";
import { Search, MapPin, Filter, SlidersHorizontal, Loader2 } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { propertyService, PropertyDocument } from "@/lib/api/services/propertyService";
import PropertyCard from "@/components/PropertyCard";
import MapView from "@/components/Map";
import { useSettingStore } from "@/store/useSettingStore";
import apiClient from "@/lib/api/apiClient";

interface BudgetFilterProps {
   currency: string;
   rates?: Record<string, number>;
   searchParams: ReturnType<typeof useSearchParams>;
}

function BudgetFilter({ currency, rates, searchParams }: BudgetFilterProps) {
   const ts = useTranslations("Search");
   const isVnd = currency === "VND";
   const sliderMin = isVnd ? 200000 : 10;
   const sliderMax = isVnd ? 5000000 : 200;
   const sliderStep = isVnd ? 100000 : 5;

   const [priceRange, setPriceRange] = React.useState<[number, number]>(() => [
      Number(searchParams.get("minPrice")) || (currency === "VND" ? 200000 : 10),
      Number(searchParams.get("maxPrice")) || (currency === "VND" ? 5000000 : 200),
   ]);

   const [isMounted, setIsMounted] = React.useState(false);

   React.useEffect(() => {
      const handle = requestAnimationFrame(() => setIsMounted(true));
      return () => cancelAnimationFrame(handle);
   }, []);

   const handlePriceChange = (newRange: [number, number]) => {
      setPriceRange(newRange);
   };

   // Auto-apply filter after 1.5s of inactivity
   React.useEffect(() => {
      const timeoutId = setTimeout(() => {
         const currentMin =
            Number(searchParams.get("minPrice")) || (currency === "VND" ? 200000 : 10);
         const currentMax =
            Number(searchParams.get("maxPrice")) || (currency === "VND" ? 5000000 : 200);

         // Only update if values actually changed
         if (priceRange[0] !== currentMin || priceRange[1] !== currentMax) {
            const params = new URLSearchParams(searchParams.toString());
            params.set("minPrice", priceRange[0].toString());
            params.set("maxPrice", priceRange[1].toString());
            window.history.pushState(null, "", `?${params.toString()}`);
         }
      }, 1500);

      return () => clearTimeout(timeoutId);
   }, [priceRange, searchParams, currency]);

   return (
      <div>
         <div className="flex justify-between items-center mb-4">
            <h3 className="font-bold text-sm">{ts("budget")}</h3>
         </div>

         <div className="px-2 space-y-4">
            <div className="relative h-2 bg-zinc-100 rounded-full">
               <input
                  type="range"
                  min={sliderMin}
                  max={sliderMax}
                  step={sliderStep}
                  value={priceRange[0]}
                  onChange={(e) => handlePriceChange([Number(e.target.value), priceRange[1]])}
                  className="absolute w-full h-full appearance-none bg-transparent pointer-events-none [&::-webkit-slider-thumb]:pointer-events-auto [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-[#006ce4] [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-white [&::-webkit-slider-thumb]:shadow-md"
               />
               <input
                  type="range"
                  min={sliderMin}
                  max={sliderMax}
                  step={sliderStep}
                  value={priceRange[1]}
                  onChange={(e) => handlePriceChange([priceRange[0], Number(e.target.value)])}
                  className="absolute w-full h-full appearance-none bg-transparent pointer-events-none [&::-webkit-slider-thumb]:pointer-events-auto [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-[#006ce4] [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-white [&::-webkit-slider-thumb]:shadow-md"
               />
            </div>

            <div className="flex justify-between items-center text-[11px] font-bold text-zinc-600">
               <span>
                  {isMounted
                     ? isVnd
                        ? `${priceRange[0].toLocaleString("vi-VN")}đ`
                        : `$${priceRange[0]}`
                     : "---"}
               </span>
               <span>
                  {isMounted
                     ? priceRange[1] >= sliderMax
                        ? isVnd
                           ? "5.000.000đ+"
                           : "$200+"
                        : isVnd
                          ? `${priceRange[1].toLocaleString("vi-VN")}đ`
                          : `$${priceRange[1]}`
                     : "---"}
               </span>
            </div>
         </div>
      </div>
   );
}

export default function SearchResultsPage() {
   const t = useTranslations("Common");
   const ts = useTranslations("Search");
   const tp = useTranslations("Partner");
   const searchParams = useSearchParams();
   const locale = useLocale();

   const rawDestination = searchParams.get("ss") || "";
   const destination = React.useMemo(() => {
      if (locale === "en") {
         switch (rawDestination) {
            case "Hồ Chí Minh":
               return "Ho Chi Minh City";
            case "Hà Nội":
               return "Hanoi";
            case "Đà Nẵng":
               return "Da Nang";
            case "Hội An":
               return "Hoi An";
            case "Phú Quốc":
               return "Phu Quoc";
            case "Quảng Ninh":
               return "Quang Ninh";
            case "Hạ Long":
               return "Ha Long";
            case "Nha Trang":
               return "Nha Trang";
            case "Đà Lạt":
               return "Da Lat";
            case "Vũng Tàu":
               return "Vung Tau";
            case "Sapa":
               return "Sapa";
            case "Huế":
               return "Hue";
            case "Hải Phòng":
               return "Hai Phong";
            case "Cần Thơ":
               return "Can Tho";
            default:
               return rawDestination;
         }
      } else {
         switch (rawDestination) {
            case "Ho Chi Minh City":
            case "Ho Chi Minh":
               return "Hồ Chí Minh";
            case "Hanoi":
               return "Hà Nội";
            case "Da Nang":
               return "Đà Nẵng";
            case "Hoi An":
               return "Hội An";
            case "Phu Quoc":
               return "Phú Quốc";
            case "Quang Ninh":
               return "Quảng Ninh";
            case "Ha Long":
               return "Hạ Long";
            case "Nha Trang":
               return "Nha Trang";
            case "Da Lat":
               return "Đà Lạt";
            case "Vung Tau":
               return "Vũng Tàu";
            case "Hue":
               return "Huế";
            case "Hai Phong":
               return "Hải Phòng";
            case "Can Tho":
               return "Cần Thơ";
            default:
               return rawDestination;
         }
      }
   }, [rawDestination, locale]);
   const checkin = searchParams.get("checkin");
   const checkout = searchParams.get("checkout");

   const { currency } = useSettingStore();
   const { data: rates } = useQuery({
      queryKey: ["currency-rates"],
      queryFn: async () => {
         const response = await apiClient.get<unknown, Record<string, number>>("/currencies/rates");
         return response;
      },
      staleTime: 1000 * 60 * 60, // 1 hour
   });

   const rate = rates?.[currency] || 1;
   const minPriceVal = searchParams.get("minPrice")
      ? Number(searchParams.get("minPrice"))
      : undefined;
   const maxPriceVal = searchParams.get("maxPrice")
      ? Number(searchParams.get("maxPrice"))
      : undefined;

   const usdMinPrice =
      minPriceVal !== undefined
         ? currency === "USD"
            ? minPriceVal
            : minPriceVal / rate
         : undefined;
   const usdMaxPrice =
      maxPriceVal !== undefined
         ? currency === "USD"
            ? maxPriceVal
            : maxPriceVal / rate
         : undefined;

   const { data: results, isLoading } = useQuery({
      queryKey: ["search", searchParams.toString(), currency, rate],
      queryFn: () =>
         propertyService.search({
            ss: destination,
            minPrice: usdMinPrice,
            maxPrice: usdMaxPrice,
            stars: searchParams.get("stars") ? Number(searchParams.get("stars")) : undefined,
            propertyType: searchParams.get("propertyType") || undefined,
            amenities: searchParams.getAll("amenities"),
            minRating: searchParams.get("minRating")
               ? Number(searchParams.get("minRating"))
               : undefined,
            page: 0,
            size: 20,
         }),
   });

   const properties = results?.content || [];

   // Sync URL params when currency changes
   React.useEffect(() => {
      const currentMin = Number(searchParams.get("minPrice"));
      const currentMax = Number(searchParams.get("maxPrice"));

      if (currentMin && currentMax) {
         if (currency === "VND" && currentMin < 1000) {
            // Price was in USD, convert to VND
            const rateVal = rates?.["VND"] || 27000;
            const newMin = Math.round((currentMin * rateVal) / 100000) * 100000;
            const newMax = Math.round((currentMax * rateVal) / 100000) * 100000;

            const params = new URLSearchParams(searchParams.toString());
            params.set("minPrice", newMin.toString());
            params.set("maxPrice", newMax.toString());
            window.history.replaceState(null, "", `?${params.toString()}`);
         } else if (currency === "USD" && currentMin > 10000) {
            // Price was in VND, convert to USD
            const rateVal = rates?.["VND"] || 27000;
            const newMin = Math.round(currentMin / rateVal / 5) * 5;
            const newMax = Math.round(currentMax / rateVal / 5) * 5;

            const params = new URLSearchParams(searchParams.toString());
            params.set("minPrice", newMin.toString());
            params.set("maxPrice", newMax.toString());
            window.history.replaceState(null, "", `?${params.toString()}`);
         }
      }
   }, [currency, searchParams, rates]);

   const [showFullMap, setShowFullMap] = React.useState(false);

   return (
      <div className="flex min-h-screen flex-col bg-[#f5f5f5]">
         <Navbar />

         {/* Header / Search Summary */}
         <div className="bg-[#003580] py-4 text-white">
            <div className="mx-auto max-w-7xl px-4 flex items-center justify-between">
               <div className="flex items-center gap-4">
                  <div className="bg-white/10 p-2 rounded-lg">
                     <MapPin className="h-5 w-5" />
                  </div>
                  <div>
                     <h1 className="text-lg font-bold">{destination || ts("allProperties")}</h1>
                     <p className="text-xs text-zinc-300">
                        {checkin} — {checkout} · {searchParams.get("group_adults")}{" "}
                        {t("Search.adults" as const)}
                     </p>
                  </div>
               </div>
               <button className="bg-white text-[#003580] px-4 py-2 rounded-md text-sm font-bold hover:bg-zinc-100 transition-colors">
                  {ts("changeSearch")}
               </button>
            </div>
         </div>

         <main className="mx-auto w-full max-w-7xl px-4 py-8 flex gap-8">
            {/* Sidebar Filters */}
            <aside className="w-72 shrink-0 hidden md:block">
               {/* Map Preview Card */}
               <div className="bg-white rounded-xl border border-zinc-200 overflow-hidden mb-4 p-1">
                  <div
                     className="relative h-32 rounded-lg overflow-hidden group cursor-pointer"
                     onClick={() => setShowFullMap(true)}
                  >
                     <MapView
                        properties={properties}
                        zoom={11}
                        showControls={false}
                        showAttribution={false}
                        searchCity={rawDestination}
                     />
                     <div className="absolute inset-0 bg-black/5 group-hover:bg-black/0 transition-colors z-1001" />
                     <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-1002">
                        <button className="bg-white text-[#006ce4] px-6 py-2.5 rounded-md text-sm font-bold shadow-lg flex items-center gap-2 hover:bg-zinc-50 transition-all border border-zinc-200 pointer-events-none whitespace-nowrap min-w-[180px] justify-center">
                           <MapPin className="h-4 w-4 shrink-0" />
                           <span>{ts("viewOnMap")}</span>
                        </button>
                     </div>
                  </div>
               </div>

               <div className="bg-white rounded-xl border border-zinc-200 overflow-hidden sticky top-4">
                  <div className="p-4 border-b border-zinc-100 bg-zinc-50/50 flex items-center justify-between">
                     <span className="font-bold text-sm">{ts("filterBy")}</span>
                     <Filter className="h-4 w-4 text-zinc-400" />
                  </div>

                  <div className="p-4 space-y-6">
                     <BudgetFilter
                        key={`${currency}-${searchParams.get("minPrice")}-${searchParams.get("maxPrice")}`}
                        currency={currency}
                        rates={rates}
                        searchParams={searchParams}
                     />

                     <div>
                        <h3 className="font-bold text-sm mb-3">{ts("propertyTypeLabel")}</h3>
                        <div className="space-y-3">
                           {["HOTEL", "APARTMENT", "VILLA", "RESORT", "HOMESTAY"].map((type) => (
                              <label
                                 key={type}
                                 className="flex items-center gap-3 cursor-pointer group"
                              >
                                 <div className="relative flex items-center justify-center">
                                    <input
                                       type="checkbox"
                                       className="peer sr-only"
                                       checked={searchParams.get("propertyType") === type}
                                       onChange={(e) => {
                                          const params = new URLSearchParams(
                                             searchParams.toString()
                                          );
                                          if (e.target.checked) params.set("propertyType", type);
                                          else params.delete("propertyType");
                                          window.history.pushState(
                                             null,
                                             "",
                                             `?${params.toString()}`
                                          );
                                       }}
                                    />
                                    <div className="w-5 h-5 border-2 border-zinc-300 rounded group-hover:border-[#006ce4] peer-checked:bg-[#006ce4] peer-checked:border-[#006ce4] transition-all duration-200 flex items-center justify-center">
                                       <div className="w-2.5 h-1.5 border-l-2 border-b-2 border-white -rotate-45 mb-0.5 scale-0 peer-checked:scale-100 transition-transform duration-200" />
                                    </div>
                                 </div>
                                 <span className="text-sm text-zinc-600 group-hover:text-black transition-colors">
                                    {tp(
                                       `dashboard.properties.type${type}` as "dashboard.properties.typeHOTEL"
                                    )}
                                 </span>
                              </label>
                           ))}
                        </div>
                     </div>

                     <div>
                        <h3 className="font-bold text-sm mb-3">{ts("popularAmenities")}</h3>
                        <div className="space-y-3">
                           {[
                              { id: "wifi", label: ts("amenitiesList.wifi") },
                              { id: "pool", label: ts("amenitiesList.pool") },
                              { id: "parking", label: ts("amenitiesList.parking") },
                              { id: "restaurant", label: ts("amenitiesList.restaurant") },
                              { id: "spa", label: ts("amenitiesList.spa") },
                           ].map((amenity) => (
                              <label
                                 key={amenity.id}
                                 className="flex items-center gap-3 cursor-pointer group"
                              >
                                 <div className="relative flex items-center justify-center">
                                    <input
                                       type="checkbox"
                                       className="peer sr-only"
                                       checked={searchParams
                                          .getAll("amenities")
                                          .includes(amenity.label)}
                                       onChange={(e) => {
                                          const params = new URLSearchParams(
                                             searchParams.toString()
                                          );
                                          const current = params.getAll("amenities");
                                          if (e.target.checked)
                                             params.append("amenities", amenity.label);
                                          else {
                                             params.delete("amenities");
                                             current
                                                .filter((a) => a !== amenity.label)
                                                .forEach((a) => params.append("amenities", a));
                                          }
                                          window.history.pushState(
                                             null,
                                             "",
                                             `?${params.toString()}`
                                          );
                                       }}
                                    />
                                    <div className="w-5 h-5 border-2 border-zinc-300 rounded group-hover:border-[#006ce4] peer-checked:bg-[#006ce4] peer-checked:border-[#006ce4] transition-all duration-200 flex items-center justify-center">
                                       <div className="w-2.5 h-1.5 border-l-2 border-b-2 border-white -rotate-45 mb-0.5 scale-0 peer-checked:scale-100 transition-transform duration-200" />
                                    </div>
                                 </div>
                                 <span className="text-sm text-zinc-600 group-hover:text-black transition-colors">
                                    {amenity.label}
                                 </span>
                              </label>
                           ))}
                        </div>
                     </div>

                     <div>
                        <h3 className="font-bold text-sm mb-3">{ts("reviewScore")}</h3>
                        <div className="space-y-3">
                           {[9, 8, 7].map((score) => (
                              <label
                                 key={score}
                                 className="flex items-center gap-3 cursor-pointer group"
                              >
                                 <div className="relative flex items-center justify-center">
                                    <input
                                       type="radio"
                                       name="minRating"
                                       className="peer sr-only"
                                       checked={searchParams.get("minRating") === score.toString()}
                                       onChange={() => {
                                          const params = new URLSearchParams(
                                             searchParams.toString()
                                          );
                                          params.set("minRating", score.toString());
                                          window.history.pushState(
                                             null,
                                             "",
                                             `?${params.toString()}`
                                          );
                                       }}
                                    />
                                    <div className="w-5 h-5 border-2 border-zinc-300 rounded-full group-hover:border-[#006ce4] peer-checked:border-[#006ce4] transition-all duration-200 flex items-center justify-center">
                                       <div className="w-2 h-2 bg-[#006ce4] rounded-full scale-0 peer-checked:scale-100 transition-transform duration-200" />
                                    </div>
                                 </div>
                                 <span className="text-sm text-zinc-600 group-hover:text-black transition-colors">
                                    {ts("excellentPlus", { score })}
                                 </span>
                              </label>
                           ))}
                        </div>
                     </div>
                  </div>
               </div>
            </aside>

            {/* Results List */}
            <div className="flex-1">
               <div className="flex items-center justify-between mb-6">
                  <h2 className="text-xl font-bold">
                     {ts("resultsFor", {
                        destination: destination || ts("allProperties"),
                        count: results?.totalElements || 0,
                     })}
                  </h2>
                  <div className="flex items-center gap-2">
                     <span className="text-sm text-zinc-500">{ts("sortBy")}</span>
                     <button className="flex items-center gap-2 bg-white border border-zinc-200 px-4 py-2 rounded-full text-sm font-bold">
                        {ts("popular")}
                        <SlidersHorizontal className="h-4 w-4" />
                     </button>
                  </div>
               </div>

               {/* Results Container */}
               <div className="space-y-4">
                  {isLoading ? (
                     <div className="bg-white rounded-2xl border border-zinc-100 p-20 flex flex-col items-center justify-center text-center">
                        <Loader2 className="h-10 w-10 text-[#006ce4] animate-spin mb-4" />
                        <p className="text-zinc-500 font-medium">{ts("searching")}</p>
                     </div>
                  ) : properties.length > 0 ? (
                     properties.map((property: PropertyDocument, index: number) => (
                        <PropertyCard key={property.id} property={property} index={index} />
                     ))
                  ) : (
                     <div className="bg-white rounded-2xl border border-zinc-100 p-20 flex flex-col items-center justify-center text-center">
                        <div className="bg-zinc-50 p-6 rounded-full mb-6">
                           <Search className="h-12 w-12 text-zinc-300" />
                        </div>
                        <h3 className="text-2xl font-bold mb-2">{ts("noResults")}</h3>
                        <p className="text-zinc-500 max-w-md">{ts("noResultsDesc")}</p>
                     </div>
                  )}
               </div>
            </div>
         </main>

         {/* Fullscreen Map Overlay */}
         {showFullMap && (
            <div className="fixed inset-0 z-9999 bg-white flex flex-col">
               <div className="h-16 border-b border-zinc-200 px-6 flex items-center justify-between bg-white">
                  <div className="flex items-center gap-4">
                     <h2 className="font-bold text-lg">
                        {ts("resultsFor", {
                           destination: destination || ts("allProperties"),
                           count: properties.length,
                        })}
                     </h2>
                  </div>
                  <button
                     onClick={() => setShowFullMap(false)}
                     className="bg-zinc-100 hover:bg-zinc-200 p-2 rounded-full transition-colors"
                  >
                     <Search className="h-6 w-6 rotate-45" />{" "}
                     {/* Close icon using rotated search */}
                  </button>
               </div>
               <div className="flex-1 relative">
                  <MapView properties={properties} zoom={13} searchCity={rawDestination} />
               </div>
            </div>
         )}
      </div>
   );
}
