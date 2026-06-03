"use client";

import { useState, useMemo, useCallback } from "react";
import { useForm, FieldErrors, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Building2, CheckCircle2 } from "lucide-react";
import { propertyService } from "@/lib/api/propertyService";
import { toast } from "sonner";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";

import { useSettingStore } from "@/store/useSettingStore";
import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api/apiClient";

import Step1BasicInfo from "./steps/Step1BasicInfo";
import Step2Location from "./steps/Step2Location";
import Step3Setup from "./steps/Step3Setup";
import Step4Media from "./steps/Step4Media";
import Step5Legal from "./steps/Step5Legal";

export type PropertyFormValues = z.infer<ReturnType<typeof getPropertySchema>>;

const getPropertySchema = (tv: (key: string) => string) =>
   z.object({
      name: z.string().min(5, tv("nameMin")),
      description: z.string().min(20, tv("descMin")),
      propertyType: z.enum(["HOTEL", "APARTMENT", "VILLA", "RESORT", "HOMESTAY", "GUESTHOUSE"]),
      address: z.string().min(5, tv("addressRequired")),
      city: z.string().min(2, tv("cityRequired")),
      country: z.string().min(2, tv("countryRequired")),
      starRating: z.number().min(0).max(5),
      checkInTime: z.string(),
      checkOutTime: z.string(),
      amenities: z.array(z.string()),
      roomTypes: z
         .array(
            z.object({
               name: z.string().min(3, tv("roomNameMin")),
               description: z.string().optional(),
               basePrice: z.number().min(1, tv("roomPriceMin")),
               capacityAdults: z.number().min(1, tv("roomCapacityMin")),
               capacityChildren: z.number().min(0),
               totalRooms: z.number().min(1, tv("totalRoomsMin")),
               roomSizeSqm: z.number().min(1, tv("roomSizeMin")),
               bedType: z.string().min(1, tv("bedTypeRequired")),
            })
         )
         .min(1, tv("roomsRequired")),
      businessRegistrationNumber: z.string().min(5, tv("businessRegRequired")),
      taxCode: z.string().min(5, tv("taxCodeRequired")),
      legalOwnerName: z.string().min(2, tv("legalOwnerRequired")),
      agreeToTerms: z.boolean().refine((val) => val === true, {
         message: tv("agreeTermsRequired"),
      }),
   });

const waitImagesSync = async (propertyId: string): Promise<number> => {
   const startTime = Date.now();
   const maxPollTime = 30000; // 30s timeout

   while (Date.now() - startTime < maxPollTime) {
      try {
         const myProperties = await propertyService.getMyProperties();
         const createdProperty = myProperties.find((p) => p.id === propertyId);
         if (createdProperty && createdProperty.imageUrl) {
            break;
         }
      } catch (error) {
         console.error("Failed to check image status", error);
      }
      await new Promise((resolve) => setTimeout(resolve, 500));
   }
   return Date.now() - startTime;
};

