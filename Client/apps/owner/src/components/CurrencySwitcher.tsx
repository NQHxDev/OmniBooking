"use client";

import { useSettingStore } from "@/store/useSettingStore";
import { useState, useRef, useEffect } from "react";
import { ChevronDown } from "lucide-react";
import { useTranslations } from "next-intl";

interface CurrencySwitcherProps {
   theme?: "blue-bg" | "white-bg";
}

export default function CurrencySwitcher({ theme = "blue-bg" }: CurrencySwitcherProps) {
   const { currency, setCurrency } = useSettingStore();
   const [isOpen, setIsOpen] = useState(false);
   const t = useTranslations("Common.Currency");
   const dropdownRef = useRef<HTMLDivElement>(null);

   const currencies = [
      { code: "USD", name: t("USD"), symbol: "$" },
      { code: "VND", name: t("VND"), symbol: "₫" },
   ];

   const currentCurrency = currencies.find((c) => c.code === currency) || currencies[0];

   useEffect(() => {
      const handleClickOutside = (event: MouseEvent) => {
         if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
            setIsOpen(false);
         }
      };
      document.addEventListener("mousedown", handleClickOutside);
      return () => document.removeEventListener("mousedown", handleClickOutside);
   }, []);

   const handleCurrencyChange = (newCurrency: string) => {
      setCurrency(newCurrency);
      setIsOpen(false);
   };

   const isBlueBg = theme === "blue-bg";

   return (
      <div className="relative" ref={dropdownRef}>
         <button
            onClick={() => setIsOpen(!isOpen)}
            className={`flex items-center gap-1.5 rounded-full px-2.5 py-1.5 transition-all active:scale-95 font-medium text-sm ${
               isBlueBg
                  ? "text-white hover:bg-white/10"
                  : "text-zinc-700 hover:bg-zinc-50 border border-zinc-100"
            }`}
         >
            <span className="text-sm font-bold">{currentCurrency.code}</span>
            <ChevronDown
               className={`h-3 w-3 transition-transform ${
                  isBlueBg ? "text-white/70" : "text-zinc-500"
               } ${isOpen ? "rotate-180" : ""}`}
            />
         </button>

         {isOpen && (
            <div className="absolute right-0 mt-2 w-48 origin-top-right rounded-xl bg-white p-1.5 text-zinc-900 shadow-2xl ring-1 ring-black ring-opacity-5 animate-in fade-in zoom-in-95 duration-200 z-60">
               <div className="mb-1.5 px-3 py-1.5 text-[11px] font-bold uppercase tracking-wider text-zinc-400">
                  Chọn loại tiền tệ
               </div>
               {currencies.map((c) => (
                  <button
                     key={c.code}
                     onClick={() => handleCurrencyChange(c.code)}
                     className={`flex w-full items-center gap-3 rounded-lg px-3 py-2 text-[13px] font-medium transition-colors ${
                        currency === c.code
                           ? "bg-blue-50 text-blue-600"
                           : "hover:bg-zinc-50 text-zinc-700"
                     }`}
                  >
                     <div className="flex h-6 w-6 items-center justify-center rounded-full bg-zinc-100 text-[11px] font-bold">
                        {c.symbol}
                     </div>
                     <div className="flex flex-col items-start">
                        <span className="leading-none">{c.code}</span>
                        <span className="text-[10px] text-zinc-400">{c.name}</span>
                     </div>
                     {currency === c.code && (
                        <div className="ml-auto h-1.5 w-1.5 rounded-full bg-blue-500" />
                     )}
                  </button>
               ))}
            </div>
         )}
      </div>
   );
}
