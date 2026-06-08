"use client";

import { useState, useRef, useEffect } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { format, differenceInDays, parseISO } from "date-fns";
import { vi, enUS } from "date-fns/locale";
import { DateRange } from "react-day-picker";
import {
   Coffee,
   Compass,
   User,
   Check,
   Calendar,
   Users,
   Plus,
   Minus,
   ChevronDown,
   Search,
   AlertTriangle,
   Loader2,
} from "lucide-react";
import PriceDisplay from "./PriceDisplay";
import DateRangePicker from "./DateRangePicker";
import { bookingService, StayPriceResult } from "@omnibooking/shared";

interface RoomType {
   id: string;
   name: string;
   description: string;
   basePrice: number;
   roomSizeSqm?: number;
   bedType?: string;
   capacityAdults: number;
   capacityChildren: number;
   currentPrice?: number;
}

interface RoomPricingSectionProps {
   propertyId: string;
   roomTypes: RoomType[];
}

const dateLocales: Record<string, typeof vi> = {
   vi,
   en: enUS,
};

export default function RoomPricingSection({ propertyId, roomTypes }: RoomPricingSectionProps) {
   const t = useTranslations("PropertyDetail");
   const tc = useTranslations("Common");
   const locale = useLocale();
   const router = useRouter();
   const pathname = usePathname();
   const searchParams = useSearchParams();
   const dateLocale = dateLocales[locale] || enUS;

   // Parse current searchParams
   const checkinParam = searchParams.get("checkin");
   const checkoutParam = searchParams.get("checkout");
   const adultsParam = Number(searchParams.get("group_adults")) || 2;
   const childrenParam = Number(searchParams.get("group_children")) || 0;
   const roomsParam = Number(searchParams.get("no_rooms")) || 1;

   const hasDates = !!(checkinParam && checkoutParam);

   // Local UI inputs state
   const [date, setDate] = useState<DateRange | undefined>(() => {
      if (checkinParam && checkoutParam) {
         try {
            return {
               from: parseISO(checkinParam),
               to: parseISO(checkoutParam),
            };
         } catch (e) {
            console.error("Failed to parse searchParams dates", e);
         }
      }
      return undefined;
   });

   const [guests, setGuests] = useState({
      adults: adultsParam,
      children: childrenParam,
      rooms: roomsParam,
   });

   const [isDateOpen, setIsDateOpen] = useState(false);
   const [isGuestOpen, setIsGuestOpen] = useState(false);
   const [isEditing, setIsEditing] = useState(!hasDates);

   const [calculatedPrices, setCalculatedPrices] = useState<Record<string, StayPriceResult>>({});
   const [pricingLoading, setPricingLoading] = useState(false);

   useEffect(() => {
      if (hasDates && propertyId && roomTypes.length > 0) {
         let active = true;
         const fetchPricing = async () => {
            setPricingLoading(true);
            try {
               const promises = roomTypes.map((room) =>
                  bookingService.calculatePrice({
                     propertyId,
                     roomTypeId: room.id,
                     checkIn: checkinParam!,
                     checkOut: checkoutParam!,
                     guestCount: adultsParam + childrenParam,
                  })
               );
               const results = await Promise.all(promises);
               if (active) {
                  const pricingMap: Record<string, StayPriceResult> = {};
                  roomTypes.forEach((room, index) => {
                     pricingMap[room.id] = results[index];
                  });
                  setCalculatedPrices(pricingMap);
               }
            } catch (error) {
               console.error("Failed to calculate dynamic pricing", error);
            } finally {
               if (active) {
                  setPricingLoading(false);
               }
            }
         };

         fetchPricing();
         return () => {
            active = false;
         };
      } else {
         Promise.resolve().then(() => {
            setCalculatedPrices((prev) => (Object.keys(prev).length === 0 ? prev : {}));
         });
      }
   }, [hasDates, propertyId, roomTypes, checkinParam, checkoutParam, adultsParam, childrenParam]);

   const dateRef = useRef<HTMLDivElement>(null);
   const guestRef = useRef<HTMLDivElement>(null);

   // Track last seen search params to detect updates
   const [prevParams, setPrevParams] = useState({
      checkin: checkinParam,
      checkout: checkoutParam,
      adults: adultsParam,
      children: childrenParam,
      rooms: roomsParam,
   });

   // Adjust state during render when search params change
   if (
      prevParams.checkin !== checkinParam ||
      prevParams.checkout !== checkoutParam ||
      prevParams.adults !== adultsParam ||
      prevParams.children !== childrenParam ||
      prevParams.rooms !== roomsParam
   ) {
      setPrevParams({
         checkin: checkinParam,
         checkout: checkoutParam,
         adults: adultsParam,
         children: childrenParam,
         rooms: roomsParam,
      });

      if (checkinParam && checkoutParam) {
         try {
            setDate({
               from: parseISO(checkinParam),
               to: parseISO(checkoutParam),
            });
            setIsEditing(false);
         } catch (e) {
            console.error(e);
         }
      } else {
         setIsEditing(true);
      }

      setGuests({
         adults: adultsParam,
         children: childrenParam,
         rooms: roomsParam,
      });
   }

   // Handle click outside to close dropdowns
   useEffect(() => {
      function handleClickOutside(event: MouseEvent) {
         if (dateRef.current && !dateRef.current.contains(event.target as Node)) {
            setIsDateOpen(false);
         }
         if (guestRef.current && !guestRef.current.contains(event.target as Node)) {
            setIsGuestOpen(false);
         }
      }
      document.addEventListener("mousedown", handleClickOutside);
      return () => document.removeEventListener("mousedown", handleClickOutside);
   }, []);

   // Calculate duration of stay
   const nights =
      checkinParam && checkoutParam
         ? Math.max(1, differenceInDays(parseISO(checkoutParam), parseISO(checkinParam)))
         : 1;

   const handleSearch = () => {
      if (!date?.from || !date.to) {
         setIsDateOpen(true);
         return;
      }

      const params = new URLSearchParams(searchParams.toString());
      params.set("checkin", format(date.from, "yyyy-MM-dd"));
      params.set("checkout", format(date.to, "yyyy-MM-dd"));
      params.set("group_adults", guests.adults.toString());
      params.set("group_children", guests.children.toString());
      params.set("no_rooms", guests.rooms.toString());

      setIsEditing(false);
      router.push(`${pathname}?${params.toString()}`, { scroll: false });
   };

   const updateGuests = (type: keyof typeof guests, operation: "inc" | "dec") => {
      setGuests((prev) => {
         const val = prev[type];
         if (operation === "dec" && val > (type === "children" ? 0 : 1)) {
            return { ...prev, [type]: val - 1 };
         }
         if (operation === "inc" && val < 10) {
            return { ...prev, [type]: val + 1 };
         }
         return prev;
      });
   };

   const getCheckinStr = () => {
      if (!date?.from) return t("chooseDatesPrompt");
      if (!date.to) return `${format(date.from, "eee, d MMM yyyy", { locale: dateLocale })}`;
      return `${format(date.from, "eee, d MMM", { locale: dateLocale })} — ${format(date.to, "eee, d MMM yyyy", { locale: dateLocale })} (${t("nightsCount", { count: differenceInDays(date.to, date.from) })})`;
   };

   return (
      <section className="bg-white rounded-2xl p-6 md:p-8 border border-zinc-200/80 shadow-xs space-y-6">
         <div className="flex flex-col gap-3.5 pb-4 border-b border-zinc-100">
            <div className="flex items-center justify-between">
               <h2 className="text-xl font-bold text-zinc-900 flex items-center gap-2">
                  <Coffee className="h-5 w-5 text-[#006ce4]" />
                  <span>{t("selectRoom")}</span>
               </h2>
            </div>

            {/* Active search details ribbon */}
            {hasDates && !isEditing && (
               <div className="flex flex-wrap items-center justify-between gap-3 bg-blue-50/50 border border-blue-100 rounded-2xl px-5 py-3 text-xs text-zinc-700 font-medium animate-in fade-in duration-200">
                  <div className="flex flex-wrap items-center gap-x-2.5 gap-y-1">
                     <Calendar className="h-4 w-4 text-[#006ce4] shrink-0" />
                     <span className="font-bold text-[#006ce4]">
                        {format(parseISO(checkinParam!), "dd/MM/yyyy")} —{" "}
                        {format(parseISO(checkoutParam!), "dd/MM/yyyy")}
                     </span>
                     <span className="text-zinc-300 select-none">·</span>
                     <div className="flex-1 min-w-0">
                        <div className="text-sm font-bold text-zinc-900 truncate">
                           {t("nightsCount", { count: nights })}
                        </div>
                        <div className="text-xs text-zinc-500 truncate mt-0.5">
                           {t("adultsCount", { count: Number(adultsParam) })}
                           {childrenParam > 0 &&
                              `, ${t("childrenCount", { count: Number(childrenParam) })}`}
                           {` · ${t("roomsCount", { count: Number(roomsParam) })}`}
                        </div>
                     </div>
                  </div>
                  <button
                     onClick={() => setIsEditing(true)}
                     className="px-4 py-2 border border-zinc-200 rounded-xl text-xs font-bold text-zinc-600 hover:bg-zinc-50 active:scale-95 transition-all cursor-pointer"
                  >
                     {t("edit")}
                  </button>
               </div>
            )}
         </div>

         {/* Compact Date/Guest Selection Bar */}
         {isEditing && (
            <div className="bg-zinc-50/80 rounded-2xl border border-zinc-200/60 p-4 space-y-4 animate-in fade-in slide-in-from-top-4 duration-300">
               <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-stretch">
                  {/* Dates Selection */}
                  <div className="relative md:col-span-7" ref={dateRef}>
                     <div
                        onClick={() => {
                           setIsDateOpen(!isDateOpen);
                           setIsGuestOpen(false);
                        }}
                        className="bg-white border border-zinc-200 rounded-xl p-3.5 hover:border-zinc-300 transition-colors flex items-center gap-3 cursor-pointer h-full"
                     >
                        <Calendar className="h-4.5 w-4.5 text-[#006ce4] shrink-0" />
                        <div className="flex flex-col min-w-0">
                           <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">
                              {t("checkInOut")}
                           </span>
                           <span className="text-xs font-bold text-zinc-800 truncate">
                              {getCheckinStr()}
                           </span>
                        </div>
                        <ChevronDown className="h-4 w-4 text-zinc-400 shrink-0" />
                     </div>

                     {isDateOpen && (
                        <div className="absolute top-full left-0 mt-2 z-50 min-w-[700px] hidden md:block">
                           <DateRangePicker date={date} onDateChange={setDate} />
                        </div>
                     )}
                     {isDateOpen && (
                        <div className="absolute top-full left-0 mt-2 z-50 w-full md:hidden">
                           <DateRangePicker date={date} onDateChange={setDate} />
                        </div>
                     )}
                  </div>

                  {/* Guests Selection */}
                  <div className="relative md:col-span-5" ref={guestRef}>
                     <div
                        onClick={() => {
                           setIsGuestOpen(!isGuestOpen);
                           setIsDateOpen(false);
                        }}
                        className="bg-white border border-zinc-200 rounded-xl p-3.5 hover:border-zinc-300 transition-colors flex items-center gap-3 cursor-pointer h-full"
                     >
                        <Users className="h-4.5 w-4.5 text-[#006ce4] shrink-0" />
                        <div className="flex flex-col min-w-0">
                           <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">
                              {t("guestsRooms")}
                           </span>
                           <span className="text-xs font-bold text-zinc-800 truncate">
                              {t("adultsCount", { count: guests.adults })}
                              {guests.children > 0 &&
                                 `, ${t("childrenCount", { count: guests.children })}`}
                              {` · ${t("roomsCount", { count: guests.rooms })}`}
                           </span>
                        </div>
                        <ChevronDown className="h-4 w-4 text-zinc-400 shrink-0" />
                     </div>

                     {isGuestOpen && (
                        <div className="absolute top-full right-0 mt-2 w-full sm:w-[320px] bg-white rounded-2xl shadow-xl border border-zinc-200/80 p-5 z-50">
                           <div className="space-y-4">
                              <div className="flex items-center justify-between">
                                 <span className="text-xs font-bold text-zinc-800">
                                    {t("adultsLabel")}
                                 </span>
                                 <div className="flex items-center gap-3">
                                    <button
                                       type="button"
                                       onClick={() => updateGuests("adults", "dec")}
                                       className="h-7 w-7 rounded-full border border-zinc-200 flex items-center justify-center text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4]"
                                       disabled={guests.adults <= 1}
                                    >
                                       <Minus className="h-3 w-3" />
                                    </button>
                                    <span className="text-xs font-bold text-zinc-900 w-4 text-center">
                                       {guests.adults}
                                    </span>
                                    <button
                                       type="button"
                                       onClick={() => updateGuests("adults", "inc")}
                                       className="h-7 w-7 rounded-full border border-zinc-200 flex items-center justify-center text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4]"
                                    >
                                       <Plus className="h-3 w-3" />
                                    </button>
                                 </div>
                              </div>

                              <div className="flex items-center justify-between">
                                 <span className="text-xs font-bold text-zinc-800">
                                    {t("childrenLabel")}
                                 </span>
                                 <div className="flex items-center gap-3">
                                    <button
                                       type="button"
                                       onClick={() => updateGuests("children", "dec")}
                                       className="h-7 w-7 rounded-full border border-zinc-200 flex items-center justify-center text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4]"
                                       disabled={guests.children <= 0}
                                    >
                                       <Minus className="h-3 w-3" />
                                    </button>
                                    <span className="text-xs font-bold text-zinc-900 w-4 text-center">
                                       {guests.children}
                                    </span>
                                    <button
                                       type="button"
                                       onClick={() => updateGuests("children", "inc")}
                                       className="h-7 w-7 rounded-full border border-zinc-200 flex items-center justify-center text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4]"
                                    >
                                       <Plus className="h-3 w-3" />
                                    </button>
                                 </div>
                              </div>

                              <div className="flex items-center justify-between">
                                 <span className="text-xs font-bold text-zinc-800">
                                    {t("roomsLabel")}
                                 </span>
                                 <div className="flex items-center gap-3">
                                    <button
                                       type="button"
                                       onClick={() => updateGuests("rooms", "dec")}
                                       className="h-7 w-7 rounded-full border border-zinc-200 flex items-center justify-center text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4]"
                                       disabled={guests.rooms <= 1}
                                    >
                                       <Minus className="h-3 w-3" />
                                    </button>
                                    <span className="text-xs font-bold text-zinc-900 w-4 text-center">
                                       {guests.rooms}
                                    </span>
                                    <button
                                       type="button"
                                       onClick={() => updateGuests("rooms", "inc")}
                                       className="h-7 w-7 rounded-full border border-zinc-200 flex items-center justify-center text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4]"
                                    >
                                       <Plus className="h-3 w-3" />
                                    </button>
                                 </div>
                              </div>

                              <button
                                 type="button"
                                 onClick={() => setIsGuestOpen(false)}
                                 className="w-full bg-[#006ce4]/10 hover:bg-[#006ce4]/20 text-[#006ce4] text-xs font-bold py-2 rounded-lg transition-colors mt-2"
                              >
                                 {tc("done")}
                              </button>
                           </div>
                        </div>
                     )}
                  </div>
               </div>

               <div className="flex flex-col sm:flex-row justify-between items-center gap-3 pt-2">
                  <div className="flex items-center gap-2 text-zinc-500 text-xs font-medium">
                     <AlertTriangle className="h-4 w-4 text-amber-500 shrink-0" />
                     <span>{t("selectDatesPrompt")}</span>
                  </div>
                  <button
                     onClick={handleSearch}
                     className="w-full sm:w-auto bg-[#006ce4] hover:bg-[#0057b7] text-white px-6 py-3.5 rounded-xl font-bold text-sm transition-all active:scale-95 shadow-md flex items-center justify-center gap-2 cursor-pointer"
                  >
                     <Search className="h-4 w-4" />
                     <span>{t("checkAvailability")}</span>
                  </button>
               </div>
            </div>
         )}

         {/* Rooms Selection List */}
         <div className="space-y-6">
            {roomTypes &&
               roomTypes.map((room) => {
                  const originalRoomPrice = room.basePrice * 1.5;

                  return (
                     <div
                        key={room.id}
                        className={`border rounded-xl p-5 hover:border-[#006ce4] transition-all duration-300 hover:shadow-xs ${
                           hasDates ? "border-zinc-200" : "border-zinc-200 bg-zinc-50/30"
                        }`}
                     >
                        <div className="flex flex-col md:flex-row md:items-start justify-between gap-4">
                           <div className="space-y-2 flex-1">
                              <h3 className="text-lg font-bold text-zinc-900 hover:text-[#006ce4] transition-colors leading-tight">
                                 {room.name}
                              </h3>
                              <p className="text-xs text-zinc-500 font-medium leading-relaxed">
                                 {room.description}
                              </p>

                              {/* Badges/Configurations */}
                              <div className="flex flex-wrap items-center gap-x-4 gap-y-2 pt-2 text-xs font-semibold text-zinc-700">
                                 {room.roomSizeSqm && (
                                    <span className="flex items-center gap-1 bg-zinc-50 px-2 py-1 rounded border border-zinc-200">
                                       <Compass className="h-3.5 w-3.5 text-zinc-500" />
                                       {room.roomSizeSqm} {t("sqm")}
                                    </span>
                                 )}
                                 {room.bedType && (
                                    <span className="flex items-center gap-1 bg-zinc-50 px-2 py-1 rounded border border-zinc-200">
                                       <User className="h-3.5 w-3.5 text-zinc-500" />
                                       {room.bedType}
                                    </span>
                                 )}
                                 <span className="flex items-center gap-1 bg-zinc-50 px-2 py-1 rounded border border-zinc-200">
                                    <User className="h-3.5 w-3.5 text-zinc-500" />
                                    {t("capacityAdultsCount", { count: room.capacityAdults })}
                                    {room.capacityChildren > 0 &&
                                       ` ${t("capacityChildrenCount", { count: room.capacityChildren })}`}
                                 </span>
                              </div>

                              {/* Policies info */}
                              <div className="space-y-1 pt-3 text-xs">
                                 <div className="flex items-center gap-1.5 text-[#008009] font-bold">
                                    <Check className="h-4 w-4" />
                                    <span>{t("freeCancel")}</span>
                                 </div>
                                 <div className="flex items-center gap-1.5 text-[#008009] font-bold">
                                    <Check className="h-4 w-4" />
                                    <span>
                                       {t("noPrepay")} -{" "}
                                       <span className="text-zinc-500 font-medium">
                                          {t("payAtProperty")}
                                       </span>
                                    </span>
                                 </div>
                              </div>
                           </div>

                           {/* Pricing & CTA */}
                           <div className="flex flex-col items-end justify-between shrink-0 text-right self-stretch md:self-auto border-t md:border-t-0 md:border-l border-zinc-100 pt-4 md:pt-0 md:pl-6">
                              {hasDates ? (
                                 /* Prices are visible */
                                 <div className="space-y-1 mb-4 md:mb-0 w-full flex flex-col items-end animate-in fade-in duration-300">
                                    <span className="text-xs text-zinc-400 block font-semibold uppercase">
                                       {t("priceNight")}
                                    </span>
                                    {/* Original price */}
                                    <span className="text-xs text-red-600 line-through block font-medium">
                                       <PriceDisplay
                                          amount={originalRoomPrice}
                                          size="sm"
                                          className="text-xs text-red-600 line-through font-medium"
                                       />
                                    </span>
                                    {/* Price */}
                                    <div
                                       className={`flex items-baseline justify-end gap-1.5 transition-opacity duration-200 ${pricingLoading ? "opacity-50" : "opacity-100"}`}
                                    >
                                       {pricingLoading && (
                                          <Loader2 className="h-4 w-4 text-[#006ce4] animate-spin self-center mr-1" />
                                       )}
                                       <PriceDisplay
                                          amount={
                                             calculatedPrices[room.id]
                                                ? calculatedPrices[room.id].totalFinalPrice / nights
                                                : room.basePrice
                                          }
                                          size="lg"
                                          className="text-[#006ce4] font-extrabold text-2xl leading-none"
                                       />
                                    </div>
                                    <span className="text-[10px] text-zinc-500 block font-medium">
                                       {t("included")}
                                    </span>

                                    {/* Adjustment Badges */}
                                    {calculatedPrices[room.id] && !pricingLoading && (
                                       <div className="flex flex-wrap gap-1 justify-end mt-1 max-w-[200px]">
                                          {calculatedPrices[room.id].totalSeasonalAdjustment >
                                             0 && (
                                             <span className="text-[9px] font-black text-amber-700 bg-amber-50 border border-amber-100 px-1.5 py-0.5 rounded">
                                                {t("seasonalRate")}
                                             </span>
                                          )}
                                          {calculatedPrices[room.id].totalWeekendAdjustment > 0 && (
                                             <span className="text-[9px] font-black text-indigo-700 bg-indigo-50 border border-indigo-100 px-1.5 py-0.5 rounded">
                                                {t("weekendRate")}
                                             </span>
                                          )}
                                          {calculatedPrices[room.id].totalOccupancyAdjustment >
                                             0 && (
                                             <span className="text-[9px] font-black text-rose-700 bg-rose-50 border border-rose-100 px-1.5 py-0.5 rounded">
                                                {t("extraGuests")}
                                             </span>
                                          )}
                                          {calculatedPrices[room.id].totalSeasonalAdjustment <
                                             0 && (
                                             <span className="text-[9px] font-black text-emerald-700 bg-emerald-50 border border-emerald-100 px-1.5 py-0.5 rounded">
                                                {t("seasonalDiscount")}
                                             </span>
                                          )}
                                       </div>
                                    )}

                                    {/* Duration summary pricing */}
                                    {nights > 1 && (
                                       <div
                                          className={`text-[10px] font-bold text-zinc-700 bg-zinc-50 border border-zinc-100 rounded px-1.5 py-0.5 mt-2 transition-opacity duration-200 ${pricingLoading ? "opacity-50" : "opacity-100"}`}
                                       >
                                          {t("totalNightsPrice", { count: nights })}
                                          <span className="text-[#006ce4] font-extrabold">
                                             <PriceDisplay
                                                amount={
                                                   calculatedPrices[room.id]
                                                      ? calculatedPrices[room.id].totalFinalPrice
                                                      : room.basePrice * nights
                                                }
                                                size="sm"
                                                className="inline-block"
                                             />
                                          </span>
                                       </div>
                                    )}

                                    <button
                                       onClick={() => {
                                          router.push(
                                             `/${locale}/booking?propertyId=${propertyId}&roomTypeId=${room.id}&checkin=${checkinParam}&checkout=${checkoutParam}&rooms=${roomsParam}`
                                          );
                                       }}
                                       className="bg-[#006ce4] hover:bg-[#0057b7] text-white px-5 py-3 rounded-lg font-bold text-xs transition-all active:scale-[0.98] cursor-pointer shadow-md hover:shadow-lg w-full md:w-auto mt-4"
                                    >
                                       {t("reserve")}
                                    </button>
                                 </div>
                              ) : (
                                 /* Prices are hidden/locked until dates are selected */
                                 <div className="mb-4 md:mb-0 w-full flex flex-col items-end justify-center h-full min-h-[85px] select-none">
                                    <button
                                       onClick={() => {
                                          setIsEditing(true);
                                          window.scrollTo({
                                             top:
                                                (document.getElementById("available-rooms")
                                                   ?.offsetTop || 500) - 200,
                                             behavior: "smooth",
                                          });
                                       }}
                                       className="bg-[#006ce4]/10 hover:bg-[#006ce4]/20 text-[#006ce4] border border-[#006ce4]/20 px-5 py-3 rounded-lg font-bold text-xs transition-all active:scale-[0.98] cursor-pointer w-full md:w-auto"
                                    >
                                       {t("chooseDatesToViewRates")}
                                    </button>
                                 </div>
                              )}
                           </div>
                        </div>
                     </div>
                  );
               })}
         </div>
      </section>
   );
}
