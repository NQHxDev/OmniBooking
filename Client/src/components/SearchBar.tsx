"use client";

import { useState, useRef, useEffect } from "react";
import {
   Calendar,
   User as UserIcon,
   Search,
   MapPin,
   ChevronDown,
   Plus,
   Minus,
   Briefcase,
   Dog,
} from "lucide-react";
import { DateRange } from "react-day-picker";
import { format } from "date-fns";
import { vi, enUS } from "date-fns/locale";
import DateRangePicker from "./DateRangePicker";
import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "next/navigation";

const LOCATIONS = ["TP. Hồ Chí Minh", "Hà Nội", "Quảng Ninh", "Đà Nẵng"];

export default function SearchBar() {
   const t = useTranslations("Common");
   const locale = useLocale();
   const router = useRouter();
   const dateLocale = locale === "vi" ? vi : enUS;

   // Location State
   const [destination, setDestination] = useState("");
   const [isLocationOpen, setIsLocationOpen] = useState(false);
   const locationRef = useRef<HTMLDivElement>(null);

   // Date State
   const [date, setDate] = useState<DateRange | undefined>(undefined);
   const [isDateOpen, setIsDateOpen] = useState(false);
   const dateRef = useRef<HTMLDivElement>(null);

   // Guests State
   const [guests, setGuests] = useState({
      adults: 2,
      children: 0,
      rooms: 1,
   });
   const [isGuestOpen, setIsGuestOpen] = useState(false);
   const guestRef = useRef<HTMLDivElement>(null);
   const [isWorkTrip, setIsWorkTrip] = useState(false);
   const [isPets, setIsPets] = useState(false);

   const handleSearch = () => {
      const params = new URLSearchParams();
      if (destination) params.set("ss", destination);
      if (date?.from) params.set("checkin", format(date.from, "yyyy-MM-dd"));
      if (date?.to) params.set("checkout", format(date.to, "yyyy-MM-dd"));
      params.set("group_adults", guests.adults.toString());
      params.set("group_children", guests.children.toString());
      params.set("no_rooms", guests.rooms.toString());

      router.push(`/${locale}/search?${params.toString()}`);
   };

   // Close dropdowns when clicking outside
   useEffect(() => {
      function handleClickOutside(event: MouseEvent) {
         if (locationRef.current && !locationRef.current.contains(event.target as Node)) {
            setIsLocationOpen(false);
         }
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

   const formatDateRange = () => {
      if (!date?.from) return `${format(new Date(), "eee, d MMM", { locale: dateLocale })}`;
      if (!date.to) return `${format(date.from, "eee, d MMM", { locale: dateLocale })}`;

      return `${format(date.from, "eee, d MMM", { locale: dateLocale })} — ${format(date.to, "eee, d MMM", { locale: dateLocale })}`;
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

   return (
      <div className="relative mx-auto -mt-12 w-full max-w-6xl px-4 z-20">
         <div className="flex flex-col md:flex-row items-stretch bg-white/95 backdrop-blur-xl rounded-[2rem] md:rounded-full shadow-[0_20px_50px_rgba(0,0,0,0.12)] border border-white/20 p-2 md:p-3 gap-1">
            {/* Location Selector */}
            <div className="relative flex-1" ref={locationRef}>
               <div
                  onClick={() => {
                     setIsLocationOpen(!isLocationOpen);
                     setIsDateOpen(false);
                     setIsGuestOpen(false);
                  }}
                  className="flex items-center gap-4 px-6 py-2 rounded-full hover:bg-zinc-50 transition-colors group cursor-pointer h-full"
               >
                  <div className="h-8.5 w-8.5 rounded-full bg-zinc-100 flex items-center justify-center text-zinc-600 group-hover:scale-105 transition-transform">
                     <MapPin className="h-4.5 w-4.5" />
                  </div>
                  <div className="flex flex-col flex-1">
                     <span className="text-[9px] uppercase font-bold text-zinc-400 tracking-wider">
                        {t("destination") || "Địa điểm"}
                     </span>
                     <div className="flex items-center justify-between">
                        <span
                           className={`text-sm font-bold ${destination ? "text-black" : "text-zinc-400"}`}
                        >
                           {destination || t("searchPlaceholder") || "Bạn muốn đến đâu?"}
                        </span>
                        <ChevronDown
                           className={`h-4 w-4 text-zinc-400 transition-transform duration-300 ${isLocationOpen ? "rotate-180" : ""}`}
                        />
                     </div>
                  </div>
               </div>

               {/* Location Dropdown */}
               {isLocationOpen && (
                  <div className="absolute top-full left-0 mt-3 w-full min-w-[220px] bg-white rounded-2xl shadow-2xl border border-zinc-100 overflow-hidden py-2 animate-in fade-in zoom-in-95 duration-200 z-50">
                     {LOCATIONS.map((loc) => (
                        <button
                           key={loc}
                           onClick={() => {
                              setDestination(loc);
                              setIsLocationOpen(false);
                           }}
                           className={`w-full flex items-center gap-3 px-4 py-3 text-sm transition-colors hover:bg-blue-50/50 ${destination === loc ? "bg-blue-50 text-[#006ce4] font-bold" : "text-zinc-600 font-medium"}`}
                        >
                           <MapPin
                              className={`h-4 w-4 ${destination === loc ? "text-[#006ce4]" : "text-zinc-300"}`}
                           />
                           {loc}
                        </button>
                     ))}
                  </div>
               )}
            </div>

            {/* Vertical Divider (Desktop) */}
            <div className="hidden md:block w-px h-8 bg-zinc-100 self-center" />

            {/* Date Picker Section */}
            <div className="relative flex-1" ref={dateRef}>
               <div
                  onClick={() => {
                     setIsDateOpen(!isDateOpen);
                     setIsLocationOpen(false);
                     setIsGuestOpen(false);
                     if (!isDateOpen && (!date || !date.to)) {
                        setDate(undefined);
                     }
                  }}
                  className="flex items-center gap-4 px-6 py-2 rounded-full hover:bg-zinc-50 transition-colors group cursor-pointer h-full"
               >
                  <div className="h-8.5 w-8.5 rounded-full bg-zinc-100 flex items-center justify-center text-zinc-600 group-hover:scale-105 transition-transform">
                     <Calendar className="h-4.5 w-4.5" />
                  </div>
                  <div className="flex flex-col">
                     <span className="text-[9px] uppercase font-bold text-zinc-400 tracking-wider">
                        {t("checkInDate") || "Thời gian"}
                     </span>
                     <span className="text-sm font-bold text-black whitespace-nowrap">
                        {formatDateRange()}
                     </span>
                  </div>
               </div>

               {/* Date Picker Popover */}
               {isDateOpen && (
                  <div className="absolute top-full left-0 md:left-1/2 md:-translate-x-1/2 mt-3 z-50 min-w-[750px] hidden md:block">
                     <DateRangePicker date={date} onDateChange={setDate} />
                  </div>
               )}
            </div>

            {/* Vertical Divider (Desktop) */}
            <div className="hidden md:block w-px h-8 bg-zinc-100 self-center" />

            {/* Guests Section */}
            <div className="relative flex-[1.2]" ref={guestRef}>
               <div
                  onClick={() => {
                     setIsGuestOpen(!isGuestOpen);
                     setIsLocationOpen(false);
                     setIsDateOpen(false);
                  }}
                  className="flex items-center gap-4 px-6 py-2 rounded-full hover:bg-zinc-50 transition-colors group cursor-pointer h-full"
               >
                  <div className="h-8.5 w-8.5 rounded-full bg-zinc-100 flex items-center justify-center text-zinc-600 group-hover:scale-105 transition-transform">
                     <UserIcon className="h-4.5 w-4.5" />
                  </div>
                  <div className="flex flex-col">
                     <span className="text-[9px] uppercase font-bold text-zinc-400 tracking-wider">
                        {t("travelers") || "Số khách"}
                     </span>
                     <span className="text-sm font-bold text-black whitespace-nowrap">
                        {guests.adults} {t("adults") || "người lớn"}
                        {guests.children > 0
                           ? ` · ${guests.children} ${t("children") || "trẻ em"}`
                           : ""}{" "}
                        · {guests.rooms} {t("rooms") || "phòng"}
                     </span>
                     {(isWorkTrip || isPets) && (
                        <div className="flex items-center gap-2 mt-0.5">
                           {isWorkTrip && (
                              <span className="text-[10px] font-bold text-zinc-600 flex items-center gap-1 bg-zinc-100 px-1.5 py-0.5 rounded-md">
                                 <Briefcase className="h-2.5 w-2.5" /> {t("workTrip") || "Công tác"}
                              </span>
                           )}
                           {isPets && (
                              <span className="text-[10px] font-bold text-zinc-600 flex items-center gap-1 bg-zinc-100 px-1.5 py-0.5 rounded-md">
                                 <Dog className="h-2.5 w-2.5" /> {t("pets") || "Thú cưng"}
                              </span>
                           )}
                        </div>
                     )}
                  </div>
               </div>

               {/* Guests Dropdown */}
               {isGuestOpen && (
                  <div className="absolute top-full right-0 mt-3 w-[340px] bg-white rounded-[2rem] shadow-2xl border border-zinc-100 p-6 animate-in fade-in zoom-in-95 duration-200 z-50">
                     <div className="space-y-6">
                        {/* Adults */}
                        <div className="flex items-center justify-between">
                           <div className="flex flex-col">
                              <span className="text-sm font-bold text-zinc-900">
                                 {t("adults") || "Người lớn"}
                              </span>
                              <span className="text-[11px] text-zinc-400 font-medium">
                                 {t("adultsAge") || "Từ 13 tuổi trở lên"}
                              </span>
                           </div>
                           <div className="flex items-center gap-4">
                              <button
                                 onClick={() => updateGuests("adults", "dec")}
                                 className="h-8 w-8 rounded-full border border-zinc-200 flex items-center justify-center text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4] transition-colors disabled:opacity-30"
                                 disabled={guests.adults <= 1}
                              >
                                 <Minus className="h-3.5 w-3.5" />
                              </button>
                              <span className="text-sm font-bold w-4 text-center">
                                 {guests.adults}
                              </span>
                              <button
                                 onClick={() => updateGuests("adults", "inc")}
                                 className="h-8 w-8 rounded-full border border-zinc-200 flex items-center justify-center text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4] transition-colors"
                              >
                                 <Plus className="h-3.5 w-3.5" />
                              </button>
                           </div>
                        </div>

                        {/* Children */}
                        <div className="flex items-center justify-between">
                           <div className="flex flex-col">
                              <span className="text-sm font-bold text-zinc-900">
                                 {t("children") || "Trẻ em"}
                              </span>
                              <span className="text-[11px] text-zinc-400 font-medium">
                                 {t("childrenAge") || "Từ 0 - 12 tuổi"}
                              </span>
                           </div>
                           <div className="flex items-center gap-4">
                              <button
                                 onClick={() => updateGuests("children", "dec")}
                                 className="h-8 w-8 rounded-full border border-zinc-200 flex items-center justify-center text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4] transition-colors disabled:opacity-30"
                                 disabled={guests.children <= 0}
                              >
                                 <Minus className="h-3.5 w-3.5" />
                              </button>
                              <span className="text-sm font-bold w-4 text-center">
                                 {guests.children}
                              </span>
                              <button
                                 onClick={() => updateGuests("children", "inc")}
                                 className="h-8 w-8 rounded-full border border-zinc-200 flex items-center justify-center text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4] transition-colors"
                              >
                                 <Plus className="h-3.5 w-3.5" />
                              </button>
                           </div>
                        </div>

                        {/* Rooms */}
                        <div className="flex items-center justify-between">
                           <div className="flex flex-col">
                              <span className="text-sm font-bold text-zinc-900">
                                 {t("rooms") || "Phòng"}
                              </span>
                              <span className="text-[11px] text-zinc-400 font-medium">
                                 {t("roomsLabel") || "Số lượng phòng đặt"}
                              </span>
                           </div>
                           <div className="flex items-center gap-4">
                              <button
                                 onClick={() => updateGuests("rooms", "dec")}
                                 className="h-8 w-8 rounded-full border border-zinc-200 flex items-center justify-center text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4] transition-colors disabled:opacity-30"
                                 disabled={guests.rooms <= 1}
                              >
                                 <Minus className="h-3.5 w-3.5" />
                              </button>
                              <span className="text-sm font-bold w-4 text-center">
                                 {guests.rooms}
                              </span>
                              <button
                                 onClick={() => updateGuests("rooms", "inc")}
                                 className="h-8 w-8 rounded-full border border-zinc-200 flex items-center justify-center text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4] transition-colors"
                              >
                                 <Plus className="h-3.5 w-3.5" />
                              </button>
                           </div>
                        </div>

                        <div className="h-px bg-zinc-100 my-2" />

                        {/* Toggles */}
                        <div className="space-y-4">
                           <div className="flex items-center justify-between">
                              <div className="flex items-center gap-3">
                                 <Briefcase className="h-4 w-4 text-zinc-400" />
                                 <span className="text-xs font-bold text-zinc-600">
                                    {t("workTrip") || "Tôi đi công tác"}
                                 </span>
                              </div>
                              <button
                                 onClick={() => setIsWorkTrip(!isWorkTrip)}
                                 className={`w-10 h-5 rounded-full transition-colors relative ${isWorkTrip ? "bg-[#006ce4]" : "bg-zinc-200"}`}
                              >
                                 <div
                                    className={`absolute top-1 w-3 h-3 bg-white rounded-full transition-all ${isWorkTrip ? "left-6" : "left-1"}`}
                                 />
                              </button>
                           </div>
                           <div className="flex items-center justify-between">
                              <div className="flex items-center gap-3">
                                 <Dog className="h-4 w-4 text-zinc-400" />
                                 <span className="text-xs font-bold text-zinc-600">
                                    {t("pets") || "Mang theo thú cưng"}
                                 </span>
                              </div>
                              <button
                                 onClick={() => setIsPets(!isPets)}
                                 className={`w-10 h-5 rounded-full transition-colors relative ${isPets ? "bg-[#006ce4]" : "bg-zinc-200"}`}
                              >
                                 <div
                                    className={`absolute top-1 w-3 h-3 bg-white rounded-full transition-all ${isPets ? "left-6" : "left-1"}`}
                                 />
                              </button>
                           </div>
                        </div>

                        <button
                           onClick={() => setIsGuestOpen(false)}
                           className="w-full bg-[#006ce4]/10 hover:bg-[#006ce4]/20 text-[#006ce4] py-3 rounded-xl font-bold text-sm transition-colors"
                        >
                           {t("done") || "Xong"}
                        </button>
                     </div>
                  </div>
               )}
            </div>

            {/* Search Button */}
            <button
               onClick={handleSearch}
               className="bg-[#006ce4] hover:bg-[#0057b7] text-white rounded-full px-8 py-3 flex items-center justify-center gap-2 shadow-lg shadow-blue-200 transition-all active:scale-[0.95] group ml-1"
            >
               <span className="font-bold text-sm">{t("searchButton") || "Tìm kiếm"}</span>
               <div className="bg-white/20 p-1 rounded-full group-hover:translate-x-1 transition-transform">
                  <Search className="h-3.5 w-3.5" />
               </div>
            </button>
         </div>

         {/* Trust Badges */}
         <div className="mt-4 flex justify-center gap-6 text-[10px] font-bold text-zinc-400 uppercase tracking-widest">
            <div className="flex items-center gap-1.5">
               <div className="h-1 w-1 rounded-full bg-green-500" />
               {t("freeCancellation") || "Hủy phòng miễn phí"}
            </div>
            <div className="flex items-center gap-1.5">
               <div className="h-1 w-1 rounded-full bg-blue-500" />
               {t("totalProperties") || "Hơn 2 triệu chỗ nghỉ"}
            </div>
         </div>
      </div>
   );
}
