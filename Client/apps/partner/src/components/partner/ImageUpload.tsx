"use client";

import { useState, useCallback } from "react";
import { Upload, X, Image as ImageIcon, Loader2, Plus } from "lucide-react";
import Image from "next/image";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

interface ImageUploadProps {
   onUploadSuccess: (file: File) => void;
   onRemove: (index: number) => void;
   onReorder: (newImages: { file: File; preview: string; isUploading: boolean }[]) => void;
   images: { file: File; preview: string; isUploading: boolean }[];
}

export default function ImageUpload({
   onUploadSuccess,
   onRemove,
   onReorder,
   images,
}: ImageUploadProps) {
   const t = useTranslations("Partner.imageUpload");
   const [isDragging, setIsDragging] = useState(false);
   const [draggedIndex, setDraggedIndex] = useState<number | null>(null);

   const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const files = e.target.files;
      if (files) {
         Array.from(files).forEach((file) => {
            if (file.type.startsWith("image/")) {
               onUploadSuccess(file);
            } else {
               toast.error(t("invalidFile", { name: file.name }));
            }
         });
      }
   };

   const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);

   // Drag & Drop logic for reordering
   const handleDragStart = (e: React.DragEvent, index: number) => {
      e.stopPropagation();
      setDraggedIndex(index);
      e.dataTransfer.effectAllowed = "move";
      // To work in Firefox
      e.dataTransfer.setData("text/plain", index.toString());
   };

   const handleDragOverItem = (e: React.DragEvent, index: number) => {
      e.preventDefault();
      e.stopPropagation();
      e.dataTransfer.dropEffect = "move";
      if (draggedIndex !== null && draggedIndex !== index) {
         setDragOverIndex(index);
      }
   };

   const handleDragLeaveItem = (e: React.DragEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setDragOverIndex(null);
   };

   const handleDropItem = (e: React.DragEvent, targetIndex: number) => {
      e.preventDefault();
      e.stopPropagation();
      setDragOverIndex(null);
      if (draggedIndex === null || draggedIndex === targetIndex) return;

      const newImages = [...images];
      const draggedItem = newImages[draggedIndex];
      newImages.splice(draggedIndex, 1);
      newImages.splice(targetIndex, 0, draggedItem);

      onReorder(newImages);
      setDraggedIndex(null);
   };

   const onDragOver = useCallback((e: React.DragEvent) => {
      e.preventDefault();
      // Only show upload overlay when dragging actual files from outside the browser
      if (e.dataTransfer.types.includes("Files")) {
         setIsDragging(true);
      }
   }, []);

   const onDragLeave = useCallback((e: React.DragEvent) => {
      e.preventDefault();
      setIsDragging(false);
   }, []);

   const onDrop = useCallback(
      (e: React.DragEvent) => {
         e.preventDefault();
         setIsDragging(false);
         const files = e.dataTransfer.files;
         if (files) {
            Array.from(files).forEach((file) => {
               if (file.type.startsWith("image/")) {
                  onUploadSuccess(file);
               }
            });
         }
      },
      [onUploadSuccess]
   );

   return (
      <div
         onDragOver={onDragOver}
         onDragLeave={onDragLeave}
         onDrop={onDrop}
         className="relative w-full transition-all duration-300"
      >
         {/* Premium Drag and Drop Overlay */}
         {isDragging && (
            <div className="absolute inset-0 z-30 bg-blue-50/80 backdrop-blur-[2px] border-2 border-dashed border-[#006ce4] rounded-2xl flex flex-col items-center justify-center animate-in fade-in duration-200">
               <Upload className="h-8 w-8 text-[#006ce4] animate-bounce mb-2" />
               <span className="text-sm font-semibold text-[#006ce4]">{t("clickToUpload")}</span>
            </div>
         )}

         {images.length === 0 ? (
            <div
               className={`relative flex flex-col items-center justify-center border-2 border-dashed rounded-2xl p-10 transition-all duration-300 ${
                  isDragging
                     ? "border-[#006ce4] bg-blue-50/50 scale-[0.99]"
                     : "border-zinc-200 hover:border-zinc-300 bg-zinc-50/30"
               }`}
            >
               <input
                  type="file"
                  multiple
                  accept="image/*"
                  onChange={handleFileChange}
                  className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
               />
               <div className="flex h-12 w-12 items-center justify-center rounded-full bg-white shadow-sm border border-zinc-100 mb-4">
                  <Upload className="h-6 w-6 text-zinc-600" />
               </div>
               <p className="text-sm font-semibold text-zinc-900">{t("clickToUpload")}</p>
               <p className="text-xs text-zinc-500 mt-1">{t("acceptedFormats")}</p>
            </div>
         ) : (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
               {/* Add More card at the start of the grid */}
               <div className="relative aspect-square rounded-xl overflow-hidden border-2 border-dashed border-zinc-200 hover:border-[#006ce4] hover:bg-zinc-50 transition-all duration-300 group cursor-pointer">
                  {/* Blurry background of the main image */}
                  <Image
                     src={images[0].preview}
                     alt="Main blur background"
                     fill
                     sizes="(max-width: 768px) 50vw, 25vw"
                     className="object-cover blur-[5px] opacity-40 scale-105"
                     unoptimized
                  />
                  <input
                     type="file"
                     multiple
                     accept="image/*"
                     onChange={handleFileChange}
                     className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10"
                  />
                  <div className="absolute inset-0 flex flex-col items-center justify-center p-2 text-center bg-black/5 group-hover:bg-transparent transition-colors">
                     <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white shadow-md border border-zinc-100 mb-2 group-hover:scale-110 transition-transform">
                        <Plus className="h-5 w-5 text-zinc-600 group-hover:text-[#006ce4] transition-colors" />
                     </div>
                     <span className="text-xs font-semibold text-zinc-700 group-hover:text-[#006ce4] transition-colors">
                        {t("addMore")}
                     </span>
                  </div>
               </div>

               {/* Existing uploaded images */}
               {images.map((img, index) => (
                  <div
                     key={index}
                     draggable
                     onDragStart={(e) => handleDragStart(e, index)}
                     onDragOver={(e) => handleDragOverItem(e, index)}
                     onDragLeave={handleDragLeaveItem}
                     onDrop={(e) => handleDropItem(e, index)}
                     onDragEnd={() => {
                        setDraggedIndex(null);
                        setDragOverIndex(null);
                     }}
                     className={`group relative aspect-square rounded-xl overflow-hidden border-2 transition-all cursor-move ${
                        draggedIndex === index
                           ? "opacity-40 border-[#006ce4] scale-95"
                           : "border-zinc-100 shadow-sm hover:border-zinc-300"
                     }`}
                  >
                     <Image
                        src={img.preview}
                        alt={`Preview ${index}`}
                        fill
                        sizes="(max-width: 768px) 50vw, 25vw"
                        className="object-cover"
                        unoptimized
                     />
                     <div className="absolute inset-0 bg-black/10 opacity-0 group-hover:opacity-100 transition-opacity" />

                     <button
                        type="button"
                        onClick={() => onRemove(index)}
                        className="absolute top-2 right-2 h-7 w-7 flex items-center justify-center rounded-full bg-white/90 text-zinc-900 shadow-lg hover:bg-white transition-all transform scale-0 group-hover:scale-100 z-10"
                     >
                        <X className="h-4 w-4" />
                     </button>

                     {img.isUploading && (
                        <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/40 backdrop-blur-[2px] text-white">
                           <Loader2 className="h-6 w-6 animate-spin mb-2" />
                           <span className="text-[10px] font-bold tracking-wider uppercase">
                              {t("uploading")}
                           </span>
                        </div>
                     )}

                     {index === 0 ? (
                        <div className="absolute bottom-2 left-2 px-2 py-0.5 rounded-md bg-[#003580] text-[10px] font-bold text-white shadow-sm flex items-center gap-1">
                           <ImageIcon className="h-3 w-3" />
                           {t("mainImage")}
                        </div>
                     ) : (
                        <div className="absolute top-2 left-2 h-6 w-6 flex items-center justify-center rounded-md bg-black/40 text-[10px] font-bold text-white opacity-0 group-hover:opacity-100 transition-opacity">
                           #{index + 1}
                        </div>
                     )}

                     {/* Insertion edge indicator (Rendered last so it sits on top of everything) */}
                     {dragOverIndex === index && draggedIndex !== null && (
                        <div
                           className={`absolute top-0 bottom-0 w-[5px] bg-[#006ce4] z-50 shadow-[0_0_8px_rgba(0,108,228,0.5)] ${
                              draggedIndex < index ? "right-0" : "left-0"
                           }`}
                        />
                     )}
                  </div>
               ))}
            </div>
         )}
      </div>
   );
}
