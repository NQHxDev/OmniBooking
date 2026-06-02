import Image from "next/image";
import Link from "next/link";
import {
   Star,
   MapPin,
   Check,
   Info,
   Shield,
   Coffee,
   Wifi,
   Clock,
   Heart,
   Sparkles,
   MessageSquare,
   Compass,
   Utensils,
   Waves,
   ChevronRight,
   Car,
   Bus,
   Dumbbell,
   Bell,
   ArrowUpDown,
   Users,
   PawPrint,
   Wind,
   Tv,
   Sunset,
   Wine,
   Lock,
   Laptop,
   Shirt,
   Bath,
   Droplet,
   Refrigerator,
   Microwave,
   Table,
   LucideIcon,
} from "lucide-react";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { propertyService, PropertyDetailResponse } from "@/services/propertyService";
import { getTranslations } from "next-intl/server";
import RoomPricingSection from "@/components/RoomPricingSection";

const AMENITY_CONFIGS: Record<
   string,
   { translationKey: string; icon: LucideIcon; iconColor: string }
> = {
   // General
   "free wi-fi": { translationKey: "freeWifi", icon: Wifi, iconColor: "#006ce4" },
   "swimming pool": { translationKey: "swimmingPool", icon: Waves, iconColor: "#006ce4" },
   parking: { translationKey: "parking", icon: Car, iconColor: "#006ce4" },
   "airport shuttle": { translationKey: "airportShuttle", icon: Bus, iconColor: "#006ce4" },
   "gym / fitness center": { translationKey: "gym", icon: Dumbbell, iconColor: "#006ce4" },
   "spa & wellness center": { translationKey: "spa", icon: Sparkles, iconColor: "#006ce4" },
   "24-hour front desk": { translationKey: "frontDesk24h", icon: Bell, iconColor: "#006ce4" },
   elevator: { translationKey: "elevator", icon: ArrowUpDown, iconColor: "#006ce4" },
   "family rooms": { translationKey: "familyRooms", icon: Users, iconColor: "#006ce4" },
   "pet friendly": { translationKey: "petFriendly", icon: PawPrint, iconColor: "#006ce4" },

   // Room
   "air conditioning": { translationKey: "airConditioning", icon: Wind, iconColor: "#006ce4" },
   "flat-screen tv": { translationKey: "flatScreenTv", icon: Tv, iconColor: "#006ce4" },
   balcony: { translationKey: "balcony", icon: Sunset, iconColor: "#006ce4" },
   minibar: { translationKey: "minibar", icon: Wine, iconColor: "#006ce4" },
   safe: { translationKey: "safe", icon: Lock, iconColor: "#006ce4" },
   "work desk": { translationKey: "workDesk", icon: Laptop, iconColor: "#006ce4" },
   "ironing facilities": { translationKey: "ironingFacilities", icon: Shirt, iconColor: "#006ce4" },

   // Bathroom
   "private bathroom": { translationKey: "privateBathroom", icon: Bath, iconColor: "#006ce4" },
   hairdryer: { translationKey: "hairdryer", icon: Wind, iconColor: "#006ce4" },
   "free toiletries": { translationKey: "freeToiletries", icon: Sparkles, iconColor: "#006ce4" },
   bathrobe: { translationKey: "bathrobe", icon: Shirt, iconColor: "#006ce4" },
   shower: { translationKey: "shower", icon: Droplet, iconColor: "#006ce4" },
   bathtub: { translationKey: "bathtub", icon: Bath, iconColor: "#006ce4" },

   // Kitchen
   refrigerator: { translationKey: "refrigerator", icon: Refrigerator, iconColor: "#006ce4" },
   microwave: { translationKey: "microwave", icon: Microwave, iconColor: "#006ce4" },
   "electric kettle": { translationKey: "electricKettle", icon: Coffee, iconColor: "#006ce4" },
   kitchenware: { translationKey: "kitchenware", icon: Utensils, iconColor: "#006ce4" },
   "dining table": { translationKey: "diningTable", icon: Table, iconColor: "#006ce4" },

   // Custom / Extras from screens
   restaurant: { translationKey: "restaurant", icon: Utensils, iconColor: "#006ce4" },
   bar: { translationKey: "bar", icon: Wine, iconColor: "#006ce4" },
   "room service": { translationKey: "roomService", icon: Bell, iconColor: "#006ce4" },
   breakfast: { translationKey: "breakfast", icon: Coffee, iconColor: "#006ce4" },
   "good breakfast": { translationKey: "breakfast", icon: Coffee, iconColor: "#006ce4" },
   "non-smoking rooms": { translationKey: "nonSmoking", icon: Wind, iconColor: "#006ce4" },
   "non-smoking room": { translationKey: "nonSmoking", icon: Wind, iconColor: "#006ce4" },
};

