import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import DashboardSidebar from "@/components/partner/DashboardSidebar";
import DashboardHeader from "@/components/partner/DashboardHeader";
import { propertyService } from "@/lib/api/propertyService";

// Import các subcomponents chi tiết chỗ nghỉ
import PropertyHeader from "@/components/partner/details/PropertyHeader";
import PropertyStats from "@/components/partner/details/PropertyStats";
import PropertyInfoCard from "@/components/partner/details/PropertyInfoCard";
import PropertyRoomsTable from "@/components/partner/details/PropertyRoomsTable";
import PropertyLegalCard from "@/components/partner/details/PropertyLegalCard";

export const metadata = {
   title: "Chi tiết chỗ nghỉ | OmniBooking Partner",
   // Private dashboard page, do not index
   robots: {
      index: false,
      follow: false,
   },
};

interface PartnerPropertyDetailPageProps {
   params: Promise<{
      id: string;
      locale: string;
   }>;
}

export default async function PartnerPropertyDetailPage({
   params,
}: PartnerPropertyDetailPageProps) {
   const { id } = await params;
   const cookieStore = await cookies();

   // Đẩy về login nếu chưa có session
   if (!cookieStore.get("session_id") && !cookieStore.get("refresh_token")) {
      redirect(`/auth/login?callbackUrl=/partner/properties/${id}`);
   }

   const allCookies = cookieStore.getAll();
   const cookieHeader = allCookies.map((c) => `${c.name}=${c.value}`).join("; ");
   const fingerprint = cookieStore.get("x_fgp")?.value;

   // Lấy thông tin chi tiết của chỗ nghỉ từ Server Side
   const property = await propertyService.getPropertyDetailServer(id, cookieHeader, fingerprint);

   if (!property) {
      // Nếu không tìm thấy hoặc bị Forbidden
      redirect("/partner/dashboard");
   }

   return (
      <div className="min-h-screen bg-zinc-50/50 font-sans">
         <DashboardSidebar />

         <main className="lg:pl-64">
            <DashboardHeader />

            <div className="p-8">
               {/* 1. Header component */}
               <PropertyHeader property={property} />

               {/* 2. Stats/KPIs component */}
               <PropertyStats />

               {/* 3. Basic Info Card (Images, Description, Location, Amenities) */}
               <PropertyInfoCard property={property} />

               {/* 4. Room Configuration Table */}
               <PropertyRoomsTable property={property} />

               {/* 5. Legal Compliance Card */}
               <PropertyLegalCard property={property} />
            </div>
         </main>
      </div>
   );
}
