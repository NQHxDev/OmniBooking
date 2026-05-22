import { getTranslations } from "next-intl/server";
import { propertyService, PropertyResponse } from "@/services/propertyService";
import PropertyCarouselClient from "./PropertyCarouselClient";

export default async function NewProperties() {
   const t = await getTranslations("Home");
   let properties: PropertyResponse[] = [];

   try {
      properties = await propertyService.getNew(15);
   } catch (error) {
      console.error("Failed to fetch new properties", error);
   }

   if (!properties || properties.length === 0) {
      return (
         <section className="mt-16 bg-zinc-50 rounded-[2rem] p-12 text-center border border-zinc-100">
            <div className="max-w-md mx-auto">
               <h3 className="text-xl font-bold text-black mb-2">
                  {t("newProperties") || "Chỗ nghỉ mới đăng"}
               </h3>
               <p className="text-sm text-zinc-500">
                  {t("noNewProperties") || "Hiện chưa có chỗ nghỉ mới nào!"}
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
                  {t("newProperties") || "Chỗ nghỉ mới đăng"}
               </h3>
               <p className="mt-1 text-zinc-500">
                  {t("newSub") || "Những chỗ nghỉ vừa gia nhập hệ thống của chúng tôi"}
               </p>
            </div>
         </div>

         <PropertyCarouselClient properties={properties} />
      </section>
   );
}
