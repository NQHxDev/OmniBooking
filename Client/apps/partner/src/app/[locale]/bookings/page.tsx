import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import DashboardSidebar from "@/components/partner/DashboardSidebar";
import DashboardHeader from "@/components/partner/DashboardHeader";
import { partnerService } from "@/lib/api/services/partnerService";
import { headers } from "next/headers";
import { getWebUrl, getPartnerUrl } from "@omnibooking/shared";

export const metadata = {
   title: "Quản lý đặt phòng | OmniBooking Partner",
   description:
      "Theo dõi và quản lý các đơn đặt phòng của chỗ nghỉ tại hệ sinh thái đối tác OmniBooking.",
};

export default async function PartnerBookingsPage() {
   const cookieStore = await cookies();

   const headersList = await headers();
   const host = headersList.get("host") || "";
   const protocol = headersList.get("x-forwarded-proto") || "http";
   const currentUrl = `${protocol}://${host}`;
   const webUrl = getWebUrl(currentUrl);
   const partnerUrl = getPartnerUrl(currentUrl);

   // Verification of active session
   if (!cookieStore.get("session_id") && !cookieStore.get("refresh_token")) {
      redirect(`${webUrl}/auth/login?callbackUrl=${partnerUrl}/bookings`);
   }

   const allCookies = cookieStore.getAll();
   const cookieHeader = allCookies.map((c) => `${c.name}=${c.value}`).join("; ");
   const fingerprint = cookieStore.get("x_fgp")?.value;

   // Server-side fetching of partner bookings
   const bookings = await partnerService.getBookingsServer(cookieHeader, fingerprint);

   if (bookings === null) {
      redirect(`${webUrl}/auth/login?callbackUrl=${partnerUrl}/bookings`);
   }

   // Import client component dynamically or directly as it is standard in Next.js
   const PartnerBookingList = (await import("@/components/partner/PartnerBookingList")).default;

   return (
      <div className="min-h-screen bg-zinc-50/50 font-sans">
         <DashboardSidebar />

         <main className="lg:pl-64">
            <DashboardHeader />

            <div className="p-8">
               {/* Welcome header & description */}
               <div className="mb-10">
                  <h1 className="text-3xl font-bold tracking-normal text-zinc-900">
                     Quản lý đặt phòng
                  </h1>
                  <p className="mt-1 text-zinc-500 font-medium">
                     Theo dõi và quản lý toàn bộ các lượt đặt phòng của khách tại chỗ nghỉ của bạn
                  </p>
               </div>

               {/* Render the clean bookings list */}
               <PartnerBookingList initialBookings={bookings} />
            </div>
         </main>
      </div>
   );
}