const getAmenityConfig = (amenityName: string) => {
   const lower = amenityName.toLowerCase().trim();
   if (AMENITY_CONFIGS[lower]) {
      return AMENITY_CONFIGS[lower];
   }

   // Fallback checks
   if (lower.includes("pool") || lower.includes("bể bơi") || lower.includes("hồ bơi")) {
      return { translationKey: "swimmingPool", icon: Waves, iconColor: "#006ce4" };
   }
   if (lower.includes("wifi") || lower.includes("wi-fi") || lower.includes("internet")) {
      return { translationKey: "freeWifi", icon: Wifi, iconColor: "#006ce4" };
   }
   if (lower.includes("spa") || lower.includes("massage") || lower.includes("trị liệu")) {
      return { translationKey: "spa", icon: Sparkles, iconColor: "#006ce4" };
   }
   if (lower.includes("nhà hàng") || lower.includes("restaurant") || lower.includes("ăn uống")) {
      return { translationKey: "restaurant", icon: Utensils, iconColor: "#006ce4" };
   }
   if (lower.includes("đưa đón") || lower.includes("shuttle") || lower.includes("transfer")) {
      return { translationKey: "airportShuttle", icon: Bus, iconColor: "#006ce4" };
   }
   if (lower.includes("đỗ xe") || lower.includes("bãi xe") || lower.includes("parking")) {
      return { translationKey: "parking", icon: Car, iconColor: "#006ce4" };
   }
   if (lower.includes("gym") || lower.includes("thể hình") || lower.includes("fitness")) {
      return { translationKey: "gym", icon: Dumbbell, iconColor: "#006ce4" };
   }
   if (lower.includes("bar") || lower.includes("rượu") || lower.includes("quầy nước")) {
      return { translationKey: "bar", icon: Wine, iconColor: "#006ce4" };
   }
   if (lower.includes("lễ tân") || lower.includes("reception") || lower.includes("front desk")) {
      return { translationKey: "frontDesk24h", icon: Bell, iconColor: "#006ce4" };
   }
   if (lower.includes("thú cưng") || lower.includes("pet")) {
      return { translationKey: "petFriendly", icon: PawPrint, iconColor: "#006ce4" };
   }
   if (lower.includes("điều hòa") || lower.includes("máy lạnh") || lower.includes("conditioning")) {
      return { translationKey: "airConditioning", icon: Wind, iconColor: "#006ce4" };
   }
   if (lower.includes("bữa sáng") || lower.includes("breakfast") || lower.includes("ăn sáng")) {
      return { translationKey: "breakfast", icon: Coffee, iconColor: "#006ce4" };
   }
   if (lower.includes("hút thuốc") || lower.includes("smoking")) {
      return { translationKey: "nonSmoking", icon: Wind, iconColor: "#006ce4" };
   }

   return {
      translationKey: "",
      icon: Check,
      iconColor: "#008009",
   };
};

interface PageProps {
   params: Promise<{
      locale: string;
      id: string;
   }>;
   searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}

