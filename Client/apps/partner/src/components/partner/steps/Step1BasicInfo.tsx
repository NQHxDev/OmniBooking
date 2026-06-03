"use client";

import { useFormContext, useWatch } from "react-hook-form";
import { useTranslations } from "next-intl";
import { Clock, Building2, Hotel, Home, Palmtree, Building, ChevronRight } from "lucide-react";
import type { PropertyFormValues } from "../CreatePropertyForm";

const PROPERTY_TYPES = [
   {
      id: "HOTEL",
      icon: Hotel,
   },
   {
      id: "APARTMENT",
      icon: Building,
   },
   {
      id: "VILLA",
      icon: Home,
   },
   {
      id: "HOMESTAY",
      icon: Palmtree,
   },
   {
      id: "RESORT",
      icon: Building2,
   },
   {
      id: "GUESTHOUSE",
      icon: Building,
   },
] as const;

interface Step1BasicInfoProps {
   isFlexibleTime: boolean;
   setIsFlexibleTime: (val: boolean) => void;
   onNext: () => void;
}

export default function Step1BasicInfo({
   isFlexibleTime,
   setIsFlexibleTime,
   onNext,
}: Step1BasicInfoProps) {
   const t = useTranslations("Partner.createPropertyForm");
   const {
      register,
      setValue,
      formState: { errors },
   } = useFormContext<PropertyFormValues>();

   const selectedType = useWatch({ name: "propertyType" });

   return (
      <div className="space-y-8 animate-in fade-in slide-in-from-right-4 duration-500">
         <section className="space-y-6">
            <div className="flex items-center gap-3 mb-2">
               <Building2 className="h-5 w-5 text-[#003580]" />
               <h2 className="text-xl font-bold text-zinc-900">{t("basicInfo")}</h2>
            </div>

            <div className="space-y-4">
               <div>
                  <label className="block text-[13px] font-bold text-zinc-700 mb-2 uppercase tracking-tight">
                     {t("propertyName")}
                  </label>
                  <input
                     {...register("name")}
                     placeholder={t("placeholders.name")}
                     className="w-full px-4 py-3 rounded-xl border border-zinc-200 focus:border-[#003580] focus:ring-4 focus:ring-blue-50 outline-none transition-all placeholder:text-zinc-300 text-zinc-800"
                  />
                  {errors.name && (
                     <p className="text-xs text-red-500 mt-1">{errors.name.message}</p>
                  )}
               </div>
            </div>
         </section>

         <section className="space-y-6">
            <label className="block text-[13px] font-bold text-zinc-700 uppercase tracking-tight">
               {t("propertyType")}
            </label>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
               {PROPERTY_TYPES.map((type) => (
                  <button
                     key={type.id}
                     type="button"
                     onClick={() => setValue("propertyType", type.id)}
                     className={`flex items-start gap-4 p-5 rounded-2xl border-2 text-left transition-all duration-300 ${
                        selectedType === type.id
                           ? "border-[#003580] bg-blue-50/30 ring-4 ring-blue-50"
                           : "border-zinc-100 hover:border-zinc-200 bg-white"
                     }`}
                  >
                     <div
                        className={`p-3 rounded-xl shrink-0 ${
                           selectedType === type.id
                              ? "bg-[#003580] text-white"
                              : "bg-zinc-50 text-zinc-400"
                        }`}
                     >
                        <type.icon className="h-6 w-6" />
                     </div>
                     <div>
                        <p className="font-bold text-zinc-900">
                           {t(`propertyTypes.${type.id}.label`)}
                        </p>
                        <p className="text-xs text-zinc-500 mt-1 leading-relaxed">
                           {t(`propertyTypes.${type.id}.description`)}
                        </p>
                     </div>
                  </button>
               ))}
            </div>
         </section>

         <section className="space-y-6 border-t border-zinc-100 pt-8">
            <div className="flex items-center justify-between">
               <div className="flex items-center gap-3">
                  <Clock className="h-5 w-5 text-[#003580]" />
                  <h2 className="text-xl font-bold text-zinc-900">{t("timeTitle")}</h2>
               </div>
               <label className="flex items-center gap-3 cursor-pointer group">
                  <span className="text-[13px] font-bold text-zinc-500 group-hover:text-[#003580] transition-colors">
                     {t("flexibleTime")}
                  </span>
                  <div className="relative">
                     <input
                        type="checkbox"
                        className="sr-only"
                        checked={isFlexibleTime}
                        onChange={(e) => {
                           const checked = e.target.checked;
                           setIsFlexibleTime(checked);
                           if (checked) {
                              setValue("checkInTime", "00:00");
                              setValue("checkOutTime", "00:00");
                           } else {
                              setValue("checkInTime", "14:00");
                              setValue("checkOutTime", "12:00");
                           }
                        }}
                     />
                     <div
                        className={`block w-12 h-6 rounded-full transition-all duration-300 ${isFlexibleTime ? "bg-[#003580]" : "bg-zinc-200"}`}
                     ></div>
                     <div
                        className={`absolute left-1 top-1 bg-white w-4 h-4 rounded-full transition-transform duration-300 ${isFlexibleTime ? "translate-x-6" : ""}`}
                     ></div>
                  </div>
               </label>
            </div>

            <div
               className={`grid grid-cols-2 gap-6 p-6 rounded-2xl border-2 transition-all duration-500 ${isFlexibleTime ? "bg-zinc-50/50 border-zinc-100 opacity-60" : "bg-white border-zinc-100"}`}
            >
               <div>
                  <label
                     className={`block text-[11px] font-bold mb-2 uppercase tracking-widest ${isFlexibleTime ? "text-zinc-400" : "text-zinc-500"}`}
                  >
                     {t("checkIn")}
                  </label>
                  <div className="relative">
                     <input
                        type="time"
                        disabled={isFlexibleTime}
                        {...register("checkInTime")}
                        className={`w-full px-4 py-3 rounded-xl border border-zinc-200 outline-none transition-all ${isFlexibleTime ? "bg-transparent cursor-not-allowed text-zinc-400" : "focus:border-[#003580] bg-white text-zinc-800"}`}
                     />
                     {isFlexibleTime && (
                        <div className="absolute inset-0 flex items-center justify-center bg-zinc-50/10 backdrop-blur-[1px] rounded-xl text-[10px] font-bold text-zinc-400 uppercase">
                           {t("placeholders.anytime")}
                        </div>
                     )}
                  </div>
               </div>
               <div>
                  <label
                     className={`block text-[11px] font-bold mb-2 uppercase tracking-widest ${isFlexibleTime ? "text-zinc-400" : "text-zinc-500"}`}
                  >
                     {t("checkOut")}
                  </label>
                  <div className="relative">
                     <input
                        type="time"
                        disabled={isFlexibleTime}
                        {...register("checkOutTime")}
                        className={`w-full px-4 py-3 rounded-xl border border-zinc-200 outline-none transition-all ${isFlexibleTime ? "bg-transparent cursor-not-allowed text-zinc-400" : "focus:border-[#003580] bg-white text-zinc-800"}`}
                     />
                     {isFlexibleTime && (
                        <div className="absolute inset-0 flex items-center justify-center bg-zinc-50/10 backdrop-blur-[1px] rounded-xl text-[10px] font-bold text-zinc-400 uppercase">
                           {t("placeholders.anytime")}
                        </div>
                     )}
                  </div>
               </div>
            </div>

            {isFlexibleTime && (
               <div className="flex items-center gap-3 p-4 rounded-xl bg-blue-50 border border-blue-100 animate-in fade-in zoom-in-95 duration-300">
                  <div className="h-2 w-2 rounded-full bg-blue-500 animate-pulse"></div>
                  <p className="text-[12px] font-medium text-[#003580]">
                     {t("messages.flexibleInfo")}
                  </p>
               </div>
            )}
         </section>

         <div className="flex justify-end pt-4 border-t border-zinc-100">
            <button
               type="button"
               onClick={onNext}
               className="flex items-center gap-2 px-8 py-3.5 bg-[#003580] text-white rounded-xl font-bold hover:bg-[#002b66] transition-all active:scale-95 shadow-lg shadow-blue-200"
            >
               {t("next")} <ChevronRight className="h-5 w-5" />
            </button>
         </div>
      </div>
   );
}
