"use client";

import { Settings, Save, ShieldAlert, KeyRound } from "lucide-react";
import DashboardSidebar from "@/components/owner/DashboardSidebar";
import DashboardHeader from "@/components/owner/DashboardHeader";

export default function SettingsPage() {
   return (
      <div className="min-h-screen bg-zinc-50/50 font-sans">
         <DashboardSidebar />

         <main className="lg:pl-64">
            <DashboardHeader />

            <div className="p-8 max-w-7xl mx-auto">
               <div className="mb-10 flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
                  <div>
                     <h1 className="text-3xl font-black tracking-tight text-zinc-900">
                        Cấu hình hệ thống
                     </h1>
                     <p className="mt-1 text-zinc-500 font-medium">
                        Quản lý tham số hệ thống, cổng thanh toán, API Keys và bảo mật.
                     </p>
                  </div>
                  <button className="inline-flex items-center gap-1.5 rounded-xl bg-rose-600 px-4 py-2.5 text-sm font-bold text-white shadow-xs hover:bg-rose-700 transition-all cursor-pointer">
                     <Save className="h-4 w-4" /> Lưu cấu hình
                  </button>
               </div>

               <div className="grid gap-6 md:grid-cols-3">
                  <div className="md:col-span-2 space-y-6">
                     {/* General config */}
                     <div className="bg-white p-6 rounded-3xl border border-zinc-150 shadow-2xs">
                        <h3 className="text-lg font-bold text-zinc-900 mb-4 flex items-center gap-2">
                           <Settings className="h-5 w-5 text-zinc-400" /> Cài đặt chung
                        </h3>
                        <div className="space-y-4">
                           <div>
                              <label className="block text-xs font-bold text-zinc-400 uppercase tracking-wider mb-2">
                                 Tên cổng dịch vụ
                              </label>
                              <input
                                 type="text"
                                 defaultValue="OmniBooking Portal"
                                 className="w-full rounded-xl border border-zinc-200 py-2.5 px-4 text-sm outline-none focus:border-rose-500 transition-all"
                              />
                           </div>
                           <div>
                              <label className="block text-xs font-bold text-zinc-400 uppercase tracking-wider mb-2">
                                 Tỷ lệ hoa hồng đối tác (%)
                              </label>
                              <input
                                 type="number"
                                 defaultValue="10"
                                 className="w-full rounded-xl border border-zinc-200 py-2.5 px-4 text-sm outline-none focus:border-rose-500 transition-all"
                              />
                           </div>
                        </div>
                     </div>

                     {/* API Keys */}
                     <div className="bg-white p-6 rounded-3xl border border-zinc-150 shadow-2xs">
                        <h3 className="text-lg font-bold text-zinc-900 mb-4 flex items-center gap-2">
                           <KeyRound className="h-5 w-5 text-zinc-400" /> API Keys & Integrations
                        </h3>
                        <div className="space-y-4">
                           <div>
                              <label className="block text-xs font-bold text-zinc-400 uppercase tracking-wider mb-2">
                                 VietMap API Key
                              </label>
                              <input
                                 type="password"
                                 value="••••••••••••••••••••••••••••••••••••••••"
                                 readOnly
                                 className="w-full rounded-xl border border-zinc-200 bg-zinc-50/50 py-2.5 px-4 text-sm outline-none cursor-not-allowed"
                              />
                           </div>
                           <div>
                              <label className="block text-xs font-bold text-zinc-400 uppercase tracking-wider mb-2">
                                 Goong Map API Key
                              </label>
                              <input
                                 type="password"
                                 value="••••••••••••••••••••••••••••••••••••••••"
                                 readOnly
                                 className="w-full rounded-xl border border-zinc-200 bg-zinc-50/50 py-2.5 px-4 text-sm outline-none cursor-not-allowed"
                              />
                           </div>
                        </div>
                     </div>
                  </div>

                  {/* Sidebar stats info */}
                  <div className="space-y-6">
                     <div className="bg-rose-50/50 border border-rose-100 p-6 rounded-3xl">
                        <h3 className="text-sm font-bold text-rose-800 flex items-center gap-2">
                           <ShieldAlert className="h-5 w-5 text-rose-600" /> Vùng bảo mật cao
                        </h3>
                        <p className="text-xs text-rose-700 mt-2 leading-relaxed font-medium">
                           Các thay đổi cấu hình này ảnh hưởng trực tiếp đến dòng tiền, tỷ lệ chiết
                           khấu và tích hợp dịch vụ bên thứ ba trên toàn hệ thống OmniBooking. Vui
                           lòng xác thực đa yếu tố (2FA) trước khi sửa đổi.
                        </p>
                     </div>
                  </div>
               </div>
            </div>
         </main>
      </div>
   );
}
