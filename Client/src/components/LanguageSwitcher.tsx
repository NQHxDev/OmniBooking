"use client";

import { useLocale, useTranslations } from "next-intl";
import { usePathname, useRouter } from "@/i18n/routing";
import { useState, useRef, useEffect } from "react";
import Image from "next/image";
import { ChevronDown } from "lucide-react";

export default function LanguageSwitcher() {
   const locale = useLocale();
   const t = useTranslations("Common");

   const router = useRouter();
   const pathname = usePathname();
   const [isOpen, setIsOpen] = useState(false);
   const dropdownRef = useRef<HTMLDivElement>(null);

   const languages = [
      {
         code: "vi",
         name: "Tiếng Việt",
         flag: "https://flagcdn.com/w40/vn.png",
      },
      {
         code: "en",
         name: "English",
         flag: "https://flagcdn.com/w40/gb.png",
      },
   ];

   const currentLang = languages.find((l) => l.code === locale) || languages[0];

   useEffect(() => {
      const handleClickOutside = (event: MouseEvent) => {
         if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
            setIsOpen(false);
         }
      };
      document.addEventListener("mousedown", handleClickOutside);
      return () => document.removeEventListener("mousedown", handleClickOutside);
   }, []);

   const handleLanguageChange = (newLocale: string) => {
      setIsOpen(false);
      router.replace(pathname, { locale: newLocale });
   };

   return (
      <div className="relative" ref={dropdownRef}>
         <button
            onClick={() => setIsOpen(!isOpen)}
            className="flex items-center gap-2 rounded-full p-1.5 hover:bg-white/10 transition-all active:scale-95"
         >
            <div className="relative h-5 w-7 overflow-hidden rounded-sm shadow-sm">
               <Image src={currentLang.flag} alt={currentLang.name} fill className="object-cover" />
            </div>
            <ChevronDown
               className={`h-3 w-3 text-white/70 transition-transform ${isOpen ? "rotate-180" : ""}`}
            />
         </button>

         {isOpen && (
            <div className="absolute right-0 mt-2 w-48 origin-top-right rounded-xl bg-white p-1.5 text-zinc-900 shadow-2xl ring-1 ring-black ring-opacity-5 animate-in fade-in zoom-in-95 duration-200 z-60">
               <div className="mb-1.5 px-3 py-1.5 text-[11px] font-bold uppercase tracking-wider text-zinc-400">
                  {t("selectLanguage") || "Chọn ngôn ngữ"}
               </div>
               {languages.map((lang) => (
                  <button
                     key={lang.code}
                     onClick={() => handleLanguageChange(lang.code)}
                     className={`flex w-full items-center gap-3 rounded-lg px-3 py-2 text-[13px] font-medium transition-colors ${
                        locale === lang.code
                           ? "bg-blue-50 text-blue-600"
                           : "hover:bg-zinc-50 text-zinc-700"
                     }`}
                  >
                     <div className="relative h-4 w-6 overflow-hidden rounded-sm border border-zinc-100">
                        <Image src={lang.flag} alt={lang.name} fill className="object-cover" />
                     </div>
                     {lang.name}
                     {locale === lang.code && (
                        <div className="ml-auto h-1.5 w-1.5 rounded-full bg-blue-500" />
                     )}
                  </button>
               ))}
            </div>
         )}
      </div>
   );
}
