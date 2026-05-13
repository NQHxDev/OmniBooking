"use client";

import { LocationPicker } from "@/components/Map";
import Navbar from "@/components/Navbar";
import PriceDisplay from "@/components/PriceDisplay";
import apiClient from "@/lib/api/apiClient";

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

            {/* <div className="bg-white rounded-2xl shadow-sm p-2">
               <LocationPicker
                  onLocationChange={(lat, lng, addr) => {
                     console.log("Location Changed:", { lat, lng, addr });
                  }}
               />
            </div> */}

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

            <div className="mt-12 bg-white rounded-2xl shadow-sm p-8 border border-gray-100">
               <div className="mb-6">
                  <h2 className="text-2xl font-bold text-gray-900">Demo: Currency Conversion</h2>
                  <p className="text-gray-600 mt-1">
                     Thử thay đổi tiền tệ ở thanh Menu phía trên để xem giá tự động cập nhật.
                  </p>
               </div>

               <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                  {[10, 49.99, 120, 1500].map((price) => (
                     <div
                        key={price}
                        className="p-6 rounded-xl bg-gray-50 border border-gray-100 flex flex-col items-center justify-center gap-2 hover:shadow-md transition-shadow"
                     >
                        <span className="text-xs font-bold uppercase tracking-wider text-gray-400">
                           Giá gốc (USD): ${price}
                        </span>
                        <PriceDisplay amount={price} size="xl" className="text-[#006ce4]" />
                        <span className="text-[10px] text-gray-500 italic">
                           (Tỉ giá lấy từ Backend & Redis)
                        </span>
                     </div>
                  ))}
               </div>
            </div>
            <div className="mt-12 bg-white rounded-2xl shadow-sm p-8 border border-gray-100">
               <div className="mb-6">
                  <h2 className="text-2xl font-bold text-gray-900">Demo: Searchable Encryption</h2>
                  <p className="text-gray-600 mt-1">
                     Thử nghiệm tìm kiếm chính xác số điện thoại đã được mã hóa trong Database bằng
                     Blind Index.
                  </p>
               </div>

               <div className="max-w-xl">
                  <div className="flex gap-3">
                     <input
                        id="phone-search-input"
                        type="text"
                        placeholder="Nhập số điện thoại cần tìm..."
                        className="flex-1 px-4 py-2 rounded-lg border border-gray-200 focus:border-[#006ce4] focus:outline-none"
                     />
                     <button
                        onClick={async () => {
                           const phone = (
                              document.getElementById("phone-search-input") as HTMLInputElement
                           ).value;
                           if (!phone) return alert("Vui lòng nhập số điện thoại");

                           try {
                              const result = await apiClient.get(
                                 `/test/search-phone?phone=${phone}`
                              );
                              const output = document.getElementById("search-result");
                              if (output) {
                                 output.textContent = JSON.stringify(result, null, 2);
                              }
                           } catch (err) {
                              alert("Lỗi khi tìm kiếm");
                           }
                        }}
                        className="px-6 py-2 bg-[#006ce4] text-white font-bold rounded-lg hover:bg-[#004b9e] transition-colors"
                     >
                        Tìm kiếm
                     </button>
                  </div>

                  <div className="mt-4 p-4 bg-gray-900 rounded-lg overflow-x-auto">
                     <pre id="search-result" className="text-green-400 text-sm font-mono">
                        Kết quả tìm kiếm sẽ hiển thị tại đây...
                     </pre>
                  </div>
               </div>
            </div>
         </div>
      </main>
   );
}
