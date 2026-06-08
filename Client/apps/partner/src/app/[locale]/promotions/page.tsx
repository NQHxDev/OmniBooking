import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import DashboardSidebar from "@/components/partner/DashboardSidebar";
import DashboardHeader from "@/components/partner/DashboardHeader";
import { propertyService } from "@/lib/api/propertyService";
import { headers } from "next/headers";
import { getWebUrl, getPartnerUrl } from "@omnibooking/shared";
import PartnerPromotions from "@/components/partner/PartnerPromotions";
import { getTranslations } from "next-intl/server";

export async function generateMetadata() {
   const t = await getTranslations("Partner.promotions");
   return {
      title: t("metaTitle"),
      description: t("metaDesc"),
   };
}

export default async function PartnerPromotionsPage() {
   const cookieStore = await cookies();
   const t = await getTranslations("Partner.promotions");

   const headersList = await headers();
   const host = headersList.get("host") || "";
   const protocol = headersList.get("x-forwarded-proto") || "http";
   const currentUrl = `${protocol}://${host}`;
   const webUrl = getWebUrl(currentUrl);
   const partnerUrl = getPartnerUrl(currentUrl);

   // Verification of active session
   if (!cookieStore.get("session_id") && !cookieStore.get("refresh_token")) {
      redirect(`${webUrl}/auth/login?callbackUrl=${partnerUrl}/promotions`);
   }

   const allCookies = cookieStore.getAll();
   const cookieHeader = allCookies.map((c) => `${c.name}=${c.value}`).join("; ");
   const fingerprint = cookieStore.get("x_fgp")?.value;

   // Fetch properties for dropdown filters
   const properties = await propertyService.getMyPropertiesServer(cookieHeader, fingerprint);

   if (properties === null) {
      redirect(`${webUrl}/auth/login?callbackUrl=${partnerUrl}/promotions`);
   }

   return (
      <div className="min-h-screen bg-zinc-50/50 font-sans">
         <DashboardSidebar />

         <main className="lg:pl-64">
            <DashboardHeader />

            <div className="p-8">
               {/* Welcome header & description */}
               <div className="mb-10">
                  <h1 className="text-3xl font-bold tracking-normal text-zinc-900">
                     {t("pageTitle")}
                  </h1>
                  <p className="mt-1 text-zinc-500 font-medium">{t("pageDesc")}</p>
               </div>

               {/* Render the promotions manager */}
               <PartnerPromotions initialProperties={properties} />
            </div>
         </main>
      </div>
   );
}
