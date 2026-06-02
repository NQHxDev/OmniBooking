import CreatePropertyForm from "@/components/partner/CreatePropertyForm";
import { getTranslations } from "next-intl/server";

export async function generateMetadata() {
   const t = await getTranslations("Partner.createProperty");
   return {
      title: t("metaTitle"),
      description: t("metaDesc"),
   };
}

export default async function NewPropertyPage() {
   return (
      <main className="min-h-screen bg-[#f5f5f5]">
         <CreatePropertyForm />
      </main>
   );
}
