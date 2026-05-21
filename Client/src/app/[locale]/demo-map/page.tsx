"use client";

import { useState } from "react";
import { MapView, LocationPicker } from "@/components/Map";
import {
   MapPin,
   Search,
   Compass,
   Settings,
   HelpCircle,
   Layers,
   Activity,
   Building,
   DollarSign,
   CheckCircle2,
   Home,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import Link from "next/link";
import { env } from "@/env";
import { PropertyDocument } from "@/lib/api/services/propertyService";

// Mock properties in Ho Chi Minh City for testing MapView
const mockProperties: PropertyDocument[] = [
   {
      id: "prop-1",
      name: "Landmark 81 Luxury Residence",
      description: "Trải nghiệm kỳ nghỉ thượng lưu tại tòa nhà cao nhất Việt Nam.",
      propertyType: "APARTMENT",
      address: "208 Nguyễn Hữu Cảnh, Phường 22, Bình Thạnh",
      city: "Thành phố Hồ Chí Minh",
      country: "Việt Nam",
      starRating: 5,
      amenities: ["wifi", "pool", "parking", "spa"],
      averageRating: 4.9,
      reviewCount: 312,
      mainImageUrl:
         "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?auto=format&fit=crop&w=400&q=80",
      minPrice: 3200000,
      location: { lat: 10.7948, lon: 106.7218 }, // Landmark 81
   },
   {
      id: "prop-2",
      name: "Majestic Hotel Saigon",
      description: "Khách sạn cổ kính mang đậm phong cách Pháp bên sông Sài Gòn.",
      propertyType: "HOTEL",
      address: "1 Đồng Khởi, Phường Bến Nghé, Quận 1",
      city: "Thành phố Hồ Chí Minh",
      country: "Việt Nam",
      starRating: 5,
      amenities: ["wifi", "restaurant", "pool"],
      averageRating: 4.7,
      reviewCount: 189,
      mainImageUrl:
         "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=400&q=80",
      minPrice: 2100000,
      location: { lat: 10.7731, lon: 106.7071 }, // Majestic Hotel
   },
   {
      id: "prop-3",
      name: "Bến Thành Retro Homestay",
      description: "Homestay nhỏ xinh, hoài cổ nằm ngay trung tâm thành phố.",
      propertyType: "HOMESTAY",
      address: "15 Lê Thánh Tôn, Phường Bến Thành, Quận 1",
      city: "Thành phố Hồ Chí Minh",
      country: "Việt Nam",
      starRating: 4,
      amenities: ["wifi"],
      averageRating: 4.5,
      reviewCount: 94,
      mainImageUrl:
         "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=400&q=80",
      minPrice: 850000,
      location: { lat: 10.7725, lon: 106.698 }, // Ben Thanh Market
   },
   {
      id: "prop-4",
      name: "Saigon Notre-Dame Villa",
      description: "Biệt thự sang trọng có khuôn viên xanh mát cạnh Nhà thờ Đức Bà.",
      propertyType: "VILLA",
      address: "8 Công xã Paris, Phường Bến Nghé, Quận 1",
      city: "Thành phố Hồ Chí Minh",
      country: "Việt Nam",
      starRating: 5,
      amenities: ["wifi", "pool", "parking"],
      averageRating: 4.8,
      reviewCount: 42,
      mainImageUrl:
         "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=400&q=80",
      minPrice: 4500000,
      location: { lat: 10.7798, lon: 106.699 }, // Notre-Dame Cathedral
   },
];

export default function DemoMapPage() {
   const [activeTab, setActiveTab] = useState<"view" | "pick">("view");

   // MapView selected state
   const [selectedPropId, setSelectedPropId] = useState<string | null>(null);

   // LocationPicker state
   const [pickerResult, setPickerResult] = useState<{
      lat: number;
      lng: number;
      address: string;
   } | null>(null);

   const [addressDetails, setAddressDetails] = useState<{
      address: string;
      city: string;
      country: string;
   } | null>(null);

   const handleLocationChange = (lat: number, lng: number, address: string) => {
      setPickerResult({ lat, lng, address });
   };

   const handleAddressDetailsChange = (details: {
      address: string;
      city: string;
      country: string;
   }) => {
      setAddressDetails(details);
   };

   return (
      <div className="min-h-screen bg-linear-to-br from-gray-900 via-zinc-950 to-gray-900 text-white font-sans selection:bg-[#006ce4]/30 selection:text-blue-300">
         {/* Top Glassmorphic Navigation */}
         <header className="sticky top-0 z-100 backdrop-blur-md bg-zinc-950/70 border-b border-zinc-800/50 px-8 py-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
               <div className="p-2 bg-[#006ce4] rounded-xl shadow-lg shadow-blue-500/20">
                  <Compass
                     className="h-6 w-6 text-white animate-spin"
                     style={{ animationDuration: "12s" }}
                  />
               </div>
               <div>
                  <h1 className="text-xl font-black tracking-wider bg-linear-to-r from-blue-400 to-indigo-400 bg-clip-text text-transparent">
                     VIETMAP INTEGRATION
                  </h1>
                  <p className="text-[10px] text-zinc-500 font-semibold uppercase tracking-widest">
                     OmniBooking Map System
                  </p>
               </div>
            </div>

            <div className="flex items-center gap-4">
               <span className="flex items-center gap-1.5 px-3 py-1 bg-green-500/10 border border-green-500/20 rounded-full text-xs text-green-400 font-medium">
                  <span className="w-1.5 h-1.5 bg-green-500 rounded-full animate-ping"></span>
                  Bản đồ đang chạy (Active)
               </span>
               <Link
                  href="/"
                  className="flex items-center gap-1.5 px-4 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 hover:text-white rounded-xl text-xs font-bold transition-all"
               >
                  <Home className="h-4 w-4" />
                  Về trang chủ
               </Link>
            </div>
         </header>

         {/* Content Wrapper */}
         <main className="max-w-7xl mx-auto px-8 py-10 grid grid-cols-1 lg:grid-cols-12 gap-8">
            {/* Control Sidebar (Column 4) */}
            <div className="lg:col-span-4 flex flex-col gap-6">
               <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="bg-zinc-900/60 border border-zinc-800/80 backdrop-blur-md p-6 rounded-2xl shadow-xl flex flex-col"
               >
                  <h2 className="text-lg font-black tracking-wide text-zinc-100 flex items-center gap-2 mb-4">
                     <Settings className="h-5 w-5 text-blue-500" />
                     BẢNG KIỂM TRA
                  </h2>
                  <p className="text-sm text-zinc-400 leading-relaxed mb-6">
                     Kiểm tra, theo dõi trạng thái tương tác thời gian thực của các component bản đồ
                     VietMap bên phải.
                  </p>

                  {/* Navigation Tabs */}
                  <div className="grid grid-cols-2 gap-2 bg-zinc-950 p-1.5 rounded-xl border border-zinc-800/50 mb-6">
                     <button
                        onClick={() => setActiveTab("view")}
                        className={`py-2.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                           activeTab === "view"
                              ? "bg-[#006ce4] text-white shadow-md shadow-blue-500/10"
                              : "text-zinc-400 hover:text-zinc-200"
                        }`}
                     >
                        MapView (Mẫu Ghim)
                     </button>
                     <button
                        onClick={() => setActiveTab("pick")}
                        className={`py-2.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                           activeTab === "pick"
                              ? "bg-[#006ce4] text-white shadow-md shadow-blue-500/10"
                              : "text-zinc-400 hover:text-zinc-200"
                        }`}
                     >
                        LocationPicker (Tìm)
                     </button>
                  </div>

                  {/* Dynamic Monitor details */}
                  <div className="flex-1">
                     <AnimatePresence mode="wait">
                        {activeTab === "view" ? (
                           <motion.div
                              key="view-detail"
                              initial={{ opacity: 0, x: -10 }}
                              animate={{ opacity: 1, x: 0 }}
                              exit={{ opacity: 0, x: 10 }}
                              className="space-y-4"
                           >
                              <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider block">
                                 Thông số Chỗ nghỉ (MapView)
                              </span>

                              <div className="bg-zinc-950/80 p-4 rounded-xl border border-zinc-800/80 space-y-3">
                                 <div className="flex items-center justify-between">
                                    <span className="text-xs text-zinc-500 font-semibold">
                                       Tổng số ghim mẫu
                                    </span>
                                    <span className="text-xs text-blue-400 font-bold">
                                       {mockProperties.length} Ghim
                                    </span>
                                 </div>
                                 <div className="flex items-center justify-between">
                                    <span className="text-xs text-zinc-500 font-semibold">
                                       Tỉ lệ tải bản đồ
                                    </span>
                                    <span className="text-xs text-green-400 font-bold">
                                       100% Mượt
                                    </span>
                                 </div>
                                 <div className="flex items-center justify-between">
                                    <span className="text-xs text-zinc-500 font-semibold">
                                       API Tile Server
                                    </span>
                                    <span className="text-xs text-zinc-300 font-bold truncate max-w-[150px]">
                                       VietMap Tiles v1
                                    </span>
                                 </div>
                              </div>

                              <div className="bg-[#006ce4]/5 border border-blue-500/10 p-4 rounded-xl">
                                 <h4 className="text-xs font-bold text-blue-400 flex items-center gap-1.5 mb-2">
                                    <CheckCircle2 className="h-4 w-4" />
                                    Hướng dẫn trải nghiệm
                                 </h4>
                                 <p className="text-xs text-zinc-400 leading-relaxed">
                                    Bản đồ MapView hiển thị các khách sạn dưới dạng ghim tag giá.
                                    Bạn hãy nhấp vào các tag giá màu xanh trên bản đồ để xem chi
                                    tiết khách sạn hiển thị popup.
                                 </p>
                              </div>
                           </motion.div>
                        ) : (
                           <motion.div
                              key="pick-detail"
                              initial={{ opacity: 0, x: -10 }}
                              animate={{ opacity: 1, x: 0 }}
                              exit={{ opacity: 0, x: 10 }}
                              className="space-y-4"
                           >
                              <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider block">
                                 Dữ liệu vị trí chọn (LocationPicker)
                              </span>

                              <div className="bg-zinc-950/80 p-4 rounded-xl border border-zinc-800/80 space-y-3">
                                 <div>
                                    <span className="text-[10px] text-zinc-500 font-bold block mb-1">
                                       Địa chỉ được trả về
                                    </span>
                                    <p className="text-xs text-zinc-200 leading-relaxed font-semibold bg-zinc-900 px-3 py-2 rounded-lg border border-zinc-800">
                                       {pickerResult?.address || "Chưa có vị trí nào được chọn"}
                                    </p>
                                 </div>

                                 <div className="grid grid-cols-2 gap-3">
                                    <div>
                                       <span className="text-[10px] text-zinc-500 font-bold block mb-1">
                                          Vĩ độ (Lat)
                                       </span>
                                       <div className="text-xs text-blue-400 font-bold bg-zinc-900 px-3 py-1.5 rounded-lg border border-zinc-800">
                                          {pickerResult ? pickerResult.lat.toFixed(6) : "—"}
                                       </div>
                                    </div>
                                    <div>
                                       <span className="text-[10px] text-zinc-500 font-bold block mb-1">
                                          Kinh độ (Lng)
                                       </span>
                                       <div className="text-xs text-blue-400 font-bold bg-zinc-900 px-3 py-1.5 rounded-lg border border-zinc-800">
                                          {pickerResult ? pickerResult.lng.toFixed(6) : "—"}
                                       </div>
                                    </div>
                                 </div>
                              </div>

                              <div className="bg-[#006ce4]/5 border border-blue-500/10 p-4 rounded-xl">
                                 <h4 className="text-xs font-bold text-blue-400 flex items-center gap-1.5 mb-2">
                                    <CheckCircle2 className="h-4 w-4" />
                                    Tính năng thông minh
                                 </h4>
                                 <p className="text-xs text-zinc-400 leading-relaxed">
                                    Bạn có thể:
                                    <br />
                                    1. Nhập địa chỉ tìm kiếm vào ô ở góc trái để tự động gợi ý
                                    VietMap Autocomplete v4.
                                    <br />
                                    2. Nhấp chuột vào bất cứ điểm nào trên bản đồ để di chuyển ghim.
                                    <br />
                                    3. Kéo thả trực tiếp ghim để chỉnh tọa độ hoàn hảo.
                                 </p>
                              </div>
                           </motion.div>
                        )}
                     </AnimatePresence>
                  </div>
               </motion.div>

               {/* Tech Details Badge Card */}
               <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.1 }}
                  className="bg-zinc-900/60 border border-zinc-800/80 backdrop-blur-md p-6 rounded-2xl shadow-xl space-y-4"
               >
                  <h3 className="text-xs font-black tracking-widest text-zinc-400 uppercase flex items-center gap-2">
                     <Activity className="h-4 w-4 text-emerald-400 animate-pulse" />
                     VIETMAP V4 ECOSYSTEM
                  </h3>

                  <div className="space-y-2.5">
                     <div className="flex items-center justify-between text-xs py-1 border-b border-zinc-800/50">
                        <span className="text-zinc-500">API Key Đang chạy</span>
                        <span className="px-2 py-0.5 bg-blue-500/10 border border-blue-500/20 text-blue-400 font-bold rounded-md text-[10px]">
                           {env.NEXT_PUBLIC_VIETMAP_API_KEY ? "Đã nạp" : "Trống (Cần cấu hình)"}
                        </span>
                     </div>
                     <div className="flex items-center justify-between text-xs py-1 border-b border-zinc-800/50">
                        <span className="text-zinc-500">Map Rendering Engine</span>
                        <span className="text-zinc-300 font-semibold">Leaflet 1.9.4</span>
                     </div>
                     <div className="flex items-center justify-between text-xs py-1 border-b border-zinc-800/50">
                        <span className="text-zinc-500">Autocomplete Version</span>
                        <span className="text-zinc-300 font-semibold">VietMap Autocomplete v4</span>
                     </div>
                     <div className="flex items-center justify-between text-xs py-1">
                        <span className="text-zinc-500">Place Lookup Version</span>
                        <span className="text-zinc-300 font-semibold">VietMap Place v4</span>
                     </div>
                  </div>
               </motion.div>
            </div>

            {/* Map Frame (Column 8) */}
            <div className="lg:col-span-8 flex flex-col h-[760px] bg-zinc-950 border border-zinc-800/60 rounded-3xl overflow-hidden shadow-2xl relative z-0">
               <MapView
                  properties={mockProperties}
                  center={[10.7769, 106.7009]}
                  zoom={13}
                  showControls={true}
               />
            </div>
         </main>

         {/* Fullscreen LocationPicker Overlay matching the partner flow */}
         <AnimatePresence>
            {activeTab === "pick" && (
               <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: 20 }}
                  transition={{ duration: 0.3 }}
                  className="fixed inset-x-0 bottom-0 top-[72px] z-50 bg-zinc-50"
               >
                  <LocationPicker
                     onLocationChange={handleLocationChange}
                     onAddressDetailsChange={handleAddressDetailsChange}
                     showNavigation={true}
                     onBack={() => setActiveTab("view")}
                     onNext={() => {
                        alert(
                           `Đăng ký vị trí thành công (Demo)!\n\n` +
                              `• Tọa độ: Lat ${pickerResult?.lat}, Lng ${pickerResult?.lng}\n` +
                              `• Địa chỉ: ${addressDetails?.address || "—"}\n` +
                              `• Thành phố: ${addressDetails?.city || "—"}\n` +
                              `• Quốc gia: ${addressDetails?.country || "—"}`
                        );
                        setActiveTab("view");
                     }}
                     className="h-full w-full border-none rounded-none shadow-none"
                  />
               </motion.div>
            )}
         </AnimatePresence>
      </div>
   );
}
