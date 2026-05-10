"use client";

import { useState } from "react";
import { useForm, FieldErrors } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
   Building2,
   MapPin,
   Clock,
   ChevronRight,
   ChevronLeft,
   CheckCircle2,
   Hotel,
   Home,
   Palmtree,
   Building,
   ImageIcon,
   Loader2,
} from "lucide-react";
import { propertyService } from "@/lib/api/propertyService";
import { toast } from "sonner";
import ImageUpload from "./ImageUpload";
import { useRouter } from "next/navigation";

const propertySchema = z.object({
   name: z.string().min(5, "Tên chỗ nghỉ phải có ít nhất 5 ký tự"),
   description: z.string().min(20, "Mô tả phải có ít nhất 20 ký tự"),
   propertyType: z.enum(["HOTEL", "APARTMENT", "VILLA", "RESORT", "HOMESTAY", "GUESTHOUSE"]),
   address: z.string().min(5, "Địa chỉ không được để trống"),
   city: z.string().min(2, "Thành phố không được để trống"),
   country: z.string().min(2, "Quốc gia không được để trống"),
   starRating: z.number().min(0).max(5),
   checkInTime: z.string(),
   checkOutTime: z.string(),
});

type PropertyFormValues = z.infer<typeof propertySchema>;

const PROPERTY_TYPES = [
   {
      id: "HOTEL",
      label: "Khách sạn",
      icon: Hotel,
      description: "Dành cho khách sạn, nhà nghỉ có lễ tân 24/7",
   },
   {
      id: "APARTMENT",
      label: "Căn hộ",
      icon: Building,
      description: "Căn hộ chung cư, studio đầy đủ tiện nghi",
   },
   {
      id: "VILLA",
      label: "Biệt thự",
      icon: Home,
      description: "Nhà nguyên căn, biệt thự nghỉ dưỡng riêng tư",
   },
   {
      id: "HOMESTAY",
      label: "Homestay",
      icon: Palmtree,
      description: "Nhà dân, trải nghiệm văn hóa địa phương",
   },
   {
      id: "RESORT",
      label: "Resort",
      icon: Building2,
      description: "Khu nghỉ dưỡng cao cấp, dịch vụ trọn gói",
   },
];

