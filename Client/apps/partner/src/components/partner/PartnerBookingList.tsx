"use client";

import React, { useState } from "react";
import { PartnerBookingResponse } from "@/lib/api/services/partnerService";
import {
   Calendar,
   User,
   Mail,
   Phone,
   Building,
   MessageSquare,
   Search,
   ChevronLeft,
   ChevronRight,
   AlertCircle,
} from "lucide-react";
import PriceDisplay from "../PriceDisplay";
import { format } from "date-fns";

interface PartnerBookingListProps {
   initialBookings: PartnerBookingResponse[];
}

const ITEMS_PER_PAGE = 5;

export default function PartnerBookingList({ initialBookings }: PartnerBookingListProps) {
   const bookings = initialBookings;
   const [searchTerm, setSearchTerm] = useState("");
   const [currentPage, setCurrentPage] = useState(1);

   // Filter bookings by guest name, property name, or email
   const filteredBookings = bookings.filter((booking) => {
      const search = searchTerm.toLowerCase();
      return (
         booking.guestName.toLowerCase().includes(search) ||
         booking.guestEmail.toLowerCase().includes(search) ||
         booking.propertyName.toLowerCase().includes(search) ||
         booking.roomTypeName.toLowerCase().includes(search)
      );
   });

   // Pagination logic
   const totalPages = Math.ceil(filteredBookings.length / ITEMS_PER_PAGE);
   const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
   const visibleBookings = filteredBookings.slice(startIndex, startIndex + ITEMS_PER_PAGE);

   const getStatusBadge = (status: PartnerBookingResponse["status"]) => {
      const styles = {
         PENDING_PAYMENT: "bg-amber-50 text-amber-700 border-amber-200/60",
         CONFIRMED: "bg-emerald-50 text-emerald-700 border-emerald-200/60",
         CHECKED_IN: "bg-indigo-50 text-indigo-700 border-indigo-200/60",
         CHECKED_OUT: "bg-blue-50 text-blue-700 border-blue-200/60",
         CANCELLED: "bg-rose-50 text-rose-700 border-rose-200/60",
         EXPIRED: "bg-zinc-50 text-zinc-500 border-zinc-200",
         NO_SHOW: "bg-purple-50 text-purple-700 border-purple-200/60",
         REFUNDED: "bg-zinc-50 text-zinc-500 border-zinc-200",
      };

      const labels = {
         PENDING_PAYMENT: "Chờ thanh toán",
         CONFIRMED: "Đã xác nhận",
         CHECKED_IN: "Đã nhận phòng",
         CHECKED_OUT: "Đã trả phòng",
         CANCELLED: "Đã hủy",
         EXPIRED: "Hết hạn",
         NO_SHOW: "Vắng mặt",
         REFUNDED: "Đã hoàn tiền",
      };

      return (
         <span
            className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border ${styles[status] || styles.PENDING_PAYMENT}`}
         >
            <span
               className={`h-1.5 w-1.5 rounded-full ${
                  status === "CONFIRMED"
                     ? "bg-emerald-500"
                     : status === "PENDING_PAYMENT"
                       ? "bg-amber-500"
                       : status === "CHECKED_IN"
                         ? "bg-indigo-500"
                         : status === "CHECKED_OUT"
                           ? "bg-blue-500"
                           : status === "CANCELLED"
                             ? "bg-rose-500"
                             : status === "NO_SHOW"
                               ? "bg-purple-500"
                               : "bg-zinc-400"
               }`}
            />
            {labels[status] || status}
         </span>
      );
   };

   const formatBookingDate = (dateStr: string) => {
      try {
         return format(new Date(dateStr), "dd/MM/yyyy");
      } catch {
         return dateStr;
      }
   };

   return (
      <div className="space-y-6">
         {/* Search & Actions Bar */}
         <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 bg-white p-4 rounded-2xl border border-zinc-200/80 shadow-xs">
            <div className="relative flex-1 max-w-md">
               <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
               <input
                  type="text"
                  placeholder="Tìm theo tên khách, chỗ nghỉ, email..."
                  value={searchTerm}
                  onChange={(e) => {
                     setSearchTerm(e.target.value);
                     setCurrentPage(1);
                  }}
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:outline-none transition-colors"
               />
            </div>
            <div className="text-xs text-zinc-500 font-medium text-right self-center">
               Tổng số đặt phòng:{" "}
               <span className="font-bold text-zinc-800">{filteredBookings.length}</span>
            </div>
         </div>

         {/* Bookings Table / List Container */}
         {filteredBookings.length === 0 ? (
            <div className="flex flex-col items-center justify-center rounded-3xl border-2 border-dashed border-zinc-200 bg-white p-16 text-center">
               <div className="flex h-16 w-16 items-center justify-center rounded-full bg-zinc-50 text-zinc-400 border border-zinc-200">
                  <AlertCircle className="h-8 w-8" />
               </div>
               <h3 className="mt-4 text-lg font-bold text-zinc-900">
                  Không tìm thấy lượt đặt phòng nào
               </h3>
               <p className="mt-1 max-w-xs text-xs text-zinc-500 font-medium">
                  {searchTerm
                     ? "Thử thay đổi từ khóa tìm kiếm của bạn"
                     : "Chỗ nghỉ của bạn hiện chưa có lượt đặt phòng nào."}
               </p>
            </div>
         ) : (
            <div className="bg-white rounded-3xl border border-zinc-200/80 shadow-sm overflow-hidden">
               {/* Desktop Table View */}
               <div className="overflow-x-auto">
                  <table className="w-full border-collapse text-left text-sm text-zinc-700">
                     <thead>
                        <tr className="border-b border-zinc-100 bg-zinc-50/70 text-xs font-bold uppercase tracking-wider text-zinc-400">
                           <th className="px-6 py-4.5 text-center">Mã / Khách hàng</th>
                           <th className="px-6 py-4.5 text-center">Chỗ nghỉ & Loại phòng</th>
                           <th className="px-6 py-4.5 text-center">Thời gian lưu trú</th>
                           <th className="px-6 py-4.5 text-center">Số lượng</th>
                           <th className="px-6 py-4.5 text-center">Doanh thu</th>
                           <th className="px-6 py-4.5 text-center">Trạng thái</th>
                        </tr>
                     </thead>
                     <tbody className="divide-y divide-zinc-100">
                        {visibleBookings.map((booking) => (
                           <React.Fragment key={booking.id}>
                              <tr className="hover:bg-zinc-50/50 transition-colors">
                                 {/* Guest details */}
                                 <td className="px-6 py-5.5">
                                    <div className="flex flex-col gap-1 max-w-[200px]">
                                       <span className="font-bold text-zinc-900 flex items-center gap-1.5">
                                          <User className="h-3.5 w-3.5 text-zinc-400 shrink-0" />
                                          {booking.guestName}
                                       </span>
                                       <span className="text-xs text-zinc-500 flex items-center gap-1.5 truncate">
                                          <Mail className="h-3 w-3 text-zinc-400 shrink-0" />
                                          {booking.guestEmail}
                                       </span>
                                       {booking.guestPhone && (
                                          <span className="text-xs text-zinc-500 flex items-center gap-1.5">
                                             <Phone className="h-3 w-3 text-zinc-400 shrink-0" />
                                             {booking.guestPhone}
                                          </span>
                                       )}
                                    </div>
                                 </td>

                                 {/* Property Details */}
                                 <td className="px-6 py-5.5">
                                    <div className="flex flex-col gap-1">
                                       <span className="font-bold text-zinc-800 flex items-center gap-1.5">
                                          <Building className="h-3.5 w-3.5 text-[#006ce4] shrink-0" />
                                          {booking.propertyName}
                                       </span>
                                       <span className="text-xs text-zinc-500 font-semibold pl-5">
                                          {booking.roomTypeName}
                                       </span>
                                    </div>
                                 </td>

                                 {/* Stay Period */}
                                 <td className="px-6 py-5.5">
                                    <div className="flex flex-col gap-1">
                                       <span className="font-bold text-zinc-800 flex items-center gap-1.5">
                                          <Calendar className="h-3.5 w-3.5 text-zinc-400 shrink-0" />
                                          {formatBookingDate(booking.checkInDate)} —{" "}
                                          {formatBookingDate(booking.checkOutDate)}
                                       </span>
                                       <span className="text-xs text-zinc-500 pl-5">
                                          Đặt lúc: {formatBookingDate(booking.createdAt)}
                                       </span>
                                    </div>
                                 </td>

                                 {/* Num Rooms */}
                                 <td className="px-6 py-5.5 font-bold text-zinc-800">
                                    {booking.numRooms} phòng
                                 </td>

                                 {/* Pricing */}
                                 <td className="px-6 py-5.5">
                                    <PriceDisplay
                                       amount={booking.finalPrice}
                                       size="md"
                                       className="font-extrabold text-zinc-900"
                                    />
                                    <span className="block text-[10px] text-zinc-400 font-medium">
                                       Giá sau thuế
                                    </span>
                                 </td>

                                 {/* Status */}
                                 <td className="px-6 py-5.5">{getStatusBadge(booking.status)}</td>
                              </tr>

                              {/* Special requests row if exists */}
                              {booking.specialRequests && (
                                 <tr className="bg-zinc-50/30">
                                    <td colSpan={6} className="px-6 py-3 border-t-0">
                                       <div className="flex items-start gap-2 text-xs text-zinc-500 bg-amber-50/50 border border-amber-100/50 rounded-xl px-4 py-2.5">
                                          <MessageSquare className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
                                          <div>
                                             <span className="font-bold text-amber-800 block mb-0.5">
                                                Yêu cầu đặc biệt của khách:
                                             </span>
                                             <p className="italic text-zinc-600 font-medium">
                                                {booking.specialRequests}
                                             </p>
                                          </div>
                                       </div>
                                    </td>
                                 </tr>
                              )}
                           </React.Fragment>
                        ))}
                     </tbody>
                  </table>
               </div>

               {/* Pagination Footer */}
               {totalPages > 1 && (
                  <div className="flex items-center justify-between border-t border-zinc-100 px-6 py-4.5 bg-zinc-50/30">
                     <div className="text-xs font-medium text-zinc-500">
                        Hiển thị{" "}
                        <span className="font-bold text-zinc-800">
                           {startIndex + 1}-
                           {Math.min(startIndex + visibleBookings.length, filteredBookings.length)}
                        </span>{" "}
                        trên tổng số{" "}
                        <span className="font-bold text-zinc-800">{filteredBookings.length}</span>{" "}
                        lượt đặt
                     </div>
                     <div className="flex items-center gap-1.5">
                        <button
                           onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                           disabled={currentPage === 1}
                           className="flex h-9 w-9 items-center justify-center rounded-xl border border-zinc-200 bg-white text-zinc-600 hover:border-[#006ce4] hover:text-[#006ce4] disabled:opacity-30 disabled:hover:border-zinc-200 disabled:hover:text-zinc-600 transition-all cursor-pointer"
                        >
                           <ChevronLeft className="h-4 w-4" />
                        </button>
                        <div className="flex gap-1">
                           {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                              <button
                                 key={page}
                                 onClick={() => setCurrentPage(page)}
                                 className={`h-9 w-9 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                                    currentPage === page
                                       ? "bg-[#006ce4] text-white shadow-md shadow-blue-100"
                                       : "bg-white border border-zinc-200 text-zinc-600 hover:border-zinc-300"
                                 }`}
                              >
                                 {page}
                              </button>
                           ))}
                        </div>
                        <button
                           onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                           disabled={currentPage === totalPages}
                           className="flex h-9 w-9 items-center justify-center rounded-xl border border-zinc-200 bg-white text-zinc-600 hover:border-[#006ce4] hover:text-[#006ce4] disabled:opacity-30 disabled:hover:border-zinc-200 disabled:hover:text-zinc-600 transition-all cursor-pointer"
                        >
                           <ChevronRight className="h-4 w-4" />
                        </button>
                     </div>
                  </div>
               )}
            </div>
         )}
      </div>
   );
}
