"use client";

import { useState, useRef, useEffect, useMemo } from "react";
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
   TrendingUp,
   History,
   Loader2,
   Hotel,
   X,
} from "lucide-react";
import { DateRange } from "react-day-picker";
import { format } from "date-fns";
import { vi, enUS } from "date-fns/locale";
import DateRangePicker from "./DateRangePicker";
import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { destinationService, DestinationSuggestionResponse } from "@/services/destinationService";
import { useDebounce } from "@/hooks/useDebounce";
import { motion, AnimatePresence } from "framer-motion";

const RECENT_SEARCHES_KEY = "omni_recent_searches";

const translateDestinationName = (name: string, id: string, locale: string): string => {
   if (locale === "en") {
      switch (id) {
         case "1":
            return "Hanoi";
         case "2":
            return "Ho Chi Minh City";
         case "3":
            return "Da Nang";
         case "4":
            return "Hoi An";
         case "5":
            return "Phu Quoc";
         case "11":
            return "Quang Ninh";
         case "12":
            return "Ha Long";
         case "13":
            return "Nha Trang";
         case "14":
            return "Da Lat";
         case "15":
            return "Vung Tau";
         case "16":
            return "Sapa";
         case "17":
            return "Hue";
         case "18":
            return "Hai Phong";
         case "19":
            return "Can Tho";
      }
      switch (name) {
         case "Hà Nội":
            return "Hanoi";
         case "Hồ Chí Minh":
            return "Ho Chi Minh City";
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
      }
   } else {
      switch (id) {
         case "1":
            return "Hà Nội";
         case "2":
            return "Hồ Chí Minh";
         case "3":
            return "Đà Nẵng";
         case "4":
            return "Hội An";
         case "5":
            return "Phú Quốc";
         case "11":
            return "Quảng Ninh";
         case "12":
            return "Hạ Long";
         case "13":
            return "Nha Trang";
         case "14":
            return "Đà Lạt";
         case "15":
            return "Vũng Tàu";
         case "16":
            return "Sapa";
         case "17":
            return "Huế";
         case "18":
            return "Hải Phòng";
         case "19":
            return "Cần Thơ";
      }
      switch (name) {
         case "Hanoi":
            return "Hà Nội";
         case "Ho Chi Minh City":
         case "Ho Chi Minh":
            return "Hồ Chí Minh";
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
      }
   }
   return name;
};

const translateCountryName = (country: string, locale: string): string => {
   if (locale === "en") {
      switch (country) {
         case "Việt Nam":
         case "Vietnam":
            return "Vietnam";
         case "Pháp":
         case "France":
            return "France";
         case "Vương quốc Anh":
         case "United Kingdom":
            return "United Kingdom";
         case "Nhật Bản":
         case "Japan":
            return "Japan";
         case "Hoa Kỳ":
         case "United States":
            return "United States";
         case "Thái Lan":
         case "Thailand":
            return "Thailand";
      }
   } else {
      switch (country) {
         case "Vietnam":
         case "Việt Nam":
            return "Việt Nam";
         case "France":
         case "Pháp":
            return "Pháp";
         case "United Kingdom":
         case "Vương quốc Anh":
            return "Vương quốc Anh";
         case "Japan":
         case "Nhật Bản":
            return "Nhật Bản";
         case "United States":
         case "Hoa Kỳ":
            return "Hoa Kỳ";
         case "Thailand":
         case "Thái Lan":
            return "Thái Lan";
      }
   }
   return country;
};

