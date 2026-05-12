"use client";

import { LocationPicker } from "@/components/Map";
import Navbar from "@/components/Navbar";

export default function DemoMapPage() {
   return (
      <main className="min-h-screen bg-gray-50">
         <Navbar />
         <div className="max-w-7xl mx-auto py-12 px-4 sm:px-6 lg:px-8">
            <div className="mb-8">
               <h1 className="text-3xl font-extrabold text-gray-900">
                  Demo: Location Picker (Booking.com Style)
               </h1>
               <p className="mt-2 text-lg text-gray-600">
                  Thử kéo thả ghim hoặc click trên bản đồ để chọn vị trí chỗ nghỉ của bạn.
               </p>
            </div>

            <div className="bg-white rounded-2xl shadow-sm p-2">
               <LocationPicker
                  onLocationChange={(lat, lng, addr) => {
                     console.log("Location Changed:", { lat, lng, addr });
                  }}
               />
            </div>

            <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-8">
               <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                  <h3 className="font-bold text-lg mb-2 text-[#006ce4]">Tương tác mượt mà</h3>
                  <p className="text-gray-600 text-sm leading-relaxed">
                     Sử dụng Framer Motion để tạo hiệu ứng lớp phủ (Overlay) mượt mà như trang chủ
                     của Booking.
                  </p>
               </div>
               <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                  <h3 className="font-bold text-lg mb-2 text-[#006ce4]">Độ chính xác cao</h3>
                  <p className="text-gray-600 text-sm leading-relaxed">
                     Pin có thể kéo thả giúp chủ nhà định vị chính xác vị trí căn hộ trên bản đồ vệ
                     tinh hoặc đường phố.
                  </p>
               </div>
               <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                  <h3 className="font-bold text-lg mb-2 text-[#006ce4]">Thiết kế Premium</h3>
                  <p className="text-gray-600 text-sm leading-relaxed">
                     Sử dụng hệ màu chuẩn và typography hiện đại, mang lại cảm giác tin cậy và
                     chuyên nghiệp.
                  </p>
               </div>
            </div>
         </div>
      </main>
   );
}