export default function CreatePropertyForm() {
   const t = useTranslations("Partner.createPropertyForm");
   const tHeader = useTranslations("Partner.createProperty");
   const tv = useTranslations("Partner.validation");
   const [step, setStep] = useState(1);
   const [isSubmitting, setIsSubmitting] = useState(false);
   const [images, setImages] = useState<{ file: File; preview: string; isUploading: boolean }[]>(
      []
   );
   const [isFlexibleTime, setIsFlexibleTime] = useState(false);
   const router = useRouter();

   const { currency } = useSettingStore();
   const { data: rates } = useQuery({
      queryKey: ["currency-rates"],
      queryFn: async () => {
         const response = await apiClient.get<unknown, Record<string, number>>("/currencies/rates");
         return response;
      },
      staleTime: 1000 * 60 * 60, // 1 hour
   });

   const propertySchema = useMemo(() => getPropertySchema(tv), [tv]);

   const methods = useForm<PropertyFormValues>({
      resolver: zodResolver(propertySchema),
      defaultValues: {
         name: "",
         description: "",
         propertyType: "HOTEL",
         address: "",
         city: "",
         country: "",
         starRating: 0,
         checkInTime: "14:00",
         checkOutTime: "12:00",
         amenities: [],
         roomTypes: [],
         businessRegistrationNumber: "",
         taxCode: "",
         legalOwnerName: "",
         agreeToTerms: false,
      },
   });

   const { handleSubmit, setValue, trigger, getValues } = methods;

   const handleAddressDetailsChange = useCallback(
      (details: { address: string; city: string; country: string }) => {
         setValue("address", details.address, { shouldValidate: true });
         setValue("city", details.city, { shouldValidate: true });
         setValue("country", details.country, { shouldValidate: true });
      },
      [setValue]
   );

   const onSubmit = async (data: PropertyFormValues) => {
      if (images.length < 5) {
         toast.error(t("messages.imageMinLimit"));
         return;
      }

      setIsSubmitting(true);
      const loadingToastId = toast.loading(
         t("messages.uploading") || "Đang đăng tải thông tin chỗ nghỉ..."
      );
      try {
         // Convert basePrice to USD before submitting
         const rate = rates?.[currency] || 1;
         const convertedRoomTypes = data.roomTypes?.map((room) => ({
            ...room,
            basePrice:
               currency === "USD" ? room.basePrice : Number((room.basePrice / rate).toFixed(2)),
         }));
         const submissionData = {
            ...data,
            roomTypes: convertedRoomTypes,
         };

         // 1. Create Property
         const property = await propertyService.createProperty(submissionData);

         // 2. Upload Images
         setImages((prev) => prev.map((img) => ({ ...img, isUploading: true })));

         const uploadPromises = images.map((img, index) =>
            propertyService.uploadMedia(img.file, property.id, "PROPERTY", index === 0)
         );

         await Promise.all(uploadPromises);

         // Cập nhật toast thành đang xử lý tối ưu ảnh
         toast.loading(
            t("messages.imageProcessing") || "Hình ảnh đang được tối ưu hóa trên hệ thống CDN...",
            {
               id: loadingToastId,
            }
         );

         // 3. Chờ ảnh được Kafka/CDN xử lý xong bất đồng bộ
         const elapsedTime = await waitImagesSync(property.id);

         // Nếu xử lý quá nhanh (dưới 3 giây), cố tình delay thêm cho đủ 3 giây để trải nghiệm mượt mà hơn
         if (elapsedTime < 3000) {
            await new Promise((resolve) => setTimeout(resolve, 3000 - elapsedTime));
         }

         toast.success(t("messages.success") || "Tạo chỗ nghỉ và đồng bộ hình ảnh thành công!", {
            id: loadingToastId,
         });

         router.push("/dashboard");
      } catch (error: unknown) {
         toast.dismiss(loadingToastId);
         const apiError = error as { response?: { data?: { message?: string } } };
         toast.error(apiError.response?.data?.message || t("messages.error"));
      } finally {
         setIsSubmitting(false);
      }
   };

   const onInvalid = (errors: FieldErrors<PropertyFormValues>) => {
      const firstError = Object.values(errors)[0];
      if (firstError?.message) {
         toast.error(t("messages.validationError"), {
            description: firstError.message as string,
         });
      }
   };

   const nextStep = async () => {
      let fieldsToValidate: (keyof PropertyFormValues)[] = [];
      if (step === 1) {
         fieldsToValidate = ["name", "propertyType", "checkInTime", "checkOutTime"];
      } else if (step === 2) {
         fieldsToValidate = ["address", "city", "country"];
      } else if (step === 3) {
         fieldsToValidate = ["roomTypes", "amenities"];
         const roomTypesValue = getValues("roomTypes") || [];
         if (roomTypesValue.length === 0) {
            toast.error(t("messages.roomsRequired"));
            return;
         }
      } else if (step === 4) {
         fieldsToValidate = ["description"];
         const isValid = await trigger(fieldsToValidate);
         if (!isValid) {
            toast.error(t("messages.stepError"));
            return;
         }
         if (images.length < 5) {
            toast.error(t("messages.imageMinLimit"));
            return;
         }
         setStep(5);
         return;
      }

      const isValid = await trigger(fieldsToValidate);
      if (isValid) {
         setStep((s) => s + 1);
      } else {
         toast.error(t("messages.stepError"));
      }
   };

   const prevStep = () => setStep((s) => s - 1);

   return (
      <div className="pb-20">
         {/* Page Header (Only when not in full-screen map Step 2) */}
         {step !== 2 && (
            <div className="bg-white border-b border-zinc-200 mb-12">
               <div className="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8">
                  <div className="flex flex-col items-center text-center">
                     <div className="h-16 w-16 bg-blue-50 text-[#003580] rounded-2xl flex items-center justify-center mb-6 shadow-sm border border-blue-100">
                        <Building2 className="h-8 w-8" />
                     </div>
                     <h1
                        className="text-3xl md:text-4xl font-extrabold text-zinc-900 tracking-tight mb-4 animate-in fade-in duration-700"
                        style={{ fontFamily: "var(--font-be-vietnam-pro)" }}
                     >
                        {tHeader("title")}
                     </h1>
                     <p className="max-w-xl text-zinc-500 text-lg leading-relaxed animate-in fade-in duration-1000">
                        {tHeader("subtitle")}
                     </p>
                  </div>
               </div>
            </div>
         )}

         {/* Compact Sticky Header (Only when in full-screen map Step 2) */}
         {step === 2 && (
            <div className="bg-white border-b border-zinc-200 shadow-sm fixed top-0 inset-x-0 h-20 z-50 flex items-center px-8">
               <div className="flex items-center justify-between w-full max-w-7xl mx-auto">
                  <div className="flex items-center gap-3">
                     <Building2 className="h-6 w-6 text-[#003580]" />
                     <span className="font-bold text-[#003580] text-lg">OmniBooking</span>
                  </div>
                  {/* Stepper Header (Inline compact version for step 2) */}
                  <div className="flex items-center gap-12">
                     {[1, 2, 3, 4, 5].map((s) => (
                        <div key={s} className="flex items-center gap-2">
                           <div
                              className={`h-8 w-8 rounded-full border-2 flex items-center justify-center font-bold text-xs transition-colors duration-300 ${
                                 step >= s
                                    ? "border-[#003580] bg-[#003580] text-white"
                                    : "border-zinc-200 bg-white text-zinc-400"
                              }`}
                           >
                              {s}
                           </div>
                           <span
                              className={`text-xs font-bold uppercase tracking-wider transition-colors duration-300 ${step >= s ? "text-zinc-800" : "text-zinc-400"}`}
                           >
                              {t(`step${s}`)}
                           </span>
                        </div>
                     ))}
                  </div>
                  <div className="w-24"></div> {/* spacer */}
               </div>
            </div>
         )}

         <div className={step === 2 ? "" : "max-w-4xl mx-auto px-4 sm:px-6 lg:px-8"}>
            {step !== 2 && (
               /* Stepper Header for Steps 1, 3, 4 & 5 */
               <div className="flex items-center justify-between mb-12 relative px-4">
                  <div className="absolute top-1/2 left-0 w-full h-0.5 bg-zinc-100 -translate-y-1/2 z-0" />
                  {[1, 2, 3, 4, 5].map((s) => (
                     <div
                        key={s}
                        className={`relative z-10 flex h-10 w-10 items-center justify-center rounded-full border-2 transition-all duration-500 ${
                           step >= s
                              ? "border-[#003580] bg-[#003580] text-white shadow-lg shadow-blue-200"
                              : "border-zinc-200 bg-white text-zinc-400"
                        }`}
                     >
                        {step > s ? (
                           <CheckCircle2 className="h-6 w-6" />
                        ) : (
                           <span className="text-sm font-bold">{s}</span>
                        )}
                        <span className="absolute -bottom-8 left-1/2 -translate-x-1/2 text-[10px] font-bold uppercase tracking-widest text-zinc-400 whitespace-nowrap">
                           {t(`step${s}`)}
                        </span>
                     </div>
                  ))}
               </div>
            )}

            <FormProvider {...methods}>
               {step === 2 ? (
                  /* Full-screen Map for Step 2 */
                  <Step2Location
                     handleAddressDetailsChange={handleAddressDetailsChange}
                     onBack={prevStep}
                     onNext={nextStep}
                  />
               ) : (
                  <form
                     onSubmit={handleSubmit(onSubmit, onInvalid)}
                     className="bg-white rounded-3xl border border-zinc-100 shadow-xl shadow-zinc-200/50 overflow-hidden"
                  >
                     <div className="p-8 md:p-12">
                        {step === 1 && (
                           <Step1BasicInfo
                              isFlexibleTime={isFlexibleTime}
                              setIsFlexibleTime={setIsFlexibleTime}
                              onNext={nextStep}
                           />
                        )}

                        {step === 3 && <Step3Setup onBack={prevStep} onNext={nextStep} />}

                        {step === 4 && (
                           <Step4Media
                              images={images}
                              setImages={setImages}
                              onBack={prevStep}
                              onNext={nextStep}
                           />
                        )}

                        {step === 5 && (
                           <Step5Legal
                              images={images}
                              isSubmitting={isSubmitting}
                              onBack={prevStep}
                           />
                        )}
                     </div>
                  </form>
               )}
            </FormProvider>
         </div>
      </div>
   );
}