export default function SearchBar() {
   const t = useTranslations("Common");
   const locale = useLocale();
   const router = useRouter();
   const dateLocale = locale === "vi" ? vi : enUS;

   // Search/Location State
   const [destination, setDestination] = useState("");
   const [isLocationOpen, setIsLocationOpen] = useState(false);
   const [suggestions, setSuggestions] = useState<DestinationSuggestionResponse[]>([]);
   const [trending, setTrending] = useState<DestinationSuggestionResponse[]>([]);
   const [recentSearches, setRecentSearches] = useState<DestinationSuggestionResponse[]>(() => {
      if (typeof window !== "undefined") {
         const saved = localStorage.getItem(RECENT_SEARCHES_KEY);
         if (saved) {
            try {
               return JSON.parse(saved).slice(0, 2);
            } catch (e) {
               console.error("Failed to parse recent searches", e);
            }
         }
      }
      return [];
   });
   const [isLoading, setIsLoading] = useState(false);
   const [activeIndex, setActiveIndex] = useState(-1);
   const locationRef = useRef<HTMLDivElement>(null);
   const inputRef = useRef<HTMLInputElement>(null);

   const debouncedSearch = useDebounce(destination, 300);

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

   // Load recent searches & trending on mount/focus/locale change
   useEffect(() => {
      const fetchTrending = async () => {
         try {
            const data = await destinationService.getTrending(locale);
            setTrending(data);
         } catch (e) {
            console.error("Failed to fetch trending", e);
         }
      };

      fetchTrending();
   }, [locale]);

   // Handle debounced search
   useEffect(() => {
      const fetchSuggestions = async () => {
         setActiveIndex(-1);
         if (debouncedSearch.trim().length <= 1) {
            setSuggestions([]);
            return;
         }

         setIsLoading(true);
         try {
            const data = await destinationService.search(debouncedSearch, locale);
            setSuggestions(data);
         } catch (e) {
            console.error("Search failed", e);
            setSuggestions([]);
         } finally {
            setIsLoading(false);
         }
      };

      fetchSuggestions();
   }, [debouncedSearch, locale]);

   const combinedResults = useMemo(() => {
      if (destination.trim().length > 1) return suggestions.slice(0, 5);
      return [...recentSearches, ...trending];
   }, [destination, suggestions, recentSearches, trending]);

   const handleSelect = (item: DestinationSuggestionResponse) => {
      setDestination(translateDestinationName(item.name, item.id, locale));
      setIsLocationOpen(false);

      // Save to recent searches
      const newRecent = [item, ...recentSearches.filter((r) => r.id !== item.id)].slice(0, 2);
      setRecentSearches(newRecent);
      localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(newRecent));
   };

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

   const handleKeyDown = (e: React.KeyboardEvent) => {
      if (!isLocationOpen) return;

      if (e.key === "ArrowDown") {
         e.preventDefault();
         setActiveIndex((prev) => (prev < combinedResults.length - 1 ? prev + 1 : prev));
      } else if (e.key === "ArrowUp") {
         e.preventDefault();
         setActiveIndex((prev) => (prev > 0 ? prev - 1 : prev));
      } else if (e.key === "Enter" && activeIndex >= 0) {
         e.preventDefault();
         handleSelect(combinedResults[activeIndex]);
      } else if (e.key === "Escape") {
         setIsLocationOpen(false);
      }
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
                     setIsLocationOpen(true);
                     setIsDateOpen(false);
                     setIsGuestOpen(false);
                     setTimeout(() => inputRef.current?.focus(), 10);
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
                     <div className="flex items-center justify-between gap-2">
                        <input
                           ref={inputRef}
                           type="text"
                           value={destination}
                           onChange={(e) => {
                              setDestination(e.target.value);
                              if (!isLocationOpen) setIsLocationOpen(true);
                           }}
                           onKeyDown={handleKeyDown}
                           placeholder={t("searchPlaceholder") || "Bạn muốn đến đâu?"}
                           className="bg-transparent border-none outline-none text-sm font-bold text-black placeholder:text-zinc-400 w-full"
                        />
                        {destination && (
                           <X
                              className="h-3 w-3 text-zinc-300 hover:text-zinc-600 cursor-pointer transition-colors"
                              onClick={(e) => {
                                 e.stopPropagation();
                                 setDestination("");
                              }}
                           />
                        )}
                        <ChevronDown
                           className={`h-4 w-4 text-zinc-400 transition-transform duration-300 shrink-0 ${isLocationOpen ? "rotate-180" : ""}`}
                        />
                     </div>
                  </div>
               </div>

               {/* Location Dropdown */}
               <AnimatePresence>
                  {isLocationOpen && (
                     <motion.div
                        initial={{ opacity: 0, y: 10, scale: 0.95 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: 10, scale: 0.95 }}
                        transition={{ duration: 0.2, ease: "easeOut" }}
                        className="absolute top-full left-0 mt-3 w-full md:w-[320px] bg-white rounded-3xl shadow-2xl border border-zinc-100 overflow-hidden py-2 z-50 max-h-[450px] overflow-y-auto custom-scrollbar"
                     >
                        {/* Status Loading */}
                        {isLoading && (
                           <div className="flex items-center justify-center py-8">
                              <Loader2 className="h-6 w-6 text-[#006ce4] animate-spin" />
                           </div>
                        )}

                        {!isLoading && (
                           <div className="space-y-2">
                              {destination.trim().length <= 1 ? (
                                 <>
                                    {/* Recent Searches */}
                                    {recentSearches.length > 0 && (
                                       <div className="px-2">
                                          <div className="px-4 py-2 flex items-center gap-2">
                                             <History className="h-3.5 w-3.5 text-zinc-400" />
                                             <span className="text-[10px] uppercase font-bold text-zinc-400 tracking-wider">
                                                {t("recentSearches") || "Tìm kiếm gần đây"}
                                             </span>
                                          </div>
                                          {recentSearches.map((item, idx) => (
                                             <SearchItem
                                                key={`recent-${item.id}`}
                                                item={item}
                                                isActive={activeIndex === idx}
                                                onClick={() => handleSelect(item)}
                                             />
                                          ))}
                                       </div>
                                    )}

                                    {/* Trending */}
                                    <div className="px-2">
                                       <div className="px-4 py-2 flex items-center gap-2">
                                          <TrendingUp className="h-3.5 w-3.5 text-[#008009]" />
                                          <span className="text-[10px] uppercase font-bold text-zinc-400 tracking-wider">
                                             {t("trendingNow") || "Xu hướng hàng đầu"}
                                          </span>
                                       </div>
                                       {trending.map((item, idx) => (
                                          <SearchItem
                                             key={`trending-${item.id}`}
                                             item={item}
                                             isActive={activeIndex === idx + recentSearches.length}
                                             onClick={() => handleSelect(item)}
                                          />
                                       ))}
                                    </div>
                                 </>
                              ) : (
                                 /* Suggestions */
                                 <div className="px-2">
                                    {suggestions.length > 0 ? (
                                       suggestions
                                          .slice(0, 5)
                                          .map((item, idx) => (
                                             <SearchItem
                                                key={`suggest-${item.id}`}
                                                item={item}
                                                isActive={activeIndex === idx}
                                                onClick={() => handleSelect(item)}
                                             />
                                          ))
                                    ) : (
                                       <div className="px-6 py-8 text-center">
                                          <p className="text-sm text-zinc-400 font-medium">
                                             {t("noResults") || "Không tìm thấy địa điểm phù hợp"}
                                          </p>
                                       </div>
                                    )}
                                 </div>
                              )}
                           </div>
                        )}
                     </motion.div>
                  )}
               </AnimatePresence>
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
                        {t("Search.travelers") || "Số khách"}
                     </span>
                     <span className="text-sm font-bold text-black whitespace-nowrap">
                        {guests.adults} {t("Search.adults") || "người lớn"}
                        {guests.children > 0
                           ? ` · ${guests.children} ${t("Search.children") || "trẻ em"}`
                           : ""}{" "}
                        · {guests.rooms} {t("Search.rooms") || "phòng"}
                     </span>
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
                                 {t("Search.adults") || "Người lớn"}
                              </span>
                              <span className="text-[11px] text-zinc-400 font-medium">
                                 {t("Search.adultsAge") || "Từ 13 tuổi trở lên"}
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
                                 {t("Search.children") || "Trẻ em"}
                              </span>
                              <span className="text-[11px] text-zinc-400 font-medium">
                                 {t("Search.childrenAge") || "Từ 0 - 12 tuổi"}
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
                                 {t("Search.rooms") || "Phòng"}
                              </span>
                              <span className="text-[11px] text-zinc-400 font-medium">
                                 {t("Search.roomsLabel") || "Số lượng phòng đặt"}
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
               className="bg-[#006ce4] hover:bg-[#0057b7] text-white rounded-full px-8 py-3 flex items-center justify-center gap-2 shadow-lg shadow-blue-200 transition-all active:scale-[0.95] group ml-1 cursor-pointer"
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

function SearchItem({
   item,
   onClick,
   isActive,
}: {
   item: DestinationSuggestionResponse;
   onClick: () => void;
   isActive: boolean;
}) {
   const t = useTranslations("Common");
   const locale = useLocale();

   const getTypeLabel = (type: string) => {
      switch (type) {
         case "CITY":
            return t("cityType");
         case "HOTEL":
            return t("hotelType");
         case "LANDMARK":
            return t("landmarkType");
         case "REGION":
            return t("regionType");
         default:
            return type;
      }
   };

   const displayName = translateDestinationName(item.name, item.id, locale);
   const displayCountry = translateCountryName(item.country, locale);

   return (
      <div
         onClick={onClick}
         className={`flex items-center gap-3 px-3 py-2 cursor-pointer transition-all rounded-xl mx-2 ${isActive ? "bg-blue-50 text-[#006ce4]" : "hover:bg-zinc-50"}`}
      >
         <div
            className={`h-8 w-8 rounded-lg flex items-center justify-center shrink-0 ${item.type === "HOTEL" ? "bg-orange-50 text-orange-500" : "bg-blue-50 text-[#006ce4]"}`}
         >
            {item.type === "HOTEL" ? (
               <Hotel className="h-4.5 w-4.5" />
            ) : (
               <MapPin className="h-4.5 w-4.5" />
            )}
         </div>
         <div className="flex flex-col min-w-0">
            <span
               className={`text-sm font-bold truncate ${isActive ? "text-[#006ce4]" : "text-zinc-900"}`}
            >
               {displayName}
            </span>
            <span className="text-[10px] font-medium text-zinc-400 truncate">
               {getTypeLabel(item.type)} · {displayCountry}
            </span>
         </div>
      </div>
   );
}
