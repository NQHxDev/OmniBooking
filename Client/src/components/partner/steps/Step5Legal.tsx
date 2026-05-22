"use client";

import { useState, useEffect } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslations } from "next-intl";
import { FileText, CheckCircle2, ChevronLeft, Loader2, History } from "lucide-react";
import type { PropertyFormValues } from "../CreatePropertyForm";
import { propertyService } from "@/lib/api/propertyService";
import type { PartnerLegalProfileResponse } from "@/lib/api/propertyService";
import Image from "next/image";

interface Step5LegalProps {
   images: { file: File; preview: string; isUploading: boolean }[];
   isSubmitting: boolean;
   onBack: () => void;
}

export default function Step5Legal({ images, isSubmitting, onBack }: Step5LegalProps) {
   const t = useTranslations("Partner.createPropertyForm");
   const {
      register,
      setValue,
      trigger,
      formState: { errors },
   } = useFormContext<PropertyFormValues>();

   const [profiles, setProfiles] = useState<PartnerLegalProfileResponse[]>([]);
   const [isLoadingProfiles, setIsLoadingProfiles] = useState(true);

   useEffect(() => {
      let isMounted = true;
      propertyService
         .getLegalProfiles()
         .then((data) => {
            if (isMounted) {
               setProfiles(data);
               setIsLoadingProfiles(false);
            }
         })
         .catch((err) => {
            console.error("Failed to fetch legal profiles", err);
            if (isMounted) {
               setIsLoadingProfiles(false);
            }
         });
      return () => {
         isMounted = false;
      };
   }, []);

   const handleSelectProfile = (profile: PartnerLegalProfileResponse) => {
      setValue("businessRegistrationNumber", profile.businessRegistrationNumber);
      setValue("taxCode", profile.taxCode);
      setValue("legalOwnerName", profile.legalOwnerName);
      trigger(["businessRegistrationNumber", "taxCode", "legalOwnerName"]);
   };

   const maskValue = (value: string) => {
      if (!value) return "";
      const trimmed = value.trim();
      if (trimmed.length <= 7) return trimmed;
      return `${trimmed.slice(0, 3)}...${trimmed.slice(-4)}`;
   };

   const watchAllFields = useWatch<PropertyFormValues>();

   const roomTypes = watchAllFields.roomTypes || [];
   const amenities = watchAllFields.amenities || [];

   return (
      <div className="space-y-8 animate-in fade-in slide-in-from-right-4 duration-500">
         {/* Legal Fields */}
         <section className="space-y-6">
            <div className="flex items-center gap-3">
               <FileText className="h-6 w-6 text-[#003580]" />
               <div>
                  <h2 className="text-xl font-bold text-zinc-900">{t("legalTitle")}</h2>
                  <p className="text-xs text-zinc-500">{t("legalSubtitle")}</p>
               </div>
            </div>

            {/* Previous Legal Profiles */}
            {!isLoadingProfiles && profiles.length > 0 && (
               <div className="space-y-3 p-4 rounded-2xl bg-zinc-50 border border-zinc-100">
                  <div className="flex items-center gap-2 text-zinc-800">
                     <History className="h-4 w-4 text-[#003580]" />
                     <span className="text-[13px] font-bold uppercase tracking-tight text-zinc-700">
                        {t("previousLegalProfiles")}
                     </span>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                     {profiles.map((profile) => (
                        <button
                           key={profile.id}
                           type="button"
                           onClick={() => handleSelectProfile(profile)}
                           className="flex flex-col text-left p-4 rounded-xl border border-zinc-200 bg-white hover:border-[#003580] hover:shadow-md hover:shadow-blue-50/50 transition-all duration-300 group cursor-pointer"
                        >
                           <span className="text-[13px] font-bold text-zinc-800 mb-1 group-hover:text-[#003580] transition-colors">
                              {profile.legalOwnerName}
                           </span>
                           <div className="space-y-0.5 text-xs text-zinc-500">
                              <p>
                                 <span className="font-semibold text-zinc-700">
                                    {t("businessRegistrationNumber")}:
                                 </span>{" "}
                                 {maskValue(profile.businessRegistrationNumber)}
                              </p>
                              <p>
                                 <span className="font-semibold text-zinc-700">
                                    {t("taxCode")}:
                                 </span>{" "}
                                 {maskValue(profile.taxCode)}
                              </p>
                           </div>
                        </button>
                     ))}
                  </div>
               </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
               <div>
                  <label className="block text-[13px] font-bold text-zinc-700 mb-2 uppercase tracking-tight">
                     {t("businessRegistrationNumber")}
                  </label>
                  <input
                     {...register("businessRegistrationNumber")}
                     placeholder={t("businessRegistrationNumberPlaceholder")}
                     className="w-full px-4 py-3 rounded-xl border border-zinc-200 focus:border-[#003580] focus:ring-4 focus:ring-blue-50 outline-none transition-all placeholder:text-zinc-300 text-zinc-800"
                  />
                  {errors.businessRegistrationNumber && (
                     <p className="text-xs text-red-500 mt-1">
                        {errors.businessRegistrationNumber.message}
                     </p>
                  )}
               </div>

               <div>
                  <label className="block text-[13px] font-bold text-zinc-700 mb-2 uppercase tracking-tight">
                     {t("taxCode")}
                  </label>
                  <input
                     {...register("taxCode")}
                     placeholder={t("taxCodePlaceholder")}
                     className="w-full px-4 py-3 rounded-xl border border-zinc-200 focus:border-[#003580] focus:ring-4 focus:ring-blue-50 outline-none transition-all placeholder:text-zinc-300 text-zinc-800"
                  />
                  {errors.taxCode && (
                     <p className="text-xs text-red-500 mt-1">{errors.taxCode.message}</p>
                  )}
               </div>

               <div className="md:col-span-2">
                  <label className="block text-[13px] font-bold text-zinc-700 mb-2 uppercase tracking-tight">
                     {t("legalOwnerName")}
                  </label>
                  <input
                     {...register("legalOwnerName")}
                     placeholder={t("legalOwnerNamePlaceholder")}
                     className="w-full px-4 py-3 rounded-xl border border-zinc-200 focus:border-[#003580] focus:ring-4 focus:ring-blue-50 outline-none transition-all placeholder:text-zinc-300 text-zinc-800"
                  />
                  {errors.legalOwnerName && (
                     <p className="text-xs text-red-500 mt-1">{errors.legalOwnerName.message}</p>
                  )}
               </div>
            </div>
         </section>

         {/* Review Summary Dashboard */}
         <section className="space-y-6 border-t border-zinc-100 pt-8">
            <div className="flex items-center gap-3">
               <CheckCircle2 className="h-6 w-6 text-[#003580]" />
               <div>
                  <h2 className="text-xl font-bold text-zinc-900">{t("reviewTitle")}</h2>
                  <p className="text-xs text-zinc-500">{t("reviewSubtitle")}</p>
               </div>
            </div>

            <div className="p-6 rounded-2xl bg-zinc-50/50 border border-zinc-100 space-y-6 text-sm">
               {/* Grid of basic info */}
               <div className="grid grid-cols-1 md:grid-cols-2 gap-6 border-b border-zinc-200/60 pb-6">
                  <div>
                     <h4 className="font-bold text-zinc-700 uppercase tracking-wider text-[11px] mb-2">
                        {t("reviewBasicInfo")}
                     </h4>
                     <div className="space-y-1 text-zinc-600">
                        <p>
                           <span className="font-semibold text-zinc-900">Name:</span>{" "}
                           {watchAllFields.name}
                        </p>
                        <p>
                           <span className="font-semibold text-zinc-900">Type:</span>{" "}
                           {watchAllFields.propertyType}
                        </p>
                        <p>
                           <span className="font-semibold text-zinc-900">Check-in:</span>{" "}
                           {watchAllFields.checkInTime} |{" "}
                           <span className="font-semibold text-zinc-900">Check-out:</span>{" "}
                           {watchAllFields.checkOutTime}
                        </p>
                     </div>
                  </div>
                  <div>
                     <h4 className="font-bold text-zinc-700 uppercase tracking-wider text-[11px] mb-2">
                        {t("reviewLocation")}
                     </h4>
                     <div className="space-y-1 text-zinc-600">
                        <p>
                           <span className="font-semibold text-zinc-900">Address:</span>{" "}
                           {watchAllFields.address}
                        </p>
                        <p>
                           <span className="font-semibold text-zinc-900">City:</span>{" "}
                           {watchAllFields.city}
                        </p>
                        <p>
                           <span className="font-semibold text-zinc-900">Country:</span>{" "}
                           {watchAllFields.country}
                        </p>
                     </div>
                  </div>
               </div>

               {/* Rooms & Amenities Summary */}
               <div className="grid grid-cols-1 md:grid-cols-2 gap-6 border-b border-zinc-200/60 pb-6">
                  <div>
                     <h4 className="font-bold text-zinc-700 uppercase tracking-wider text-[11px] mb-2">
                        {t("reviewRooms", { count: roomTypes.length })}
                     </h4>
                     {roomTypes.length > 0 ? (
                        <div className="max-h-40 overflow-y-auto pr-2 space-y-2">
                           {roomTypes.map((room, index) => {
                              if (!room) return null;
                              return (
                                 <div
                                    key={index}
                                    className="p-2 rounded bg-white border border-zinc-100 flex justify-between items-center text-xs text-zinc-800"
                                 >
                                    <div>
                                       <p className="font-semibold text-zinc-955">
                                          {room.name || `Room #${index + 1}`}
                                       </p>
                                       <p className="text-zinc-500">
                                          {room.bedType} • Max {room.capacityAdults} Guests
                                       </p>
                                    </div>
                                    <span className="font-bold text-[#003580]">
                                       {new Intl.NumberFormat("vi-VN", {
                                          style: "currency",
                                          currency: "VND",
                                       }).format(room.basePrice || 0)}
                                    </span>
                                 </div>
                              );
                           })}
                        </div>
                     ) : (
                        <p className="text-zinc-400 text-xs">No rooms configured.</p>
                     )}
                  </div>
                  <div>
                     <h4 className="font-bold text-zinc-700 uppercase tracking-wider text-[11px] mb-2">
                        {t("reviewAmenities", { count: amenities.length })}
                     </h4>
                     <div className="flex flex-wrap gap-1.5 max-h-40 overflow-y-auto pr-2">
                        {amenities.map((amenity) => (
                           <span
                              key={amenity}
                              className="inline-flex items-center px-2 py-1 rounded bg-blue-50 text-[#003580] text-[11px] font-bold"
                           >
                              {amenity}
                           </span>
                        ))}
                        {amenities.length === 0 && (
                           <p className="text-zinc-400 text-xs">No amenities selected.</p>
                        )}
                     </div>
                  </div>
               </div>

               {/* Images & Legal Summary */}
               <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                     <h4 className="font-bold text-zinc-700 uppercase tracking-wider text-[11px] mb-2">
                        {t("reviewImages", { count: images.length })}
                     </h4>
                     <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-thin">
                        {images.map((img, index) => (
                           <div
                              key={index}
                              className="relative h-14 w-20 rounded-lg overflow-hidden shrink-0 border border-zinc-200"
                           >
                              <Image
                                 src={img.preview}
                                 alt={`Preview ${index}`}
                                 className="h-full w-full object-cover"
                                 width={80}
                                 height={56}
                                 unoptimized
                              />
                              {index === 0 && (
                                 <span className="absolute bottom-0 inset-x-0 bg-[#003580]/90 text-white text-[8px] font-bold text-center py-0.5 uppercase">
                                    Main
                                 </span>
                              )}
                           </div>
                        ))}
                     </div>
                  </div>
                  <div>
                     <h4 className="font-bold text-zinc-700 uppercase tracking-wider text-[11px] mb-2">
                        {t("reviewLegal")}
                     </h4>
                     <div className="space-y-1 text-zinc-600 text-xs">
                        <p>
                           <span className="font-semibold text-zinc-900">Owner:</span>{" "}
                           {watchAllFields.legalOwnerName}
                        </p>
                        <p>
                           <span className="font-semibold text-zinc-900">Reg No:</span>{" "}
                           {watchAllFields.businessRegistrationNumber}
                        </p>
                        <p>
                           <span className="font-semibold text-zinc-900">Tax Code:</span>{" "}
                           {watchAllFields.taxCode}
                        </p>
                     </div>
                  </div>
               </div>
            </div>
         </section>

         {/* Terms Checkbox */}
         <section className="space-y-4">
            <label className="flex items-start gap-3 cursor-pointer group">
               <input
                  type="checkbox"
                  {...register("agreeToTerms")}
                  className="mt-1 h-4 w-4 rounded border-zinc-300 text-[#003580] focus:ring-[#003580]"
               />
               <span className="text-xs text-zinc-500 group-hover:text-zinc-700 transition-colors leading-relaxed">
                  {t("termsAgreement")}
               </span>
            </label>
            {errors.agreeToTerms && (
               <p className="text-xs text-red-500">{errors.agreeToTerms.message}</p>
            )}
         </section>

         <div className="flex justify-between pt-8 border-t border-zinc-100">
            <button
               type="button"
               onClick={onBack}
               disabled={isSubmitting}
               className="flex items-center gap-2 px-8 py-3.5 bg-zinc-100 text-zinc-700 rounded-xl font-bold hover:bg-zinc-200 transition-all active:scale-95 disabled:opacity-50"
            >
               <ChevronLeft className="h-5 w-5" /> {t("back")}
            </button>
            <button
               type="submit"
               disabled={isSubmitting}
               className="flex items-center gap-2 px-10 py-3.5 bg-[#006ce4] text-white rounded-xl font-bold hover:bg-[#005bb8] transition-all active:scale-95 shadow-lg shadow-blue-200 disabled:opacity-50"
            >
               {isSubmitting ? (
                  <>
                     <Loader2 className="h-5 w-5 animate-spin" /> {t("processing")}
                  </>
               ) : (
                  t("finish")
               )}
            </button>
         </div>
      </div>
   );
}
