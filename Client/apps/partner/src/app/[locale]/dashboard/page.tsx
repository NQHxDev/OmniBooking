import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { Plus } from "lucide-react";
import Link from "next/link";
import DashboardSidebar from "@/components/partner/DashboardSidebar";
import DashboardStats from "@/components/partner/DashboardStats";
import PropertyTable from "@/components/partner/PropertyTable";
import DashboardHeader from "@/components/partner/DashboardHeader";
import { propertyService } from "@/lib/api/propertyService";
import { partnerService } from "@/lib/api/services/partnerService";
import { getTranslations } from "next-intl/server";
import { headers } from "next/headers";
import { getWebUrl, getPartnerUrl } from "@omnibooking/shared";

export default async function PartnerDashboard() {
   const cookieStore = await cookies();
   const t = await getTranslations("Partner.dashboard");

   const headersList = await headers();
   const host = headersList.get("host") || "";
   const protocol = headersList.get("x-forwarded-proto") || "http";
   const currentUrl = `${protocol}://${host}`;
   const webUrl = getWebUrl(currentUrl);
   const partnerUrl = getPartnerUrl(currentUrl);

   if (!cookieStore.get("session_id") && !cookieStore.get("refresh_token")) {
      redirect(`${webUrl}/auth/login?callbackUrl=${partnerUrl}/dashboard`);
   }

   // Adhering to the principle of using services for all API calls
   const allCookies = cookieStore.getAll();
   const cookieHeader = allCookies.map((c) => `${c.name}=${c.value}`).join("; ");
   const fingerprint = cookieStore.get("x_fgp")?.value;

   const [properties, stats] = await Promise.all([
      propertyService.getMyPropertiesServer(cookieHeader, fingerprint),
      partnerService.getStatsServer(cookieHeader, fingerprint),
   ]);

   if (properties === null) {
      redirect(`${webUrl}/auth/login?callbackUrl=${partnerUrl}/dashboard`);
   }
   return (
      <div className="min-h-screen bg-zinc-50/50 font-sans">
         <DashboardSidebar />

         <main className="lg:pl-64">
            <DashboardHeader />

            <div className="p-8">
               {/* Welcome & Action */}
               <div className="mb-10 flex flex-col justify-between gap-4 md:flex-row md:items-end">
                  <div>
                     <h1 className="text-3xl font-bold tracking-normal text-zinc-900">
                        {t("title")}
                     </h1>
                     <p className="mt-1 text-zinc-500 font-medium">{t("welcome")}</p>
                  </div>
                  <Link
                     href="/properties/new"
                     className="flex items-center justify-center gap-2 rounded-2xl bg-[#006ce4] px-6 py-3.5 text-sm font-bold text-white shadow-xl shadow-blue-100 hover:bg-[#0057b7] hover:shadow-blue-200 active:scale-[0.98] transition-all"
                  >
                     <Plus className="h-5 w-5" />
                     {t("addNew")}
                  </Link>
               </div>

               {/* Stats Grid */}
               <DashboardStats stats={stats} />

               {/* Properties Table Section */}
               <div className="mt-12">
                  <div className="mb-6 flex items-center justify-between px-2">
                     <div>
                        <h2 className="text-xl font-bold text-zinc-900">{t("properties.title")}</h2>
                        <p className="text-sm text-zinc-500 mt-1">{t("properties.subtitle")}</p>
                     </div>
                     <Link
                        href="/properties"
                        className="text-sm font-bold text-[#006ce4] hover:underline"
                     >
                        {t("properties.viewAll")}
                     </Link>
                  </div>

                  <PropertyTable properties={properties} />
               </div>
            </div>
         </main>
      </div>
   );
}
