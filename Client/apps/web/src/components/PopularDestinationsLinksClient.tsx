"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Plus, Minus } from "lucide-react";
import { useTranslations } from "next-intl";

interface LinkItem {
   name: string;
   searchQuery: string;
}

interface PopularDestinationsLinksClientProps {
   locale: string;
   countryName: string;
}

export default function PopularDestinationsLinksClient({
   locale,
   countryName,
}: PopularDestinationsLinksClientProps) {
   const t = useTranslations("Home");
   const router = useRouter();
   const [activeTab, setActiveTab] = useState(0);
   const [isExpanded, setIsExpanded] = useState(false);

   // 1. Domestic Cities
   const domesticCitiesVi: LinkItem[] = [
      { name: "Khách sạn TP. Hồ Chí Minh", searchQuery: "Thành Phố Hồ Chí Minh" },
      { name: "Khách sạn Vũng Tàu", searchQuery: "Vũng Tàu" },
      { name: "Khách sạn Hà Nội", searchQuery: "Hà Nội" },
      { name: "Khách sạn Đà Nẵng", searchQuery: "Đà Nẵng" },
      { name: "Khách sạn Đà Lạt", searchQuery: "Đà Lạt" },
      { name: "Khách sạn Phú Quốc", searchQuery: "Phú Quốc" },
      { name: "Khách sạn Nha Trang", searchQuery: "Nha Trang" },
      { name: "Khách sạn Huế", searchQuery: "Huế" },
      { name: "Khách sạn Mũi Né", searchQuery: "Mũi Né" },
      { name: "Khách sạn Sa Pa", searchQuery: "Sapa" },
      { name: "Khách sạn Thành phố Hải Phòng", searchQuery: "Hải Phòng" },
      { name: "Khách sạn Mai Châu", searchQuery: "Mai Châu" },
      { name: "Khách sạn Hà Tiên", searchQuery: "Hà Tiên" },
      { name: "Khách sạn Tuần Châu", searchQuery: "Tuần Châu" },
      { name: "Khách sạn Hội An", searchQuery: "Hội An" },
      { name: "Khách sạn Tam Đảo", searchQuery: "Tam Đảo" },
      { name: "Khách sạn Cao Lãnh", searchQuery: "Cao Lãnh" },
      { name: "Khách sạn Vĩnh Phúc", searchQuery: "Vĩnh Phúc" },
      { name: "Khách sạn Châu Đốc", searchQuery: "Châu Đốc" },
      { name: "Khách sạn Đảo Cát Bà", searchQuery: "Cát Bà" },
      { name: "Khách sạn Cần Thơ", searchQuery: "Cần Thơ" },
      { name: "Khách sạn Bến Tre", searchQuery: "Bến Tre" },
      { name: "Khách sạn Buôn Ma Thuột", searchQuery: "Buôn Ma Thuột" },
      { name: "Khách sạn Mộc Châu", searchQuery: "Mộc Châu" },
      { name: "Khách sạn Thanh Khê", searchQuery: "Thanh Khê" },
   ];

   const domesticCitiesEn: LinkItem[] = [
      { name: "Hotels in Ho Chi Minh City", searchQuery: "Thành Phố Hồ Chí Minh" },
      { name: "Hotels in Vung Tau", searchQuery: "Vung Tau" },
      { name: "Hotels in Hanoi", searchQuery: "Hanoi" },
      { name: "Hotels in Da Nang", searchQuery: "Da Nang" },
      { name: "Hotels in Da Lat", searchQuery: "Da Lat" },
      { name: "Hotels in Phu Quoc", searchQuery: "Phu Quoc" },
      { name: "Hotels in Nha Trang", searchQuery: "Nha Trang" },
      { name: "Hotels in Hue", searchQuery: "Hue" },
      { name: "Hotels in Mui Ne", searchQuery: "Mui Ne" },
      { name: "Hotels in Sa Pa", searchQuery: "Sapa" },
      { name: "Hotels in Hai Phong", searchQuery: "Hai Phong" },
      { name: "Hotels in Mai Chau", searchQuery: "Mai Chau" },
      { name: "Hotels in Ha Tien", searchQuery: "Ha Tien" },
      { name: "Hotels in Tuan Châu", searchQuery: "Tuan Chau" },
      { name: "Hotels in Hoi An", searchQuery: "Hoi An" },
      { name: "Hotels in Tam Dao", searchQuery: "Tam Dao" },
      { name: "Hotels in Cao Lanh", searchQuery: "Cao Lanh" },
      { name: "Hotels in Vinh Phuc", searchQuery: "Vinh Phuc" },
      { name: "Hotels in Chau Doc", searchQuery: "Chau Doc" },
      { name: "Hotels in Cat Ba Island", searchQuery: "Cat Ba" },
      { name: "Hotels in Can Tho", searchQuery: "Can Tho" },
      { name: "Hotels in Ben Tre", searchQuery: "Ben Tre" },
      { name: "Hotels in Buon Ma Thuot", searchQuery: "Buon Ma Thuot" },
      { name: "Hotels in Moc Chau", searchQuery: "Moc Chau" },
      { name: "Hotels in Thanh Khe", searchQuery: "Thanh Khe" },
   ];

   // 2. International Cities
   const internationalCitiesVi: LinkItem[] = [
      { name: "Khách sạn Paris", searchQuery: "Paris" },
      { name: "Khách sạn London", searchQuery: "London" },
      { name: "Khách sạn Tokyo", searchQuery: "Tokyo" },
      { name: "Khách sạn New York", searchQuery: "New York" },
      { name: "Khách sạn Bangkok", searchQuery: "Bangkok" },
      { name: "Khách sạn Singapore", searchQuery: "Singapore" },
      { name: "Khách sạn Seoul", searchQuery: "Seoul" },
      { name: "Khách sạn Sydney", searchQuery: "Sydney" },
      { name: "Khách sạn Rome", searchQuery: "Rome" },
      { name: "Khách sạn Barcelona", searchQuery: "Barcelona" },
      { name: "Khách sạn Kuala Lumpur", searchQuery: "Kuala Lumpur" },
      { name: "Khách sạn Hong Kong", searchQuery: "Hong Kong" },
      { name: "Khách sạn Đài Bắc", searchQuery: "Taipei" },
      { name: "Khách sạn Melbourne", searchQuery: "Melbourne" },
      { name: "Khách sạn Amsterdam", searchQuery: "Amsterdam" },
      { name: "Khách sạn Berlin", searchQuery: "Berlin" },
      { name: "Khách sạn Dubai", searchQuery: "Dubai" },
      { name: "Khách sạn Thượng Hải", searchQuery: "Shanghai" },
      { name: "Khách sạn Los Angeles", searchQuery: "Los Angeles" },
      { name: "Khách sạn Istanbul", searchQuery: "Istanbul" },
      { name: "Khách sạn Bali", searchQuery: "Bali" },
      { name: "Khách sạn Phuket", searchQuery: "Phuket" },
      { name: "Khách sạn Milan", searchQuery: "Milan" },
      { name: "Khách sạn Vienna", searchQuery: "Vienna" },
      { name: "Khách sạn Prague", searchQuery: "Prague" },
   ];

   const internationalCitiesEn: LinkItem[] = [
      { name: "Hotels in Paris", searchQuery: "Paris" },
      { name: "Hotels in London", searchQuery: "London" },
      { name: "Hotels in Tokyo", searchQuery: "Tokyo" },
      { name: "Hotels in New York", searchQuery: "New York" },
      { name: "Hotels in Bangkok", searchQuery: "Bangkok" },
      { name: "Hotels in Singapore", searchQuery: "Singapore" },
      { name: "Hotels in Seoul", searchQuery: "Seoul" },
      { name: "Hotels in Sydney", searchQuery: "Sydney" },
      { name: "Hotels in Rome", searchQuery: "Rome" },
      { name: "Hotels in Barcelona", searchQuery: "Barcelona" },
      { name: "Hotels in Kuala Lumpur", searchQuery: "Kuala Lumpur" },
      { name: "Hotels in Hong Kong", searchQuery: "Hong Kong" },
      { name: "Hotels in Taipei", searchQuery: "Taipei" },
      { name: "Hotels in Melbourne", searchQuery: "Melbourne" },
      { name: "Hotels in Amsterdam", searchQuery: "Amsterdam" },
      { name: "Hotels in Berlin", searchQuery: "Berlin" },
      { name: "Hotels in Dubai", searchQuery: "Dubai" },
      { name: "Hotels in Shanghai", searchQuery: "Shanghai" },
      { name: "Hotels in Los Angeles", searchQuery: "Los Angeles" },
      { name: "Hotels in Istanbul", searchQuery: "Istanbul" },
      { name: "Hotels in Bali", searchQuery: "Bali" },
      { name: "Hotels in Phuket", searchQuery: "Phuket" },
      { name: "Hotels in Milan", searchQuery: "Milan" },
      { name: "Hotels in Vienna", searchQuery: "Vienna" },
      { name: "Hotels in Prague", searchQuery: "Prague" },
   ];

   // 3. Regions
   const regionsVi: LinkItem[] = [
      { name: "Phú Quốc", searchQuery: "Phú Quốc" },
      { name: "Đảo Cát Bà", searchQuery: "Cát Bà" },
      { name: "Côn Đảo", searchQuery: "Côn Đảo" },
      { name: "Sầm Sơn", searchQuery: "Sầm Sơn" },
      { name: "Mũi Né", searchQuery: "Mũi Né" },
      { name: "Vịnh Hạ Long", searchQuery: "Hạ Long" },
      { name: "Cô Tô", searchQuery: "Cô Tô" },
      { name: "Phong Nha", searchQuery: "Phong Nha" },
      { name: "Tràng An", searchQuery: "Tràng An" },
      { name: "Hồ Tuyền Lâm", searchQuery: "Hồ Tuyền Lâm" },
      { name: "Bán đảo Sơn Trà", searchQuery: "Sơn Trà" },
      { name: "Đồi Chè Cầu Đất", searchQuery: "Cầu Đất" },
      { name: "Đồng Văn", searchQuery: "Đồng Văn" },
      { name: "Mù Cang Chải", searchQuery: "Mù Cang Chải" },
      { name: "Cửa Lò", searchQuery: "Cửa Lò" },
   ];

   const regionsEn: LinkItem[] = [
      { name: "Phu Quoc", searchQuery: "Phu Quoc" },
      { name: "Cat Ba Island", searchQuery: "Cat Ba" },
      { name: "Con Dao", searchQuery: "Con Dao" },
      { name: "Sam Son", searchQuery: "Sam Son" },
      { name: "Mui Ne", searchQuery: "Mui Ne" },
      { name: "Ha Long Bay", searchQuery: "Ha Long" },
      { name: "Co To Island", searchQuery: "Co To" },
      { name: "Phong Nha", searchQuery: "Phong Nha" },
      { name: "Trang An", searchQuery: "Trang An" },
      { name: "Tuyen Lam Lake", searchQuery: "Tuyen Lam" },
      { name: "Son Tra Peninsula", searchQuery: "Son Tra" },
      { name: "Cau Dat Tea Hill", searchQuery: "Cau Dat" },
      { name: "Dong Van", searchQuery: "Dong Van" },
      { name: "Mu Cang Chai", searchQuery: "Mu Cang Chai" },
      { name: "Cua Lo", searchQuery: "Cua Lo" },
   ];

   // 4. Countries
   const countriesVi: LinkItem[] = [
      { name: "Việt Nam", searchQuery: "Việt Nam" },
      { name: "Thái Lan", searchQuery: "Thái Lan" },
      { name: "Nhật Bản", searchQuery: "Nhật Bản" },
      { name: "Hoa Kỳ", searchQuery: "Hoa Kỳ" },
      { name: "Pháp", searchQuery: "Pháp" },
      { name: "Vương quốc Anh", searchQuery: "Vương quốc Anh" },
      { name: "Singapore", searchQuery: "Singapore" },
      { name: "Hàn Quốc", searchQuery: "Hàn Quốc" },
      { name: "Úc", searchQuery: "Úc" },
      { name: "Ý", searchQuery: "Ý" },
      { name: "Tây Ban Nha", searchQuery: "Tây Ban Nha" },
      { name: "Đức", searchQuery: "Đức" },
      { name: "Trung Quốc", searchQuery: "Trung Quốc" },
      { name: "Malaysia", searchQuery: "Malaysia" },
      { name: "Indonesia", searchQuery: "Indonesia" },
   ];

   const countriesEn: LinkItem[] = [
      { name: "Vietnam", searchQuery: "Vietnam" },
      { name: "Thailand", searchQuery: "Thailand" },
      { name: "Japan", searchQuery: "Japan" },
      { name: "United States", searchQuery: "United States" },
      { name: "France", searchQuery: "France" },
      { name: "United Kingdom", searchQuery: "United Kingdom" },
      { name: "Singapore", searchQuery: "Singapore" },
      { name: "South Korea", searchQuery: "South Korea" },
      { name: "Australia", searchQuery: "Australia" },
      { name: "Italy", searchQuery: "Italy" },
      { name: "Spain", searchQuery: "Spain" },
      { name: "Germany", searchQuery: "Germany" },
      { name: "China", searchQuery: "China" },
      { name: "Malaysia", searchQuery: "Malaysia" },
      { name: "Indonesia", searchQuery: "Indonesia" },
   ];

   // 5. Accommodations
   const accommodationsVi: LinkItem[] = [
      { name: "Căn hộ", searchQuery: "căn hộ" },
      { name: "Biệt thự", searchQuery: "biệt thự" },
      { name: "Resort", searchQuery: "resort" },
      { name: "Homestay", searchQuery: "homestay" },
      { name: "Nhà nghỉ", searchQuery: "nhà nghỉ" },
      { name: "Nhà khách", searchQuery: "nhà khách" },
      { name: "Khách sạn", searchQuery: "khách sạn" },
      { name: "B&B", searchQuery: "B&B" },
      { name: "Hostel", searchQuery: "hostel" },
      { name: "Khách sạn căn hộ", searchQuery: "khách sạn căn hộ" },
   ];

   const accommodationsEn: LinkItem[] = [
      { name: "Apartments", searchQuery: "apartment" },
      { name: "Villas", searchQuery: "villa" },
      { name: "Resorts", searchQuery: "resort" },
      { name: "Homestays", searchQuery: "homestay" },
      { name: "Guesthouses", searchQuery: "guesthouse" },
      { name: "Hotels", searchQuery: "hotel" },
      { name: "B&Bs", searchQuery: "B&B" },
      { name: "Hostels", searchQuery: "hostel" },
      { name: "Aparthotels", searchQuery: "aparthotel" },
   ];

   const isVi = locale === "vi";

   const tabs = [
      { id: 0, label: t("tabDomesticCities"), data: isVi ? domesticCitiesVi : domesticCitiesEn },
      {
         id: 1,
         label: t("tabInternationalCities"),
         data: isVi ? internationalCitiesVi : internationalCitiesEn,
      },
      { id: 2, label: t("tabRegions"), data: isVi ? regionsVi : regionsEn },
      { id: 3, label: t("tabCountries"), data: isVi ? countriesVi : countriesEn },
      { id: 4, label: t("tabAccommodations"), data: isVi ? accommodationsVi : accommodationsEn },
   ];

   const currentTab = tabs[activeTab];
   const visibleData = isExpanded ? currentTab.data : currentTab.data.slice(0, 10);

   const handleLinkClick = (query: string) => {
      const params = new URLSearchParams();
      params.set("ss", query);
      params.set("group_adults", "2");
      params.set("group_children", "0");
      params.set("no_rooms", "1");
      router.push(`/${locale}/search?${params.toString()}`);
   };

   // SEO Inline Links list
   const seoLinksVi = [
      "Các quốc gia",
      "Khu vực",
      "Thành phố",
      "Quận",
      "Sân bay",
      "Khách sạn",
      "Địa điểm được quan tâm",
      "Các Nhà Nghỉ Dưỡng",
      "Căn hộ",
      "Các resort",
      "Các biệt thự",
      "Các hostel",
      "Nhà nghỉ B&B",
      "Các nhà khách",
      "Những chỗ nghỉ độc đáo",
      "Tất cả các điểm đến",
      "Tất cả các điểm đến có chuyến bay",
      "Tất cả địa điểm cho thuê xe",
      "Tất cả điểm đến cho kỳ nghỉ",
      "Hướng dẫn",
      "Khám phá",
      "Khám phá lưu trú theo tháng",
   ];

   const seoLinksEn = [
      "Countries",
      "Regions",
      "Cities",
      "Districts",
      "Airports",
      "Hotels",
      "Places of interest",
      "Vacation homes",
      "Apartments",
      "Resorts",
      "Villas",
      "Hostels",
      "B&Bs",
      "Guesthouses",
      "Unique places to stay",
      "All destinations",
      "All flight destinations",
      "All car rental locations",
      "All holiday destinations",
      "Guides",
      "Discover",
      "Discover monthly stays",
   ];

   const seoLinks = isVi ? seoLinksVi : seoLinksEn;

   return (
      <section className="mt-10 pb-6">
         {/* Title */}
         <h3 className="text-2xl font-bold text-black">
            {t("popularTravelersTitle", { country: countryName })}
         </h3>

         {/* Tabs Selector */}
         <div className="flex gap-2 mt-4 overflow-x-auto pb-2 no-scrollbar">
            {tabs.map((tab) => {
               const selected = activeTab === tab.id;
               return (
                  <button
                     key={tab.id}
                     onClick={() => {
                        setActiveTab(tab.id);
                        setIsExpanded(false);
                     }}
                     className={`px-4 py-2 text-sm font-semibold rounded-full cursor-pointer whitespace-nowrap transition-all ${
                        selected
                           ? "border border-[#006ce4] bg-[#006ce4]/5 text-[#006ce4]"
                           : "text-zinc-600 hover:text-black hover:bg-zinc-100 border border-transparent"
                     }`}
                  >
                     {tab.label}
                  </button>
               );
            })}
         </div>

         {/* Links Grid */}
         <div className="mt-6 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-y-3 gap-x-4">
            {visibleData.map((item, idx) => (
               <div
                  key={idx}
                  onClick={() => handleLinkClick(item.searchQuery)}
                  className="text-sm text-zinc-600 hover:text-[#006ce4] hover:underline cursor-pointer transition-colors truncate"
               >
                  {item.name}
               </div>
            ))}
         </div>

         {/* Show More/Less Button */}
         {currentTab.data.length > 10 && (
            <button
               onClick={() => setIsExpanded(!isExpanded)}
               className="flex items-center gap-1.5 text-sm font-bold text-[#006ce4] hover:text-[#0057b7] cursor-pointer mt-6 transition-colors"
            >
               {isExpanded ? (
                  <>
                     <Minus className="h-4 w-4 stroke-3" />
                     <span>{t("showLess")}</span>
                  </>
               ) : (
                  <>
                     <Plus className="h-4 w-4 stroke-3" />
                     <span>{t("showMore")}</span>
                  </>
               )}
            </button>
         )}

         {/* SEO Dot Separated Inline Links */}
         <div className="mt-8 pt-8 border-t border-zinc-100 text-[11px] text-[#006ce4] leading-relaxed flex flex-wrap gap-x-2 gap-y-1.5 justify-start">
            {seoLinks.map((link, idx) => (
               <div key={idx} className="flex items-center gap-2">
                  <span
                     onClick={() => handleLinkClick(link)}
                     className="hover:underline cursor-pointer transition-colors"
                  >
                     {link}
                  </span>
                  {idx < seoLinks.length - 1 && (
                     <span className="text-zinc-300 select-none font-normal">·</span>
                  )}
               </div>
            ))}
         </div>
      </section>
   );
}
