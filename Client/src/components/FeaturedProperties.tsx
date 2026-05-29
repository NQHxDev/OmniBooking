import { getTranslations } from "next-intl/server";
import { propertyService, PropertyResponse } from "@/services/propertyService";
import PropertyCarouselClient from "./PropertyCarouselClient";

export default async function FeaturedProperties() {
   const t = await getTranslations("Home");
   let properties: PropertyResponse[] = [];

   try {
      properties = await propertyService.getFeatured(15);
   } catch (error) {
      console.error("Failed to fetch featured properties", error);
   }

   if (!properties || properties.length === 0) {
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
      <section className="mt-6">
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

         <PropertyCarouselClient properties={properties} />
      </section>
   );
}