export default async function PropertyDetailPage({ params, searchParams }: PageProps) {
   const { locale, id } = await params;
   await searchParams; // Await to satisfy Next.js dynamic APIs requirement
   const isVi = locale === "vi";

   let property: PropertyDetailResponse | null = null;
   try {
      property = await propertyService.getPropertyDetail(id);
   } catch (error) {
      console.error("Failed to fetch property details on server", error);
   }

   const t = await getTranslations({ locale, namespace: "PropertyDetail" });

   if (!property) {
      return (
         <div className="flex min-h-screen flex-col bg-zinc-50/50 text-zinc-900 font-sans selection:bg-[#006ce4] selection:text-white">
            <Navbar />
            <main className="mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8 py-20 flex-1 flex flex-col items-center justify-center text-center">
               <div className="max-w-md bg-white rounded-2xl p-8 border border-zinc-200 shadow-lg space-y-6">
                  <div className="mx-auto w-16 h-16 rounded-full bg-amber-50 flex items-center justify-center border border-amber-200">
                     <Info className="h-8 w-8 text-amber-600" />
                  </div>
                  <h1 className="text-2xl font-extrabold tracking-tight text-zinc-900">
                     {t("notFoundTitle")}
                  </h1>
                  <p className="text-sm text-zinc-500 leading-relaxed">{t("notFoundDesc")}</p>
                  <div className="pt-2">
                     <Link
                        href={`/${locale}`}
                        className="inline-flex items-center gap-2 bg-[#006ce4] hover:bg-[#0057b7] text-white px-6 py-3 rounded-xl font-bold text-sm shadow-md transition-all active:scale-[0.98]"
                     >
                        {isVi ? "Quay lại trang chủ" : "Back to Home"}
                     </Link>
                  </div>
               </div>
            </main>
            <Footer />
         </div>
      );
   }

   const finalProperty = property;

   const starRating = finalProperty.starRating || 5;
   const displayRating = 9.4; // Premium fixed rating for mock presentation
   const displayReviewsCount = 384;

   const rawImageUrls = finalProperty.imageUrls || [];
   const uniqueRealImages = finalProperty.imageUrl
      ? [finalProperty.imageUrl, ...rawImageUrls.filter((url) => url !== finalProperty.imageUrl)]
      : rawImageUrls;

   const displayImages = uniqueRealImages;

   const renderStars = (count: number) => {
      return Array.from({ length: count }).map((_, i) => (
         <Star key={i} className="h-4.5 w-4.5 fill-[#ffb700] text-[#ffb700] shrink-0" />
      ));
   };

   const getRatingText = (rating: number) => {
      if (rating >= 9) return t("exceptional");
      if (rating >= 8) return t("excellent");
      if (rating >= 7) return t("good");
      return t("pleasant");
   };

   // Format Property Type representation
   const formatPropertyType = (type: string) => {
      if (!type) return "";
      const lower = type.toLowerCase();
      if (lower === "hotel") return isVi ? "Khách sạn" : "Hotel";
      if (lower === "apartment") return isVi ? "Căn hộ" : "Apartment";
      if (lower === "resort") return isVi ? "Khu nghỉ dưỡng" : "Resort";
      if (lower === "villa") return isVi ? "Biệt thự" : "Villa";
      return type.charAt(0).toUpperCase() + lower.slice(1);
   };

   return (
      <div className="flex min-h-screen flex-col bg-zinc-50/50 text-zinc-900 font-sans selection:bg-[#006ce4] selection:text-white">
         <Navbar />

         <div className="mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8 py-6 flex-1">
            {/* Professional Hierarchy Breadcrumbs Navigation */}
            <nav
               className="mb-6 text-xs md:text-sm text-zinc-500 flex flex-wrap items-center gap-x-1.5 gap-y-1.5 font-semibold leading-none select-none"
               aria-label="Breadcrumb"
            >
               <Link
                  href={`/${locale}`}
                  className="text-[#006ce4] hover:underline font-medium transition-colors"
               >
                  {isVi ? "Trang chủ" : "Home"}
               </Link>
               <ChevronRight className="h-3.5 w-3.5 text-zinc-400 shrink-0" />

               <Link
                  href={`/${locale}/search?propertyType=${finalProperty.propertyType}`}
                  className="text-[#006ce4] hover:underline font-medium transition-colors"
               >
                  {formatPropertyType(finalProperty.propertyType)}
               </Link>
               <ChevronRight className="h-3.5 w-3.5 text-zinc-400 shrink-0" />

               <Link
                  href={`/${locale}/search?ss=${finalProperty.country}`}
                  className="text-[#006ce4] hover:underline font-medium transition-colors"
               >
                  {finalProperty.country}
               </Link>
               <ChevronRight className="h-3.5 w-3.5 text-zinc-400 shrink-0" />

               <Link
                  href={`/${locale}/search?ss=${finalProperty.city}`}
                  className="text-[#006ce4] hover:underline font-medium transition-colors"
               >
                  {isVi ? `Thành phố ${finalProperty.city}` : `${finalProperty.city} City`}
               </Link>
               <ChevronRight className="h-3.5 w-3.5 text-zinc-400 shrink-0" />

               <Link
                  href={`/${locale}/search?ss=${finalProperty.city}`}
                  className="text-[#006ce4] hover:underline font-medium transition-colors"
               >
                  {finalProperty.city}
               </Link>
               <ChevronRight className="h-3.5 w-3.5 text-zinc-400 shrink-0" />

               <span className="text-zinc-600 font-medium truncate max-w-[200px] sm:max-w-[300px] md:max-w-md">
                  {isVi
                     ? `Ưu đãi cho ${finalProperty.name} (${formatPropertyType(finalProperty.propertyType)}) (${finalProperty.country})`
                     : `Deals for ${finalProperty.name} (${formatPropertyType(finalProperty.propertyType)}) (${finalProperty.country})`}
               </span>
            </nav>

            {/* Header info */}
            <div className="flex flex-col md:flex-row md:items-start justify-between gap-6 mb-6">
               <div className="space-y-2">
                  <div className="flex flex-wrap items-center gap-3">
                     <span className="inline-block bg-[#003580] text-white text-[11px] font-extrabold px-2.5 py-0.5 rounded tracking-wide uppercase shadow-sm">
                        {formatPropertyType(finalProperty.propertyType)}
                     </span>
                     <div className="flex items-center gap-0.5">{renderStars(starRating)}</div>
                     <span className="inline-flex items-center gap-1 bg-[#ffb700]/25 border border-[#ffb700]/50 text-amber-900 text-[10px] font-extrabold px-2 py-0.5 rounded-full">
                        <Sparkles className="h-3.5 w-3.5 text-amber-700" />
                        <span>{t("featuredBadge")}</span>
                     </span>
                  </div>

                  <h1 className="text-3xl md:text-4xl font-extrabold text-zinc-900 tracking-tight leading-tight">
                     {finalProperty.name}
                  </h1>

                  <div className="flex items-center gap-1.5 text-sm text-zinc-600">
                     <MapPin className="h-4 w-4 text-[#006ce4] shrink-0" />
                     <span className="font-semibold text-zinc-800">
                        {finalProperty.address}, {finalProperty.city}, {finalProperty.country}
                     </span>
                     <span className="text-zinc-300 select-none">·</span>
                     <button className="text-[#006ce4] font-bold hover:underline cursor-pointer">
                        {t("map")}
                     </button>
                  </div>
               </div>

               {/* Guest Rating Box */}
               <div className="flex items-center gap-3 shrink-0 self-start md:self-auto bg-white p-3.5 rounded-xl border border-zinc-200 shadow-xs">
                  <div className="text-right">
                     <p className="text-base font-bold text-zinc-900 leading-tight">
                        {getRatingText(displayRating)}
                     </p>
                     <p className="text-xs text-zinc-500 font-semibold leading-none mt-1">
                        {displayReviewsCount} {t("reviews")}
                     </p>
                  </div>
                  <div className="bg-[#003580] text-white font-extrabold h-11 w-11 rounded-lg rounded-bl-none flex items-center justify-center text-lg shadow-sm shrink-0">
                     {displayRating.toFixed(1)}
                  </div>
               </div>
            </div>

            {/* Premium Photo Gallery */}
            <section className="mb-10">
               {displayImages.length === 0 ? (
                  <div className="w-full h-[260px] md:h-[430px] bg-zinc-100 rounded-2xl flex flex-col items-center justify-center border border-zinc-200 shadow-sm text-zinc-400 gap-3">
                     <div className="h-16 w-16 rounded-full bg-zinc-50 flex items-center justify-center border border-zinc-200">
                        <Compass className="h-8 w-8 text-zinc-400" />
                     </div>
                     <p className="text-sm font-semibold text-zinc-500">
                        {isVi
                           ? "Hình ảnh chỗ nghỉ đang được cập nhật"
                           : "Property images are being updated"}
                     </p>
                  </div>
               ) : displayImages.length === 1 ? (
                  <div className="w-full relative h-[260px] md:h-[430px] rounded-2xl overflow-hidden shadow-lg select-none group border border-zinc-200">
                     <Image
                        src={displayImages[0]}
                        alt={finalProperty.name}
                        fill
                        priority
                        sizes="100vw"
                        className="object-cover group-hover:scale-102 transition-transform duration-500"
                     />
                     <button className="absolute top-4 right-4 h-10 w-10 rounded-full bg-white/95 backdrop-blur-xs flex items-center justify-center shadow-md hover:bg-white hover:scale-105 active:scale-[0.95] transition-all cursor-pointer">
                        <Heart className="h-5 w-5 text-zinc-600 hover:text-red-500 transition-colors" />
                     </button>
                  </div>
               ) : displayImages.length === 2 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-2 md:gap-3 rounded-2xl overflow-hidden shadow-lg select-none">
                     <div className="relative h-[200px] md:h-[430px] overflow-hidden group border border-zinc-200">
                        <Image
                           src={displayImages[0]}
                           alt={finalProperty.name}
                           fill
                           priority
                           sizes="(max-width: 768px) 100vw, 50vw"
                           className="object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                        <button className="absolute top-4 right-4 h-10 w-10 rounded-full bg-white/95 backdrop-blur-xs flex items-center justify-center shadow-md hover:bg-white hover:scale-105 active:scale-[0.95] transition-all cursor-pointer">
                           <Heart className="h-5 w-5 text-zinc-600 hover:text-red-500 transition-colors" />
                        </button>
                     </div>
                     <div className="relative h-[200px] md:h-[430px] overflow-hidden group border border-zinc-200">
                        <Image
                           src={displayImages[1]}
                           alt={finalProperty.name}
                           fill
                           sizes="(max-width: 768px) 100vw, 50vw"
                           className="object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                     </div>
                  </div>
               ) : displayImages.length === 3 ? (
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-2 md:gap-3 rounded-2xl overflow-hidden shadow-lg select-none">
                     <div className="md:col-span-2 relative h-[260px] md:h-[430px] overflow-hidden group border border-zinc-200">
                        <Image
                           src={displayImages[0]}
                           alt={finalProperty.name}
                           fill
                           priority
                           sizes="(max-width: 768px) 100vw, 66vw"
                           className="object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                        <button className="absolute top-4 right-4 h-10 w-10 rounded-full bg-white/95 backdrop-blur-xs flex items-center justify-center shadow-md hover:bg-white hover:scale-105 active:scale-[0.95] transition-all cursor-pointer">
                           <Heart className="h-5 w-5 text-zinc-600 hover:text-red-500 transition-colors" />
                        </button>
                     </div>
                     <div className="hidden sm:flex flex-col gap-2 md:gap-3">
                        <div className="relative flex-1 h-[130px] md:h-[210px] overflow-hidden group border border-zinc-200">
                           <Image
                              src={displayImages[1]}
                              alt="Room details"
                              fill
                              sizes="(max-width: 768px) 50vw, 33vw"
                              className="object-cover group-hover:scale-105 transition-transform duration-500"
                           />
                        </div>
                        <div className="relative flex-1 h-[130px] md:h-[210px] overflow-hidden group border border-zinc-200">
                           <Image
                              src={displayImages[2]}
                              alt="Room details"
                              fill
                              sizes="(max-width: 768px) 50vw, 33vw"
                              className="object-cover group-hover:scale-105 transition-transform duration-500"
                           />
                        </div>
                     </div>
                  </div>
               ) : displayImages.length === 4 ? (
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-2 md:gap-3 rounded-2xl overflow-hidden shadow-lg select-none">
                     <div className="md:col-span-2 relative h-[260px] md:h-[430px] overflow-hidden group border border-zinc-200">
                        <Image
                           src={displayImages[0]}
                           alt={finalProperty.name}
                           fill
                           priority
                           sizes="(max-width: 768px) 100vw, 50vw"
                           className="object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                        <button className="absolute top-4 right-4 h-10 w-10 rounded-full bg-white/95 backdrop-blur-xs flex items-center justify-center shadow-md hover:bg-white hover:scale-105 active:scale-[0.95] transition-all cursor-pointer">
                           <Heart className="h-5 w-5 text-zinc-600 hover:text-red-500 transition-colors" />
                        </button>
                     </div>
                     <div className="hidden sm:flex flex-col gap-2 md:gap-3">
                        <div className="relative flex-1 h-[130px] md:h-[210px] overflow-hidden group border border-zinc-200">
                           <Image
                              src={displayImages[1]}
                              alt="Room details"
                              fill
                              sizes="(max-width: 768px) 50vw, 25vw"
                              className="object-cover group-hover:scale-105 transition-transform duration-500"
                           />
                        </div>
                        <div className="relative flex-1 h-[130px] md:h-[210px] overflow-hidden group border border-zinc-200">
                           <Image
                              src={displayImages[2]}
                              alt="Room details"
                              fill
                              sizes="(max-width: 768px) 50vw, 25vw"
                              className="object-cover group-hover:scale-105 transition-transform duration-500"
                           />
                        </div>
                     </div>
                     <div className="relative hidden sm:block h-[260px] md:h-[430px] overflow-hidden group border border-zinc-200">
                        <Image
                           src={displayImages[3]}
                           alt="Room details"
                           fill
                           sizes="(max-width: 768px) 50vw, 25vw"
                           className="object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                     </div>
                  </div>
               ) : (
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-2 md:gap-3 rounded-2xl overflow-hidden shadow-lg select-none">
                     {/* Main Large Image */}
                     <div className="md:col-span-2 relative h-[260px] md:h-[430px] overflow-hidden group border border-zinc-200">
                        <Image
                           src={displayImages[0]}
                           alt={finalProperty.name}
                           fill
                           priority
                           sizes="(max-width: 768px) 100vw, 50vw"
                           className="object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                        <button className="absolute top-4 right-4 h-10 w-10 rounded-full bg-white/95 backdrop-blur-xs flex items-center justify-center shadow-md hover:bg-white hover:scale-105 active:scale-[0.95] transition-all cursor-pointer">
                           <Heart className="h-5 w-5 text-zinc-600 hover:text-red-500 transition-colors" />
                        </button>
                     </div>
                     {/* Col 3: Two stacked */}
                     <div className="hidden sm:flex flex-col gap-2 md:gap-3">
                        <div className="relative flex-1 h-[130px] md:h-[210px] overflow-hidden group border border-zinc-200">
                           <Image
                              src={displayImages[1]}
                              alt="Room details"
                              fill
                              sizes="(max-width: 768px) 50vw, 25vw"
                              className="object-cover group-hover:scale-105 transition-transform duration-500"
                           />
                        </div>
                        <div className="relative flex-1 h-[130px] md:h-[210px] overflow-hidden group border border-zinc-200">
                           <Image
                              src={displayImages[2]}
                              alt="Spa & Leisure"
                              fill
                              sizes="(max-width: 768px) 50vw, 25vw"
                              className="object-cover group-hover:scale-105 transition-transform duration-500"
                           />
                        </div>
                     </div>
                     {/* Col 4: Two stacked */}
                     <div className="hidden sm:flex flex-col gap-2 md:gap-3">
                        <div className="relative flex-1 h-[130px] md:h-[210px] overflow-hidden group border border-zinc-200">
                           <Image
                              src={displayImages[3]}
                              alt="Pool scenery"
                              fill
                              sizes="(max-width: 768px) 50vw, 25vw"
                              className="object-cover group-hover:scale-105 transition-transform duration-500"
                           />
                        </div>
                        <div className="relative flex-1 h-[130px] md:h-[210px] overflow-hidden group border border-zinc-200">
                           <Image
                              src={displayImages[4]}
                              alt="Dining area"
                              fill
                              sizes="(max-width: 768px) 50vw, 25vw"
                              className="object-cover group-hover:scale-105 transition-transform duration-500"
                           />
                           {displayImages.length > 5 && (
                              <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-100 transition-opacity duration-300 pointer-events-none">
                                 <span className="text-white text-sm font-bold border border-white px-3 py-1.5 rounded-full">
                                    +{displayImages.length - 5} {isVi ? "ảnh" : "photos"}
                                 </span>
                              </div>
                           )}
                        </div>
                     </div>
                  </div>
               )}
            </section>

            {/* Content Layout */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
               {/* Left 2/3 - Main Details */}
               <div className="lg:col-span-2 space-y-8">
                  {/* About Description */}
                  <section className="bg-white rounded-2xl p-6 md:p-8 border border-zinc-200/80 shadow-xs">
                     <h2 className="text-xl font-bold text-zinc-900 mb-4 pb-2 border-b border-zinc-100 flex items-center gap-2">
                        <Compass className="h-5 w-5 text-[#006ce4]" />
                        <span>{t("about")}</span>
                     </h2>
                     <p className="text-zinc-700 leading-relaxed text-sm md:text-base text-justify whitespace-pre-line">
                        {finalProperty.description}
                     </p>
                  </section>

                  {/* Amenities */}
                  {finalProperty.amenities && finalProperty.amenities.length > 0 && (
                     <section className="bg-white rounded-2xl p-6 md:p-8 border border-zinc-200/80 shadow-xs">
                        <h2 className="text-xl font-bold text-zinc-900 mb-5 pb-2 border-b border-zinc-100 flex items-center gap-2">
                           <Sparkles className="h-5 w-5 text-[#ffb700]" />
                           <span>{t("amenities")}</span>
                        </h2>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                           {finalProperty.amenities.map((amenity, index) => {
                              const config = getAmenityConfig(amenity);
                              const IconComponent = config.icon;
                              const translatedText = config.translationKey
                                 ? t(`amenitiesList.${config.translationKey}`)
                                 : amenity;
                              return (
                                 <div
                                    key={index}
                                    className="flex items-center gap-3 text-sm text-zinc-700 hover:text-zinc-900 transition-colors"
                                 >
                                    <div className="h-8 w-8 rounded-full bg-zinc-50 flex items-center justify-center shrink-0 border border-zinc-100">
                                       <IconComponent
                                          className="h-4 w-4"
                                          style={{ color: config.iconColor }}
                                       />
                                    </div>
                                    <span className="font-semibold text-zinc-800">
                                       {translatedText}
                                    </span>
                                 </div>
                              );
                           })}
                        </div>
                     </section>
                  )}

                  {/* Rooms Selection */}
                  <RoomPricingSection
                     propertyId={finalProperty.id}
                     roomTypes={finalProperty.roomTypes || []}
                  />

                  {/* Real Reviews Mockup */}
                  <section className="bg-white rounded-2xl p-6 md:p-8 border border-zinc-200/80 shadow-xs">
                     <h2 className="text-xl font-bold text-zinc-900 mb-6 pb-2 border-b border-zinc-100 flex items-center gap-2">
                        <MessageSquare className="h-5 w-5 text-[#006ce4]" />
                        <span>
                           {t("reviews")} ({displayReviewsCount})
                        </span>
                     </h2>
                     <p className="text-xs text-zinc-400 font-semibold mb-4 uppercase tracking-wider">
                        {t("verifiedReviews")}
                     </p>

                     <div className="space-y-6">
                        <div className="space-y-2">
                           <div className="flex items-center gap-3">
                              <div className="h-9 w-9 rounded-full bg-indigo-50 border border-indigo-100 flex items-center justify-center font-bold text-indigo-700 text-xs shrink-0">
                                 MH
                              </div>
                              <div>
                                 <p className="text-sm font-bold text-zinc-900">Minh Hoàng</p>
                                 <p className="text-[10px] text-zinc-400 font-medium">
                                    Việt Nam · Gia đình trẻ
                                 </p>
                              </div>
                              <div className="ml-auto bg-[#003580] text-white font-bold px-2 py-0.5 rounded text-xs">
                                 10.0
                              </div>
                           </div>
                           <p className="text-zinc-600 text-xs italic pl-12 leading-relaxed">
                              &ldquo;
                              {isVi
                                 ? "Bể bơi vô cực siêu đẹp, phục vụ buffet sáng rất phong phú và ngon miệng. Các bạn nhân viên phục vụ cực kỳ tận tâm và lịch sự. Gia đình tôi chắc chắn sẽ quay lại."
                                 : "Absolutely stunning infinity pool. The breakfast buffet was extensive and delicious. Staff were exceptionally polite and attentive. Highly recommend!"}
                              &rdquo;
                           </p>
                        </div>
                        <div className="space-y-2 border-t border-zinc-100 pt-4">
                           <div className="flex items-center gap-3">
                              <div className="h-9 w-9 rounded-full bg-amber-50 border border-amber-100 flex items-center justify-center font-bold text-amber-700 text-xs shrink-0">
                                 ES
                              </div>
                              <div>
                                 <p className="text-sm font-bold text-zinc-900">Emily Stone</p>
                                 <p className="text-[10px] text-zinc-400 font-medium">
                                    United Kingdom · Solo Traveller
                                 </p>
                              </div>
                              <div className="ml-auto bg-[#003580] text-white font-bold px-2 py-0.5 rounded text-xs">
                                 9.5
                              </div>
                           </div>
                           <p className="text-zinc-600 text-xs italic pl-12 leading-relaxed">
                              &ldquo;
                              {isVi
                                 ? "Resort vô cùng yên tĩnh và thư thái. Khu Spa đẳng cấp, tay nghề nhân viên trị liệu rất tốt. Dịch vụ quản gia hỗ trợ chu đáo từng li một."
                                 : "Extremely peaceful and clean resort. World-class spa experience, the therapist had incredible skills. The butler helped with everything I needed."}
                              &rdquo;
                           </p>
                        </div>
                     </div>
                  </section>
               </div>

               {/* Right 1/3 - Sticky Info & Guarantee */}
               <div className="space-y-6 lg:sticky lg:top-6 h-fit z-10">
                  {/* Sticky Booking Helper Card */}
                  <div className="bg-white rounded-2xl p-6 border border-zinc-200/80 shadow-sm space-y-5">
                     <div className="flex items-center justify-between pb-3 border-b border-zinc-100">
                        <span className="text-sm font-bold text-zinc-900">{t("guestRating")}</span>
                        <span className="bg-[#003580] text-white text-sm font-extrabold px-2 py-1 rounded">
                           {displayRating.toFixed(1)} / 10
                        </span>
                     </div>

                     <div className="space-y-3">
                        <div className="flex justify-between text-xs text-zinc-500 font-medium">
                           <span>{isVi ? "Vị trí địa lý" : "Location"}</span>
                           <span className="font-bold text-zinc-700">9.6 / 10</span>
                        </div>
                        <div className="flex justify-between text-xs text-zinc-500 font-medium">
                           <span>{isVi ? "Mức độ sạch sẽ" : "Cleanliness"}</span>
                           <span className="font-bold text-zinc-700">9.8 / 10</span>
                        </div>
                        <div className="flex justify-between text-xs text-zinc-500 font-medium">
                           <span>{isVi ? "Thái độ phục vụ" : "Staff Service"}</span>
                           <span className="font-bold text-zinc-700">9.7 / 10</span>
                        </div>
                     </div>

                     <div className="bg-zinc-50/80 rounded-xl p-4 border border-zinc-100 space-y-3.5">
                        <p className="text-xs font-bold text-zinc-900 uppercase tracking-wider">
                           {t("quickCheck")}
                        </p>

                        <div className="flex items-center gap-2 text-xs text-zinc-700 font-semibold">
                           <Clock className="h-4 w-4 text-[#006ce4] shrink-0" />
                           <span>{t("checkIn")}: 14:00 - 23:30</span>
                        </div>
                        <div className="flex items-center gap-2 text-xs text-zinc-700 font-semibold">
                           <Clock className="h-4 w-4 text-[#006ce4] shrink-0" />
                           <span>{t("checkOut")}: 06:00 - 12:00</span>
                        </div>
                     </div>

                     <div className="space-y-2">
                        <div className="flex items-center gap-2 text-xs text-zinc-600 font-medium">
                           <Shield className="h-4 w-4 text-[#008009] shrink-0" />
                           <span>{t("safeStay")}</span>
                        </div>
                        <div className="flex items-center gap-2 text-xs text-zinc-600 font-medium">
                           <Check className="h-4 w-4 text-[#008009] shrink-0" />
                           <span>{t("bestPrice")}</span>
                        </div>
                     </div>

                     <a
                        href="#available-rooms"
                        className="block text-center bg-[#006ce4] hover:bg-[#0057b7] text-white font-bold py-3.5 rounded-lg text-sm transition-all duration-300 shadow-md hover:shadow-lg cursor-pointer active:scale-[0.98]"
                     >
                        {isVi ? "Lựa chọn phòng ngủ" : "Choose Rooms"}
                     </a>
                  </div>

                  {/* Simple Mock Map Container */}
                  <div className="bg-white rounded-2xl overflow-hidden border border-zinc-200/80 shadow-xs p-1">
                     <div className="relative h-48 w-full bg-zinc-100 flex items-center justify-center text-center select-none rounded-xl overflow-hidden">
                        {/* Map Grid Vector Background */}
                        <div className="absolute inset-0 bg-[linear-gradient(to_right,#e4e4e7_1px,transparent_1px),linear-gradient(to_bottom,#e4e4e7_1px,transparent_1px)] bg-size-[20px_20px] opacity-35" />
                        <div className="absolute inset-0 bg-blue-900/5" />
                        <div className="absolute flex flex-col items-center gap-2 p-4">
                           <div className="h-9 w-9 rounded-full bg-white flex items-center justify-center shadow-lg border border-zinc-100 animate-bounce">
                              <MapPin className="h-5 w-5 text-red-500 fill-red-100" />
                           </div>
                           <p className="text-xs font-bold text-zinc-800 tracking-tight drop-shadow-xs">
                              {finalProperty.address}, {finalProperty.city}
                           </p>
                           <button className="bg-white hover:bg-zinc-50 border border-zinc-200/80 shadow-xs px-3 py-1.5 rounded-full text-[11px] font-bold text-[#006ce4] transition-colors cursor-pointer mt-1">
                              {t("map")}
                           </button>
                        </div>
                     </div>
                  </div>

                  {/* General Policies */}
                  <div className="bg-white rounded-2xl p-5 border border-zinc-200/80 shadow-xs space-y-4">
                     <h3 className="text-sm font-bold text-zinc-900 flex items-center gap-1.5 pb-2 border-b border-zinc-100">
                        <Shield className="h-4.5 w-4.5 text-[#006ce4]" />
                        <span>{t("policies")}</span>
                     </h3>
                     <div className="space-y-3 text-xs leading-relaxed text-zinc-600 font-medium">
                        <p>
                           {isVi
                              ? "· Thú cưng không được phép mang vào chỗ nghỉ để bảo vệ vệ sinh chung."
                              : "· Pets are not allowed inside the rooms for general hygiene standard."}
                        </p>
                        <p>
                           {isVi
                              ? "· Vui lòng không hút thuốc trong khuôn viên phòng ngủ khép kín."
                              : "· Non-smoking policy applies to all enclosed bedrooms."}
                        </p>
                        <p>
                           {isVi
                              ? "· Độ tuổi tối thiểu để check-in độc lập là 18 tuổi."
                              : "· Minimum age required for solo check-in is 18 years old."}
                        </p>
                     </div>
                  </div>
               </div>
            </div>

            {/* Anchor point target for room select button */}
            <div id="available-rooms" className="h-1" />
         </div>

         <Footer />
      </div>
   );
}
