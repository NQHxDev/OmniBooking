"use client";

import { Search, Filter, ShieldCheck, Mail, Calendar, UserX } from "lucide-react";
import DashboardSidebar from "@/components/owner/DashboardSidebar";
import DashboardHeader from "@/components/owner/DashboardHeader";

export default function UsersManagementPage() {
   const mockUsers = [
      {
         id: "u-01",
         name: "Nguyễn Văn A",
         email: "vana@gmail.com",
         role: "ROLE_PARTNER",
         status: "Active",
         joined: "2026-01-15",
      },
      {
         id: "u-02",
         name: "Trần Thị B",
         email: "thib@gmail.com",
         role: "ROLE_PARTNER",
         status: "Active",
         joined: "2026-02-10",
      },
      {
         id: "u-03",
         name: "Lê Hoàng C",
         email: "hoangc@gmail.com",
         role: "ROLE_USER",
         status: "Active",
         joined: "2026-03-01",
      },
      {
         id: "u-04",
         name: "Phạm Minh D",
         email: "minhd@gmail.com",
         role: "ROLE_ADMIN",
         status: "Active",
         joined: "2025-12-01",
      },
      {
         id: "u-05",
         name: "Vũ Hoàng E",
         email: "hoange@gmail.com",
         role: "ROLE_USER",
         status: "Suspended",
         joined: "2026-04-12",
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
                     Quản lý người dùng
                  </h1>
                  <p className="mt-1 text-zinc-500 font-medium">
                     Tìm kiếm, phân quyền và khóa tài khoản người dùng hệ thống.
                  </p>
               </div>

               {/* Filters */}
               <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between bg-white p-4 rounded-2xl border border-zinc-150/70 shadow-2xs">
                  <div className="relative flex-1 max-w-md">
                     <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400" />
                     <input
                        type="text"
                        placeholder="Tìm theo tên hoặc email..."
                        className="w-full rounded-xl border border-zinc-200 bg-zinc-50/30 py-2 pl-10 pr-4 text-sm outline-none focus:border-rose-500 focus:ring-4 focus:ring-rose-50 transition-all"
                     />
                  </div>
                  <div className="flex gap-2">
                     <button className="inline-flex items-center gap-1.5 rounded-xl border border-zinc-200 bg-white px-4 py-2 text-sm font-bold text-zinc-600 hover:bg-zinc-50 transition-all cursor-pointer">
                        <Filter className="h-4 w-4" /> Bộ lọc
                     </button>
                  </div>
               </div>

               {/* Users Table */}
               <div className="overflow-hidden rounded-3xl border border-zinc-150 bg-white shadow-xs">
                  <div className="overflow-x-auto">
                     <table className="w-full border-collapse text-left text-sm text-zinc-500">
                        <thead className="bg-zinc-50/75 border-b border-zinc-100 text-xs font-bold uppercase tracking-wider text-zinc-400">
                           <tr>
                              <th scope="col" className="px-6 py-4">
                                 Hội viên
                              </th>
                              <th scope="col" className="px-6 py-4">
                                 Vai trò
                              </th>
                              <th scope="col" className="px-6 py-4">
                                 Ngày tham gia
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
                           {mockUsers.map((user) => (
                              <tr key={user.id} className="hover:bg-zinc-50/50 transition-colors">
                                 <td className="px-6 py-4">
                                    <div className="flex items-center gap-3">
                                       <div className="h-9 w-9 rounded-full bg-linear-to-tr from-zinc-200 to-zinc-100 flex items-center justify-center font-bold text-zinc-600 text-xs">
                                          {user.name.charAt(0)}
                                       </div>
                                       <div className="flex flex-col">
                                          <span className="text-sm font-bold text-zinc-900">
                                             {user.name}
                                          </span>
                                          <span className="text-xs text-zinc-400 flex items-center gap-1 mt-0.5">
                                             <Mail className="h-3 w-3" /> {user.email}
                                          </span>
                                       </div>
                                    </div>
                                 </td>
                                 <td className="px-6 py-4">
                                    <span
                                       className={`inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-semibold ${
                                          user.role.includes("ADMIN")
                                             ? "bg-red-50 text-red-600"
                                             : user.role.includes("PARTNER")
                                               ? "bg-blue-50 text-blue-600"
                                               : "bg-zinc-100 text-zinc-650"
                                       }`}
                                    >
                                       {user.role}
                                    </span>
                                 </td>
                                 <td className="px-6 py-4 text-zinc-400 flex items-center gap-1.5 py-6">
                                    <Calendar className="h-3.5 w-3.5" /> {user.joined}
                                 </td>
                                 <td className="px-6 py-4">
                                    <span
                                       className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold ${
                                          user.status === "Active"
                                             ? "bg-emerald-50 text-emerald-700"
                                             : "bg-zinc-100 text-zinc-600"
                                       }`}
                                    >
                                       {user.status}
                                    </span>
                                 </td>
                                 <td className="px-6 py-4 text-right">
                                    <div className="flex justify-end gap-2">
                                       <button className="inline-flex items-center gap-1 rounded-xl border border-zinc-200 bg-white px-3 py-1.5 text-xs font-bold text-zinc-600 hover:bg-zinc-50 transition-all cursor-pointer">
                                          <ShieldCheck className="h-3.5 w-3.5" /> Phân quyền
                                       </button>
                                       <button className="inline-flex items-center gap-1 rounded-xl border border-rose-250/70 bg-rose-50 px-3 py-1.5 text-xs font-bold text-rose-600 hover:bg-rose-100 transition-all cursor-pointer">
                                          <UserX className="h-3.5 w-3.5" /> Khóa
                                       </button>
                                    </div>
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
