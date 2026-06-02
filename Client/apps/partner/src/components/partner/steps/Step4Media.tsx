"use client";

import { useFormContext } from "react-hook-form";
import { useTranslations } from "next-intl";
import { Building2, ImageIcon, ChevronLeft, ChevronRight } from "lucide-react";
import ImageUpload from "../ImageUpload";
import type { PropertyFormValues } from "../CreatePropertyForm";

interface Step4MediaProps {
   images: { file: File; preview: string; isUploading: boolean }[];
   setImages: React.Dispatch<
      React.SetStateAction<{ file: File; preview: string; isUploading: boolean }[]>
   >;
   onBack: () => void;
   onNext: () => void;
}

export default function Step4Media({ images, setImages, onBack, onNext }: Step4MediaProps) {
   const t = useTranslations("Partner.createPropertyForm");
   const {
      register,
      formState: { errors },
   } = useFormContext<PropertyFormValues>();

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

   return (
      <div className="space-y-8 animate-in fade-in slide-in-from-right-4 duration-500">
         <section className="space-y-6">
            <div className="flex items-center gap-3">
               <Building2 className="h-5 w-5 text-[#003580]" />
               <div>
                  <h2 className="text-xl font-bold text-zinc-900">{t("description")}</h2>
               </div>
            </div>
            <textarea
               {...register("description")}
               placeholder={t("placeholders.description")}
               rows={6}
               className="w-full px-4 py-3 rounded-xl border border-zinc-200 focus:border-[#003580] focus:ring-4 focus:ring-blue-50 outline-none transition-all placeholder:text-zinc-300 resize-none text-zinc-800"
            />
            {errors.description && (
               <p className="text-xs text-red-500">{errors.description.message}</p>
            )}
         </section>

         <section className="space-y-6 border-t border-zinc-100 pt-8">
            <div className="flex items-center gap-3">
               <ImageIcon className="h-5 w-5 text-[#003580]" />
               <div>
                  <h2 className="text-xl font-bold text-zinc-900">{t("imagesTitle")}</h2>
                  <p className="text-xs text-zinc-500 mt-1">{t("messages.imageUploadDesc")}</p>
               </div>
            </div>

            {/* Progress bar / count indicator for images */}
            <div className="flex items-center justify-between bg-zinc-50 px-4 py-3 rounded-xl border border-zinc-100">
               <span className="text-xs font-bold text-zinc-500">Upload Progress</span>
               <div className="flex items-center gap-3">
                  <span
                     className={`text-xs font-extrabold ${
                        images.length >= 5 ? "text-emerald-600" : "text-amber-500"
                     }`}
                  >
                     {images.length} / 5+ images
                  </span>
                  <div className="w-24 bg-zinc-200 h-2 rounded-full overflow-hidden">
                     <div
                        className={`h-full transition-all duration-500 ${
                           images.length >= 5 ? "bg-emerald-500" : "bg-amber-500"
                        }`}
                        style={{ width: `${Math.min((images.length / 5) * 100, 100)}%` }}
                     ></div>
                  </div>
               </div>
            </div>

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
