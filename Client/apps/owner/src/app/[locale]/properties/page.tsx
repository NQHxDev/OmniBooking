"use client";

import { Building2, Search, Filter, CheckCircle2, Clock, XCircle } from "lucide-react";
import DashboardSidebar from "@/components/owner/DashboardSidebar";
import DashboardHeader from "@/components/owner/DashboardHeader";

export default function PropertiesAdminPage() {
   const mockProperties = [
      {
         id: "p-01",
         name: "Grand Mercure Da Nang",
         type: "Hotel",
         owner: "Nguyễn Văn A",
         status: "Approved",
         date: "2026-05-10",
      },
      {
         id: "p-02",
         name: "Dalat Pine Hill Villa",
         type: "Villa",
         owner: "Trần Thị B",
         status: "Approved",
         date: "2026-05-18",
      },
      {
         id: "p-03",
         name: "Saigon Sky Loft Studio",
         type: "Apartment",
         owner: "Lê Hoàng C",
         status: "Pending",
         date: "2026-06-02",
      },
      {
         id: "p-04",
         name: "Nha Trang Beachside Resort",
         type: "Resort",
         owner: "Phạm Văn D",
         status: "Rejected",
         date: "2026-05-25",
      },
   ];

   return (
      <div className="min-h-screen bg-zinc-50/50 font-sans">
         <DashboardSidebar />

         <main className="lg:pl-64">
            <DashboardHeader />

            <div className="p-8 max-w-7xl mx-auto">
               <div className="mb-10">
                  <h1 className="text-3xl font-black tracking-tight text-zinc-900">
                     Duyệt chỗ nghỉ
                  </h1>
                  <p className="mt-1 text-zinc-500 font-medium">
                     Xem xét thông tin đăng ký cơ sở lưu trú và tài liệu pháp lý từ các đối tác.
                  </p>
               </div>

               {/* Filters */}
               <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between bg-white p-4 rounded-2xl border border-zinc-150/70 shadow-2xs">
                  <div className="relative flex-1 max-w-md">
                     <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400" />
                     <input
                        type="text"
                        placeholder="Tìm kiếm chỗ nghỉ hoặc tên đối tác..."
                        className="w-full rounded-xl border border-zinc-200 bg-zinc-50/30 py-2 pl-10 pr-4 text-sm outline-none focus:border-rose-500 focus:ring-4 focus:ring-rose-50 transition-all"
                     />
                  </div>
                  <div className="flex gap-2">
                     <button className="inline-flex items-center gap-1.5 rounded-xl border border-zinc-200 bg-white px-4 py-2 text-sm font-bold text-zinc-600 hover:bg-zinc-50 transition-all cursor-pointer">
                        <Filter className="h-4 w-4" /> Bộ lọc
                     </button>
                  </div>
               </div>

               {/* Properties Table */}
               <div className="overflow-hidden rounded-3xl border border-zinc-150 bg-white shadow-xs">
                  <div className="overflow-x-auto">
                     <table className="w-full border-collapse text-left text-sm text-zinc-500">
                        <thead className="bg-zinc-50/75 border-b border-zinc-100 text-xs font-bold uppercase tracking-wider text-zinc-400">
                           <tr>
                              <th scope="col" className="px-6 py-4">
                                 Chỗ nghỉ
                              </th>
                              <th scope="col" className="px-6 py-4">
                                 Loại hình
                              </th>
                              <th scope="col" className="px-6 py-4">
                                 Ngày đăng ký
                              </th>
                              <th scope="col" className="px-6 py-4">
                                 Trạng thái
                              </th>
                              <th scope="col" className="px-6 py-4 text-right">
                                 Hành động
                              </th>
                           </tr>
                        </thead>
                        <tbody className="divide-y divide-zinc-100 font-medium">
                           {mockProperties.map((prop) => (
                              <tr key={prop.id} className="hover:bg-zinc-50/50 transition-colors">
                                 <td className="px-6 py-4">
                                    <div className="flex items-center gap-3">
                                       <div className="h-9 w-9 rounded-full bg-linear-to-tr from-zinc-200 to-zinc-100 flex items-center justify-center text-zinc-500">
                                          <Building2 className="h-5 w-5" />
                                       </div>
                                       <div className="flex flex-col">
                                          <span className="text-sm font-bold text-zinc-900">
                                             {prop.name}
                                          </span>
                                          <span className="text-xs text-zinc-400 mt-0.5">
                                             Đối tác: {prop.owner}
                                          </span>
                                       </div>
                                    </div>
                                 </td>
                                 <td className="px-6 py-4">
                                    <span className="inline-flex items-center rounded-md bg-zinc-100 px-2 py-1 text-xs font-semibold text-zinc-650">
                                       {prop.type}
                                    </span>
                                 </td>
                                 <td className="px-6 py-4 text-zinc-400">{prop.date}</td>
                                 <td className="px-6 py-4">
                                    <span
                                       className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-bold ${
                                          prop.status === "Approved"
                                             ? "bg-emerald-50 text-emerald-700"
                                             : prop.status === "Pending"
                                               ? "bg-amber-50 text-amber-700"
                                               : "bg-rose-50 text-rose-700"
                                       }`}
                                    >
                                       {prop.status === "Approved" && (
                                          <CheckCircle2 className="h-3 w-3" />
                                       )}
                                       {prop.status === "Pending" && <Clock className="h-3 w-3" />}
                                       {prop.status === "Rejected" && (
                                          <XCircle className="h-3 w-3" />
                                       )}
                                       {prop.status}
                                    </span>
                                 </td>
                                 <td className="px-6 py-4 text-right">
                                    <button className="text-xs font-bold text-rose-600 hover:underline cursor-pointer">
                                       Chi tiết hồ sơ
                                    </button>
                                 </td>
                              </tr>
                           ))}
                        </tbody>
                     </table>
                  </div>
               </div>
            </div>
         </main>
      </div>
   );
}
