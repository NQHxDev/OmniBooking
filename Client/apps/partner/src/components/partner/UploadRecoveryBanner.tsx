"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { propertyService } from "@/lib/api/propertyService";
import { useTranslations } from "next-intl";
import { AlertTriangle, X, Play, Loader2 } from "lucide-react";
import Link from "next/link";
import { toast } from "sonner";
import { motion, AnimatePresence } from "framer-motion";

export default function UploadRecoveryBanner() {
   const t = useTranslations("Partner.uploadProgress");
   const queryClient = useQueryClient();

   const { data: response, isLoading } = useQuery({
      queryKey: ["incomplete-uploads"],
      queryFn: async () => {
         const res = await propertyService.getIncompleteUploads();
         return res.data || [];
      },
      refetchOnWindowFocus: false,
   });

   const dismissMutation = useMutation({
      mutationFn: async (propertyId: string) => {
         await propertyService.dismissIncompleteUpload(propertyId);
      },
      onSuccess: () => {
         queryClient.invalidateQueries({ queryKey: ["incomplete-uploads"] });
         toast.success("Đã lưu trạng thái ảnh thành công");
      },
      onError: (err) => {
         console.error("Failed to dismiss incomplete upload", err);
         toast.error("Không thể bỏ qua cảnh báo");
      },
   });

   if (isLoading || !response || response.length === 0) {
      return null;
   }

   return (
      <div className="space-y-4 mb-6">
         <AnimatePresence>
            {response.map((item) => (
               <motion.div
                  key={item.propertyId}
                  initial={{ height: 0, opacity: 0, y: -20 }}
                  animate={{ height: "auto", opacity: 1, y: 0 }}
                  exit={{ height: 0, opacity: 0, y: -20 }}
                  className="overflow-hidden"
               >
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-5 bg-amber-50/85 backdrop-blur-md border border-amber-200/60 rounded-3xl shadow-sm">
                     <div className="flex items-start gap-4">
                        <div className="p-3 bg-amber-100/80 rounded-2xl text-amber-600 shrink-0">
                           <AlertTriangle className="h-6 w-6" />
                        </div>
                        <div>
                           <h4 className="text-base font-bold text-amber-900 leading-tight">
                              {t("recoveryTitle")}
                           </h4>
                           <p className="mt-1 text-sm font-medium text-amber-700/90 leading-normal">
                              {t("recoveryMessage", {
                                 name: item.propertyName,
                                 actual: item.actualCount,
                                 expected: item.expectedCount,
                              })}
                           </p>
                        </div>
                     </div>
                     <div className="flex items-center gap-3 shrink-0 self-end md:self-center">
                        <button
                           onClick={() => dismissMutation.mutate(item.propertyId)}
                           disabled={dismissMutation.isPending}
                           className="flex items-center justify-center gap-2 px-5 py-3 rounded-2xl text-sm font-bold text-amber-700 hover:bg-amber-100/50 disabled:opacity-50 transition active:scale-[0.98]"
                        >
                           {dismissMutation.isPending ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                           ) : (
                              <X className="h-4 w-4" />
                           )}
                           {t("dismiss")}
                        </button>
                        <Link
                           href={`/properties/${item.propertyId}`}
                           className="flex items-center justify-center gap-2 px-5 py-3 rounded-2xl bg-amber-600 text-white text-sm font-bold shadow-lg shadow-amber-200 hover:bg-amber-700 hover:shadow-amber-300 transition active:scale-[0.98]"
                        >
                           <Play className="h-4 w-4 fill-white" />
                           {t("resume")}
                        </Link>
                     </div>
                  </div>
               </motion.div>
            ))}
         </AnimatePresence>
      </div>
   );
}
