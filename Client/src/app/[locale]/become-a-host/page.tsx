import PartnerNavbar from "@/components/PartnerNavbar";
import HostCTASection from "@/components/become-a-host/HostCTASection";
import PropertyTypes from "@/components/become-a-host/PropertyTypes";
import PaymentFeatures from "@/components/become-a-host/PaymentFeatures";
import WhyJoinUs from "@/components/become-a-host/WhyJoinUs";
import StepByStep from "@/components/become-a-host/StepByStep";
import HostFooter from "@/components/become-a-host/HostFooter";

export default function BecomeAHostPage() {
   return (
      <div className="min-h-screen bg-white font-sans selection:bg-blue-100 selection:text-blue-900">
         <PartnerNavbar />

         {/* Hero Section: 60/40 Split */}
         <section className="bg-[#003580] text-white">
            <div className="mx-auto max-w-[1100px] px-4 py-16 sm:px-6 lg:px-8">
               <div className="grid grid-cols-1 gap-12 lg:grid-cols-12 items-center">
                  {/* Left Column: Headline */}
                  <div className="lg:col-span-7">
                     <h1 className="text-3xl font-black leading-[1.4] tracking-tighter sm:text-4xl">
                        Đăng ký <span className="text-blue-400">chỗ nghỉ</span> của Quý vị trên
                        OmniBooking.com
                     </h1>
                     <p className="mt-8 text-lg text-blue-100 leading-relaxed max-w-2xl">
                        Quý vị có một căn hộ cho thuê hay một khách sạn sang trọng, chúng tôi luôn
                        có những công cụ tốt nhất để giúp Quý vị kinh doanh thành công.
                     </p>
                  </div>

                  {/* Right Column: CTA Card (Client Component) */}
                  <HostCTASection />
               </div>
            </div>
         </section>

         {/* Sub-components (Server Components) */}
         <PropertyTypes />
         <WhyJoinUs />
         <PaymentFeatures />
         <StepByStep />
         <HostFooter />
      </div>
   );
}
