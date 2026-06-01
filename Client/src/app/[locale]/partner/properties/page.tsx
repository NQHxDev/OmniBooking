import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { Plus } from "lucide-react";
import Link from "next/link";
import DashboardSidebar from "@/components/partner/DashboardSidebar";
import DashboardHeader from "@/components/partner/DashboardHeader";
import PropertyTable from "@/components/partner/PropertyTable";
import { propertyService } from "@/lib/api/propertyService";
import { getTranslations } from "next-intl/server";

export const metadata = {
   title: "Danh sách chỗ nghỉ | OmniBooking Partner",
   // Private page, do not index
   robots: {
      index: false,
      follow: false,
   },
};

export default async function PartnerPropertiesPage() {
   const cookieStore = await cookies();
   const t = await getTranslations("Partner.dashboard");

   if (!cookieStore.get("session_id") && !cookieStore.get("refresh_token")) {
      redirect("/auth/login?callbackUrl=/partner/properties");
   }

   const allCookies = cookieStore.getAll();
   const cookieHeader = allCookies.map((c) => `${c.name}=${c.value}`).join("; ");
   const fingerprint = cookieStore.get("x_fgp")?.value;

   const properties = await propertyService.getMyPropertiesServer(cookieHeader, fingerprint);

   if (properties === null) {
      redirect("/auth/login?callbackUrl=/partner/properties");
   }

   return (
      <div className="min-h-screen bg-zinc-50/50 font-sans">
         <DashboardSidebar />

         <main className="lg:pl-64">
            <DashboardHeader />

            <div className="p-8">
               {/* Header Section */}
               <div className="mb-10 flex flex-col justify-between gap-4 md:flex-row md:items-end">
                  <div>
                     <h1 className="text-3xl font-bold tracking-normal text-zinc-900">
                        {t("properties.title")}
                     </h1>
                     <p className="mt-1 text-zinc-500 font-medium">{t("properties.subtitle")}</p>
                  </div>
                  <Link
                     href="/partner/properties/new"
                     className="flex items-center justify-center gap-2 rounded-2xl bg-[#006ce4] px-6 py-3.5 text-sm font-bold text-white shadow-xl shadow-blue-100 hover:bg-[#0057b7] hover:shadow-blue-200 active:scale-[0.98] transition-all"
                  >
                     <Plus className="h-5 w-5" />
                     {t("addNew")}
                  </Link>
               </div>

               {/* Properties Table Card */}
               <div className="rounded-[2rem] bg-white p-6 shadow-sm border border-zinc-100">
                  <PropertyTable properties={properties} />
               </div>
            </div>
         </main>
      </div>
   );
}