export default function CreatePropertyForm() {
   const [step, setStep] = useState(1);
   const [isSubmitting, setIsSubmitting] = useState(false);
   const [images, setImages] = useState<{ file: File; preview: string; isUploading: boolean }[]>(
      []
   );
   const [isFlexibleTime, setIsFlexibleTime] = useState(false);
   const router = useRouter();

   const {
      register,
      handleSubmit,
      setValue,
      watch,
      trigger,
      formState: { errors },
   } = useForm<PropertyFormValues>({
      resolver: zodResolver(propertySchema),
      defaultValues: {
         propertyType: "HOTEL",
         starRating: 0,
         checkInTime: "14:00",
         checkOutTime: "12:00",
      },
   });

   const selectedType = watch("propertyType");

   const handleImageUpload = (file: File) => {
      const preview = URL.createObjectURL(file);
      setImages((prev) => [...prev, { file, preview, isUploading: false }]);
   };

   const removeImage = (index: number) => {
      setImages((prev) => {
         const newImages = [...prev];
         URL.revokeObjectURL(newImages[index].preview);
         newImages.splice(index, 1);
         return newImages;
      });
   };

   const onSubmit = async (data: PropertyFormValues) => {
      if (images.length === 0) {
         toast.error("Vui lòng tải lên ít nhất một ảnh cho chỗ nghỉ");
         return;
      }

      setIsSubmitting(true);
      try {
         // 1. Create Property
         const property = await propertyService.createProperty(data);
         toast.success("Đã tạo chỗ nghỉ thành công!");

         // 2. Upload Images in Background
         setImages((prev) => prev.map((img) => ({ ...img, isUploading: true })));

         const uploadPromises = images.map((img, index) =>
            propertyService.uploadMedia(img.file, property.id, "PROPERTY", index === 0)
         );

         await Promise.all(uploadPromises);
         toast.success("Đã gửi yêu cầu xử lý ảnh lên hệ thống");

         router.push("/partner/dashboard");
      } catch (error: unknown) {
         const apiError = error as { response?: { data?: { message?: string } } };
         toast.error(apiError.response?.data?.message || "Đã xảy ra lỗi khi tạo chỗ nghỉ");
      } finally {
         setIsSubmitting(false);
      }
   };

   const onInvalid = (errors: FieldErrors<PropertyFormValues>) => {
      const firstError = Object.values(errors)[0];
      if (firstError?.message) {
         toast.error("Vui lòng kiểm tra lại thông tin", {
            description: firstError.message,
         });
      }
   };

   const nextStep = async () => {
      let fieldsToValidate: (keyof PropertyFormValues)[] = [];
      if (step === 1) fieldsToValidate = ["name", "description", "propertyType"];
      if (step === 2)
         fieldsToValidate = ["address", "city", "country", "checkInTime", "checkOutTime"];

      const isValid = await trigger(fieldsToValidate);
      if (isValid) {
         setStep((s) => s + 1);
      } else {
         toast.error("Vui lòng điền đầy đủ thông tin ở bước này");
      }
   };

   const prevStep = () => setStep((s) => s - 1);

   return (
      <div className="max-w-4xl mx-auto">
         {/* Stepper Header */}
         <div className="flex items-center justify-between mb-12 relative px-4">
            <div className="absolute top-1/2 left-0 w-full h-0.5 bg-zinc-100 -translate-y-1/2 z-0" />
            {[1, 2, 3].map((s) => (
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
                     {s === 1 ? "Thông tin" : s === 2 ? "Vị trí" : "Hình ảnh"}
                  </span>
               </div>
            ))}
         </div>

         <form
            onSubmit={handleSubmit(onSubmit, onInvalid)}
            className="bg-white rounded-3xl border border-zinc-100 shadow-xl shadow-zinc-200/50 overflow-hidden"
         >
            <div className="p-8 md:p-12">
               {/* Step 1: Basic Info */}
               {step === 1 && (
                  <div className="space-y-8 animate-in fade-in slide-in-from-right-4 duration-500">
                     <section className="space-y-6">
                        <div className="flex items-center gap-3 mb-2">
                           <Building2 className="h-5 w-5 text-[#003580]" />
                           <h2 className="text-xl font-bold text-zinc-900">Thông tin cơ bản</h2>
                        </div>

                        <div className="space-y-4">
                           <div>
                              <label className="block text-[13px] font-bold text-zinc-700 mb-2 uppercase tracking-tight">
                                 Tên chỗ nghỉ
                              </label>
                              <input
                                 {...register("name")}
                                 placeholder="Ví dụ: Omni Luxury Hotel & Spa"
                                 className="w-full px-4 py-3 rounded-xl border border-zinc-200 focus:border-[#003580] focus:ring-4 focus:ring-blue-50 outline-none transition-all placeholder:text-zinc-300"
                              />
                              {errors.name && (
                                 <p className="mt-2 text-xs text-red-500 font-medium">
                                    {errors.name.message}
                                 </p>
                              )}
                           </div>

                           <div>
                              <label className="block text-[13px] font-bold text-zinc-700 mb-2 uppercase tracking-tight">
                                 Mô tả chi tiết
                              </label>
                              <textarea
                                 {...register("description")}
                                 rows={4}
                                 placeholder="Hãy giới thiệu những điểm nổi bật nhất của chỗ nghỉ..."
                                 className="w-full px-4 py-3 rounded-xl border border-zinc-200 focus:border-[#003580] focus:ring-4 focus:ring-blue-50 outline-none transition-all placeholder:text-zinc-300 resize-none"
                              />
                              {errors.description && (
                                 <p className="mt-2 text-xs text-red-500 font-medium">
                                    {errors.description.message}
                                 </p>
                              )}
                           </div>
                        </div>
                     </section>

                     <section className="space-y-6">
                        <label className="block text-[13px] font-bold text-zinc-700 mb-2 uppercase tracking-tight">
                           Loại hình chỗ nghỉ
                        </label>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                           {PROPERTY_TYPES.map((type) => (
                              <button
                                 key={type.id}
                                 type="button"
                                 onClick={() =>
                                    setValue(
                                       "propertyType",
                                       type.id as PropertyFormValues["propertyType"]
                                    )
                                 }
                                 className={`flex items-start gap-4 p-4 rounded-2xl border-2 text-left transition-all duration-300 ${
                                    selectedType === type.id
                                       ? "border-[#003580] bg-blue-50/50"
                                       : "border-zinc-100 hover:border-zinc-200 bg-zinc-50/30"
                                 }`}
                              >
                                 <div
                                    className={`p-3 rounded-xl ${selectedType === type.id ? "bg-[#003580] text-white" : "bg-white text-zinc-400 shadow-sm"}`}
                                 >
                                    <type.icon className="h-6 w-6" />
                                 </div>
                                 <div>
                                    <p className="font-bold text-zinc-900">{type.label}</p>
                                    <p className="text-xs text-zinc-500 mt-1 leading-relaxed">
                                       {type.description}
                                    </p>
                                 </div>
                              </button>
                           ))}
                        </div>
                     </section>

                     <div className="flex justify-end pt-4">
                        <button
                           type="button"
                           onClick={nextStep}
                           className="flex items-center gap-2 px-8 py-3.5 bg-[#003580] text-white rounded-xl font-bold hover:bg-[#002b66] transition-all active:scale-95 shadow-lg shadow-blue-200"
                        >
                           Tiếp theo <ChevronRight className="h-5 w-5" />
                        </button>
                     </div>
                  </div>
               )}

               {/* Step 2: Location & Rating */}
               {step === 2 && (
                  <div className="space-y-8 animate-in fade-in slide-in-from-right-4 duration-500">
                     <section className="space-y-6">
                        <div className="flex items-center gap-3 mb-2">
                           <MapPin className="h-5 w-5 text-[#003580]" />
                           <h2 className="text-xl font-bold text-zinc-900">Vị trí & Xếp hạng</h2>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                           <div className="md:col-span-2">
                              <label className="block text-[13px] font-bold text-zinc-700 mb-2 uppercase tracking-tight">
                                 Địa chỉ cụ thể
                              </label>
                              <input
                                 {...register("address")}
                                 className="w-full px-4 py-3 rounded-xl border border-zinc-200 focus:border-[#003580] focus:ring-4 focus:ring-blue-50 outline-none transition-all"
                              />
                           </div>
                           <div>
                              <label className="block text-[13px] font-bold text-zinc-700 mb-2 uppercase tracking-tight">
                                 Thành phố
                              </label>
                              <input
                                 {...register("city")}
                                 className="w-full px-4 py-3 rounded-xl border border-zinc-200 focus:border-[#003580] focus:ring-4 focus:ring-blue-50 outline-none transition-all"
                              />
                           </div>
                           <div>
                              <label className="block text-[13px] font-bold text-zinc-700 mb-2 uppercase tracking-tight">
                                 Quốc gia
                              </label>
                              <input
                                 {...register("country")}
                                 className="w-full px-4 py-3 rounded-xl border border-zinc-200 focus:border-[#003580] focus:ring-4 focus:ring-blue-50 outline-none transition-all"
                              />
                           </div>
                        </div>
                     </section>

                     <section className="space-y-6">
                        <div className="flex items-center justify-between">
                           <div className="flex items-center gap-3">
                              <Clock className="h-5 w-5 text-[#003580]" />
                              <h2 className="text-xl font-bold text-zinc-900">
                                 Thời gian Nhận/Trả phòng
                              </h2>
                           </div>
                           <label className="flex items-center gap-3 cursor-pointer group">
                              <span className="text-[13px] font-bold text-zinc-500 group-hover:text-[#003580] transition-colors">
                                 Linh hoạt 24/7
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
                                 Nhận phòng
                              </label>
                              <div className="relative">
                                 <input
                                    type="time"
                                    disabled={isFlexibleTime}
                                    {...register("checkInTime")}
                                    className={`w-full px-4 py-3 rounded-xl border border-zinc-200 outline-none transition-all ${isFlexibleTime ? "bg-transparent cursor-not-allowed" : "focus:border-[#003580] bg-white"}`}
                                 />
                                 {isFlexibleTime && (
                                    <div className="absolute inset-0 flex items-center justify-center bg-zinc-50/10 backdrop-blur-[1px] rounded-xl text-[10px] font-bold text-zinc-400 uppercase">
                                       Anytime
                                    </div>
                                 )}
                              </div>
                           </div>
                           <div>
                              <label
                                 className={`block text-[11px] font-bold mb-2 uppercase tracking-widest ${isFlexibleTime ? "text-zinc-400" : "text-zinc-500"}`}
                              >
                                 Trả phòng
                              </label>
                              <div className="relative">
                                 <input
                                    type="time"
                                    disabled={isFlexibleTime}
                                    {...register("checkOutTime")}
                                    className={`w-full px-4 py-3 rounded-xl border border-zinc-200 outline-none transition-all ${isFlexibleTime ? "bg-transparent cursor-not-allowed" : "focus:border-[#003580] bg-white"}`}
                                 />
                                 {isFlexibleTime && (
                                    <div className="absolute inset-0 flex items-center justify-center bg-zinc-50/10 backdrop-blur-[1px] rounded-xl text-[10px] font-bold text-zinc-400 uppercase">
                                       Anytime
                                    </div>
                                 )}
                              </div>
                           </div>
                        </div>

                        {isFlexibleTime && (
                           <div className="flex items-center gap-3 p-4 rounded-xl bg-blue-50 border border-blue-100 animate-in fade-in zoom-in-95 duration-300">
                              <div className="h-2 w-2 rounded-full bg-blue-500 animate-pulse"></div>
                              <p className="text-[12px] font-medium text-[#003580]">
                                 Chế độ 24/7 đang bật: Khách có thể nhận và trả phòng vào bất kỳ lúc
                                 nào trong ngày.
                              </p>
                           </div>
                        )}
                     </section>

                     <div className="flex justify-between pt-4">
                        <button
                           type="button"
                           onClick={prevStep}
                           className="flex items-center gap-2 px-8 py-3.5 bg-zinc-100 text-zinc-700 rounded-xl font-bold hover:bg-zinc-200 transition-all active:scale-95"
                        >
                           <ChevronLeft className="h-5 w-5" /> Quay lại
                        </button>
                        <button
                           type="button"
                           onClick={nextStep}
                           className="flex items-center gap-2 px-8 py-3.5 bg-[#003580] text-white rounded-xl font-bold hover:bg-[#002b66] transition-all active:scale-95 shadow-lg shadow-blue-200"
                        >
                           Tiếp theo <ChevronRight className="h-5 w-5" />
                        </button>
                     </div>
                  </div>
               )}

               {/* Step 3: Images */}
               {step === 3 && (
                  <div className="space-y-8 animate-in fade-in slide-in-from-right-4 duration-500">
                     <section className="space-y-6">
                        <div className="flex items-center gap-3 mb-2">
                           <ImageIcon className="h-5 w-5 text-[#003580]" />
                           <h2 className="text-xl font-bold text-zinc-900">Hình ảnh chỗ nghỉ</h2>
                        </div>
                        <p className="text-sm text-zinc-500">
                           Tải lên ít nhất 1 ảnh. Ảnh đầu tiên sẽ được chọn làm ảnh đại diện.
                        </p>

                        <ImageUpload
                           images={images}
                           onUploadSuccess={handleImageUpload}
                           onRemove={removeImage}
                           onReorder={setImages}
                        />
                     </section>

                     <div className="flex justify-between pt-8 border-t border-zinc-100">
                        <button
                           type="button"
                           onClick={prevStep}
                           disabled={isSubmitting}
                           className="flex items-center gap-2 px-8 py-3.5 bg-zinc-100 text-zinc-700 rounded-xl font-bold hover:bg-zinc-200 transition-all active:scale-95 disabled:opacity-50"
                        >
                           <ChevronLeft className="h-5 w-5" /> Quay lại
                        </button>
                        <button
                           type="submit"
                           disabled={isSubmitting}
                           className="flex items-center gap-2 px-10 py-3.5 bg-[#006ce4] text-white rounded-xl font-bold hover:bg-[#005bb8] transition-all active:scale-95 shadow-lg shadow-blue-200 disabled:opacity-50"
                        >
                           {isSubmitting ? (
                              <>
                                 <Loader2 className="h-5 w-5 animate-spin" /> Đang xử lý...
                              </>
                           ) : (
                              "Hoàn tất & Đăng ký"
                           )}
                        </button>
                     </div>
                  </div>
               )}
            </div>
         </form>
      </div>
   );
}
