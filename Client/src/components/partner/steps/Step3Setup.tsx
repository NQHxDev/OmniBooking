"use client";

import { useFormContext, useFieldArray, useWatch } from "react-hook-form";
import { useTranslations } from "next-intl";
import { Bed, Plus, Trash2, Check, ChevronLeft, ChevronRight } from "lucide-react";
import type { PropertyFormValues } from "../CreatePropertyForm";

const AMENITIES_BY_CATEGORY = {
   GENERAL: [
      "Free Wi-Fi",
      "Swimming Pool",
      "Parking",
      "Airport Shuttle",
      "Gym / Fitness Center",
      "Spa & Wellness Center",
      "24-Hour Front Desk",
      "Elevator",
      "Family Rooms",
      "Pet Friendly",
   ],
   ROOM: [
      "Air Conditioning",
      "Flat-screen TV",
      "Balcony",
      "Minibar",
      "Safe",
      "Work Desk",
      "Ironing Facilities",
   ],
   BATHROOM: ["Private Bathroom", "Hairdryer", "Free Toiletries", "Bathrobe", "Shower", "Bathtub"],
   KITCHEN: ["Refrigerator", "Microwave", "Electric Kettle", "Kitchenware", "Dining Table"],
};

interface Step3SetupProps {
   onBack: () => void;
   onNext: () => void;
}

