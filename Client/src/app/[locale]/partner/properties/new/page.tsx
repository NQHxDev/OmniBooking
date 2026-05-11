import CreatePropertyForm from "@/components/partner/CreatePropertyForm";
import { Building2 } from "lucide-react";
import { getTranslations } from "next-intl/server";

export async function generateMetadata() {
   const t = await getTranslations("Partner.createProperty");
   return {
      title: t("metaTitle"),
      description: t("metaDesc"),
   };
}

export default async function NewPropertyPage() {
   const t = await getTranslations("Partner.createProperty");

   return (
      <main className="min-h-screen bg-[#f5f5f5] pb-20">
         {/* Page Header (Server Rendered) */}
         <div className="bg-white border-b border-zinc-200 mb-12">
            <div className="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8">
               <div className="flex flex-col items-center text-center">
                  <div className="h-16 w-16 bg-blue-50 text-[#003580] rounded-2xl flex items-center justify-center mb-6 shadow-sm border border-blue-100">
                     <Building2 className="h-8 w-8" />
                  </div>
                  <h1 className="text-3xl md:text-4xl font-extrabold text-zinc-900 tracking-tight mb-4">
                     {t("title")}
                  </h1>
                  <p className="max-w-xl text-zinc-500 text-lg leading-relaxed">{t("subtitle")}</p>
               </div>
            </div>
         </div>

         {/* Form Section (Client Interactivity) */}
         <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <CreatePropertyForm />
         </div>
      </main>
   );
}
