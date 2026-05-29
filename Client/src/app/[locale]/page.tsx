import Navbar from "@/components/Navbar";
import GeniusBanner from "@/components/GeniusBanner";
import SearchBar from "@/components/SearchBar";
import FeaturedProperties from "@/components/FeaturedProperties";
import NewProperties from "@/components/NewProperties";
import Image from "next/image";
import { getTranslations } from "next-intl/server";
import { headers } from "next/headers";
import DiscoverDestinations from "@/components/DiscoverDestinations";
import PopularDestinationsLinks from "@/components/PopularDestinationsLinks";
import Footer from "@/components/Footer";

export default async function Home({ params }: { params: Promise<{ locale: string }> }) {
   const { locale } = await params;
   const t = await getTranslations({ locale, namespace: "Common" });
   const tHome = await getTranslations({ locale, namespace: "Home" });

   const headerList = await headers();
   const clientIp = headerList.get("x-forwarded-for")?.split(",")[0].trim() || "127.0.0.1";

   return (
      <div className="flex min-h-screen flex-col bg-white">
         <Navbar />

         {/* Hero Section */}
         <section className="bg-[#003580] pt-16 pb-28 text-white">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
               <h2 className="text-5xl font-bold leading-tight">
                  {t("heroTitle") || "Tìm chỗ nghỉ tiếp theo"}
               </h2>
               <p className="mt-4 text-2xl text-zinc-100">
                  {t("heroSub") || "Tìm ưu đãi khách sạn, chỗ nghỉ dạng nhà và nhiều hơn nữa..."}
               </p>
            </div>
         </section>

         {/* Search Bar Component */}
         <SearchBar />

         {/* Main Content */}
         <main className="mx-auto w-full max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
            {/* Promo Section */}
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
               <div className="relative h-64 overflow-hidden rounded-lg bg-zinc-900 group cursor-pointer shadow-md">
                  <Image
                     src="/images/hero_banner.png"
                     alt="Promo 1"
                     fill
                     sizes="(max-width: 768px) 100vw, 50vw"
                     className="object-cover opacity-80 group-hover:scale-105 transition-transform duration-500"
                     priority
                  />
                  <div className="absolute inset-0 bg-linear-to-t from-black/60 to-transparent" />
                  <div className="absolute bottom-6 left-6 text-white">
                     <h3 className="text-2xl font-bold">
                        {tHome("promoSummerTitle") || "Giảm giá cho kỳ nghỉ hè"}
                     </h3>
                     <p className="mt-2 text-sm">
                        {tHome("promoSummerDesc") ||
                           "Tiết kiệm 15% hoặc hơn khi đặt từ nay đến 30/9/2026"}
                     </p>
                     <button className="mt-4 rounded bg-[#006ce4] px-4 py-2 text-sm font-bold hover:bg-[#0057b7] transition-colors">
                        {tHome("promoSummerBtn") || "Tìm ưu đãi"}
                     </button>
                  </div>
               </div>
               <div className="relative h-64 overflow-hidden rounded-lg bg-zinc-900 group cursor-pointer shadow-md">
                  <Image
                     src="/images/hanoi.png"
                     alt="Promo 2"
                     fill
                     sizes="(max-width: 768px) 100vw, 50vw"
                     className="object-cover opacity-80 group-hover:scale-105 transition-transform duration-500"
                  />
                  <div className="absolute inset-0 bg-linear-to-t from-black/60 to-transparent" />
                  <div className="absolute bottom-6 left-6 text-white">
                     <h3 className="text-2xl font-bold">
                        {tHome("promoHanoiTitle") || "Khám phá Hà Nội"}
                     </h3>
                     <p className="mt-2 text-sm">
                        {tHome("promoHanoiDesc") ||
                           "Trải nghiệm nét cổ kính và ẩm thực đường phố tuyệt vời"}
                     </p>
                  </div>
               </div>
            </div>

            {/* Dynamic Discover Destinations based on client IP */}
            <DiscoverDestinations locale={locale} clientIp={clientIp} />

            {/* Featured Properties from DB */}
            <FeaturedProperties />

            {/* New Properties from DB */}
            <NewProperties />

            {/* Loyalty/Genius Section */}
            <GeniusBanner />

            {/* Popular Destinations and SEO links */}
            <PopularDestinationsLinks locale={locale} clientIp={clientIp} />
         </main>

         {/* Redesigned Footer */}
         <Footer />
      </div>
   );
}
