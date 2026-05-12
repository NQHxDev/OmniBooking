"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { useTranslations } from "next-intl";
import { propertyService, PropertyResponse } from "@/services/propertyService";
import { Star, MapPin, Loader2 } from "lucide-react";
import { motion } from "framer-motion";

export default function FeaturedProperties() {
   const t = useTranslations("Home");
   const [properties, setProperties] = useState<PropertyResponse[]>([]);
   const [loading, setLoading] = useState(true);

   useEffect(() => {
      const fetchProperties = async () => {
         try {
            const data = await propertyService.getFeatured(4);
            if (data && data.length > 0) {
               setProperties(data);
            }
         } catch (error) {
            console.error("Failed to fetch featured properties", error);
         } finally {
            setLoading(false);
         }
      };

      fetchProperties();
   }, []);

   if (loading) {
      return (
         <div className="mt-16 flex justify-center py-20">
            <Loader2 className="h-10 w-10 text-[#006ce4] animate-spin" />
         </div>
      );
   }

   if (properties.length === 0) {
      return (
         <section className="mt-16 bg-zinc-50 rounded-[2rem] p-12 text-center border border-zinc-100">
            <div className="max-w-md mx-auto">
               <h3 className="text-xl font-bold text-black mb-2">
                  {t("featuredProperties") || "Chỗ nghỉ nổi bật"}
               </h3>
               <p className="text-sm text-zinc-500">
                  {t("noFeaturedProperties") ||
                     "Hiện chưa có chỗ nghỉ nổi bật nào tại khu vực này!"}
               </p>
            </div>
         </section>
      );
   }

   return (
      <section className="mt-16">
         <div className="flex items-center justify-between">
            <div>
               <h3 className="text-2xl font-bold text-black">
                  {t("featuredProperties") || "Chỗ nghỉ nổi bật"}
               </h3>
               <p className="mt-1 text-zinc-500">
                  {t("featuredSub") ||
                     "Khám phá những lựa chọn tuyệt vời nhất cho chuyến đi của bạn"}
               </p>
            </div>
         </div>

         <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {properties.map((property, index) => (
               <motion.div
                  key={property.id}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.5, delay: index * 0.1 }}
                  viewport={{ once: true }}
                  className="group cursor-pointer bg-white rounded-2xl overflow-hidden border border-zinc-100 hover:shadow-2xl transition-all duration-300 hover:-translate-y-1"
               >
                  <div className="relative h-64 w-full">
                     <Image
                        src={
                           property.imageUrl ||
                           "https://images.unsplash.com/photo-1566073771259-6a8506099945?q=80&w=2070&auto=format&fit=crop"
                        }
                        alt={property.name}
                        fill
                        className="object-cover group-hover:scale-110 transition-transform duration-700"
                     />
                     <div className="absolute top-4 right-4 bg-white/90 backdrop-blur-sm px-3 py-1 rounded-full flex items-center gap-1.5 shadow-sm">
                        <Star className="h-3.5 w-3.5 text-yellow-500 fill-yellow-500" />
                        <span className="text-xs font-bold text-black">4.9</span>
                     </div>
                  </div>
                  <div className="p-5">
                     <div className="flex items-center gap-1 text-zinc-400 mb-2">
                        <MapPin className="h-3 w-3" />
                        <span className="text-[11px] font-bold uppercase tracking-wider">
                           {property.city}, {property.country}
                        </span>
                     </div>
                     <h4 className="text-base font-bold text-black group-hover:text-[#006ce4] transition-colors line-clamp-1">
                        {property.name}
                     </h4>
                     <p className="mt-1 text-xs text-zinc-500 font-medium">
                        {property.propertyType}
                     </p>

                     <div className="mt-4 pt-4 border-t border-zinc-50 flex items-center justify-between">
                        <div>
                           <span className="text-[10px] text-zinc-400 font-bold uppercase block">
                              Giá từ
                           </span>
                           <span className="text-lg font-black text-[#006ce4]">1.250.000₫</span>
                        </div>
                        <div className="bg-[#006ce4] text-white px-4 py-2 rounded-xl text-xs font-bold group-hover:bg-[#0057b7] transition-colors">
                           Đặt ngay
                        </div>
                     </div>
                  </div>
               </motion.div>
            ))}
         </div>
      </section>
   );
}