export default function Step3Setup({ onBack, onNext }: Step3SetupProps) {
   const t = useTranslations("Partner.createPropertyForm");
   const {
      register,
      control,
      setValue,
      getValues,
      formState: { errors },
   } = useFormContext<PropertyFormValues>();

   const { fields, append, remove } = useFieldArray({
      control,
      name: "roomTypes",
   });

   const selectedAmenities = useWatch({ name: "amenities" }) || [];

   const handleAmenityChange = (name: string, checked: boolean) => {
      const current = getValues("amenities") || [];
      if (checked) {
         setValue("amenities", [...current, name], { shouldValidate: true });
      } else {
         setValue(
            "amenities",
            current.filter((x) => x !== name),
            { shouldValidate: true }
         );
      }
   };

   return (
      <div className="space-y-8 animate-in fade-in slide-in-from-right-4 duration-500">
         {/* Rooms Section */}
         <section className="space-y-6">
            <div className="flex items-center justify-between border-b border-zinc-100 pb-4">
               <div className="flex items-center gap-3">
                  <Bed className="h-6 w-6 text-[#003580]" />
                  <div>
                     <h2 className="text-xl font-bold text-zinc-900">{t("roomsTitle")}</h2>
                     <p className="text-xs text-zinc-500">{t("roomsSubtitle")}</p>
                  </div>
               </div>
               <button
                  type="button"
                  onClick={() =>
                     append({
                        name: "",
                        bedType: "Double bed",
                        roomSizeSqm: 25,
                        capacityAdults: 2,
                        capacityChildren: 0,
                        totalRooms: 5,
                        basePrice: 1000000,
                        description: "",
                     })
                  }
                  className="flex items-center gap-2 px-4 py-2 bg-blue-50 text-[#003580] hover:bg-blue-100/70 active:scale-95 transition-all rounded-lg text-sm font-bold border border-blue-100"
               >
                  <Plus className="h-4 w-4" />
                  {t("addRoom")}
               </button>
            </div>

            {fields.length === 0 ? (
               <div className="flex flex-col items-center justify-center py-12 px-4 border-2 border-dashed border-zinc-200 rounded-2xl bg-zinc-50/30 text-center">
                  <Bed className="h-10 w-10 text-zinc-300 mb-3" />
                  <p className="font-bold text-zinc-600 text-sm">No room types added yet</p>
                  <p className="text-xs text-zinc-400 max-w-xs mt-1">
                     You must add at least one room type to proceed and receive bookings.
                  </p>
               </div>
            ) : (
               <div className="space-y-6">
                  {fields.map((field, index) => (
                     <div
                        key={field.id}
                        className="p-6 rounded-2xl border border-zinc-200 bg-white shadow-sm relative space-y-4 animate-in fade-in slide-in-from-bottom-2 duration-300"
                     >
                        <div className="flex items-center justify-between border-b border-zinc-100 pb-3">
                           <span className="text-xs font-bold text-[#003580] uppercase tracking-wider">
                              Room Type #{index + 1}
                           </span>
                           <button
                              type="button"
                              onClick={() => remove(index)}
                              className="p-2 hover:bg-red-50 text-zinc-400 hover:text-red-600 rounded-lg transition-colors"
                              title="Remove room type"
                           >
                              <Trash2 className="h-4 w-4" />
                           </button>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                           <div>
                              <label className="block text-[11px] font-bold text-zinc-600 uppercase mb-1">
                                 {t("roomName")}
                              </label>
                              <input
                                 {...register(`roomTypes.${index}.name` as const)}
                                 placeholder={t("roomNamePlaceholder")}
                                 className="w-full px-3 py-2 rounded-lg border border-zinc-200 focus:border-[#003580] focus:ring-2 focus:ring-blue-50 outline-none text-sm transition-all text-zinc-800"
                              />
                              {errors.roomTypes?.[index]?.name && (
                                 <p className="text-xs text-red-500 mt-1">
                                    {errors.roomTypes[index]?.name?.message}
                                 </p>
                              )}
                           </div>

                           <div>
                              <label className="block text-[11px] font-bold text-zinc-600 uppercase mb-1">
                                 {t("bedType")}
                              </label>
                              <select
                                 {...register(`roomTypes.${index}.bedType` as const)}
                                 className="w-full px-3 py-2 rounded-lg border border-zinc-200 focus:border-[#003580] focus:ring-2 focus:ring-blue-50 outline-none text-sm transition-all bg-white text-zinc-800"
                              >
                                 <option value="Single bed">Single bed</option>
                                 <option value="Double bed">Double bed</option>
                                 <option value="Queen bed">Queen bed</option>
                                 <option value="King bed">King bed</option>
                                 <option value="Twin beds">Twin beds</option>
                                 <option value="Sofa bed">Sofa bed</option>
                              </select>
                           </div>
                        </div>

                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                           <div>
                              <label className="block text-[11px] font-bold text-zinc-600 uppercase mb-1">
                                 {t("roomSize")}
                              </label>
                              <input
                                 type="number"
                                 {...register(`roomTypes.${index}.roomSizeSqm` as const, {
                                    valueAsNumber: true,
                                 })}
                                 className="w-full px-3 py-2 rounded-lg border border-zinc-200 focus:border-[#003580] focus:ring-2 focus:ring-blue-50 outline-none text-sm transition-all text-zinc-800"
                              />
                              {errors.roomTypes?.[index]?.roomSizeSqm && (
                                 <p className="text-xs text-red-500 mt-1">
                                    {errors.roomTypes[index]?.roomSizeSqm?.message}
                                 </p>
                              )}
                           </div>

                           <div>
                              <label className="block text-[11px] font-bold text-zinc-600 uppercase mb-1">
                                 {t("capacityAdults")}
                              </label>
                              <input
                                 type="number"
                                 {...register(`roomTypes.${index}.capacityAdults` as const, {
                                    valueAsNumber: true,
                                 })}
                                 className="w-full px-3 py-2 rounded-lg border border-zinc-200 focus:border-[#003580] focus:ring-2 focus:ring-blue-50 outline-none text-sm transition-all text-zinc-800"
                              />
                              {errors.roomTypes?.[index]?.capacityAdults && (
                                 <p className="text-xs text-red-500 mt-1">
                                    {errors.roomTypes[index]?.capacityAdults?.message}
                                 </p>
                              )}
                           </div>

                           <div>
                              <label className="block text-[11px] font-bold text-zinc-600 uppercase mb-1">
                                 {t("capacityChildren")}
                              </label>
                              <input
                                 type="number"
                                 {...register(`roomTypes.${index}.capacityChildren` as const, {
                                    valueAsNumber: true,
                                 })}
                                 className="w-full px-3 py-2 rounded-lg border border-zinc-200 focus:border-[#003580] focus:ring-2 focus:ring-blue-50 outline-none text-sm transition-all text-zinc-800"
                              />
                           </div>

                           <div>
                              <label className="block text-[11px] font-bold text-zinc-600 uppercase mb-1">
                                 {t("totalRooms")}
                              </label>
                              <input
                                 type="number"
                                 {...register(`roomTypes.${index}.totalRooms` as const, {
                                    valueAsNumber: true,
                                 })}
                                 className="w-full px-3 py-2 rounded-lg border border-zinc-200 focus:border-[#003580] focus:ring-2 focus:ring-blue-50 outline-none text-sm transition-all text-zinc-800"
                              />
                              {errors.roomTypes?.[index]?.totalRooms && (
                                 <p className="text-xs text-red-500 mt-1">
                                    {errors.roomTypes[index]?.totalRooms?.message}
                                 </p>
                              )}
                           </div>
                        </div>

                        <div>
                           <label className="block text-[11px] font-bold text-zinc-600 uppercase mb-1">
                              {t("basePrice")}
                           </label>
                           <div className="relative">
                              <span className="absolute left-3 top-2 text-zinc-400 text-sm font-semibold">
                                 VND
                              </span>
                              <input
                                 type="number"
                                 {...register(`roomTypes.${index}.basePrice` as const, {
                                    valueAsNumber: true,
                                 })}
                                 className="w-full pl-16 pr-3 py-2 rounded-lg border border-zinc-200 focus:border-[#003580] focus:ring-2 focus:ring-blue-50 outline-none text-sm transition-all text-zinc-800"
                              />
                           </div>
                           {errors.roomTypes?.[index]?.basePrice && (
                              <p className="text-xs text-red-500 mt-1">
                                 {errors.roomTypes[index]?.basePrice?.message}
                              </p>
                           )}
                        </div>
                     </div>
                  ))}
               </div>
            )}
         </section>

         {/* Amenities Section */}
         <section className="space-y-6 border-t border-zinc-100 pt-8">
            <div className="flex items-center gap-3">
               <Plus className="h-6 w-6 text-[#003580]" />
               <div>
                  <h2 className="text-xl font-bold text-zinc-900">{t("amenitiesTitle")}</h2>
                  <p className="text-xs text-zinc-500">{t("amenitiesSubtitle")}</p>
               </div>
            </div>

            <div className="space-y-6 animate-in fade-in duration-500">
               {Object.entries(AMENITIES_BY_CATEGORY).map(([category, list]) => (
                  <div key={category} className="space-y-3">
                     <h3 className="text-xs font-bold text-zinc-700 uppercase tracking-widest bg-zinc-50 py-1.5 px-3 rounded-md inline-block">
                        {t(`amenitiesCategories.${category}`)}
                     </h3>
                     <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                        {list.map((name) => {
                           const isChecked = (selectedAmenities || []).includes(name);
                           return (
                              <button
                                 key={name}
                                 type="button"
                                 onClick={() => handleAmenityChange(name, !isChecked)}
                                 className={`flex items-center gap-3 p-3 rounded-xl border text-left text-sm transition-all ${
                                    isChecked
                                       ? "border-[#003580] bg-blue-50/30 text-[#003580] font-semibold"
                                       : "border-zinc-200 hover:border-zinc-300 text-zinc-600 bg-white"
                                 }`}
                              >
                                 <div
                                    className={`h-4.5 w-4.5 rounded border flex items-center justify-center shrink-0 transition-all ${
                                       isChecked
                                          ? "bg-[#003580] border-[#003580] text-white"
                                          : "border-zinc-300 bg-white"
                                    }`}
                                 >
                                    {isChecked && <Check className="h-3 w-3 stroke-3" />}
                                 </div>
                                 <span className="truncate">{name}</span>
                              </button>
                           );
                        })}
                     </div>
                  </div>
               ))}
            </div>
         </section>

         <div className="flex justify-between pt-8 border-t border-zinc-100">
            <button
               type="button"
               onClick={onBack}
               className="flex items-center gap-2 px-8 py-3.5 bg-zinc-100 text-zinc-700 rounded-xl font-bold hover:bg-zinc-200 transition-all active:scale-95"
            >
               <ChevronLeft className="h-5 w-5" /> {t("back")}
            </button>
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
