"use client";

import { BarChart3, Download, ArrowUpRight } from "lucide-react";
import DashboardSidebar from "@/components/owner/DashboardSidebar";
import DashboardHeader from "@/components/owner/DashboardHeader";

export default function ReportsPage() {
   return (
      <div className="min-h-screen bg-zinc-50/50 font-sans">
         <DashboardSidebar />

         <main className="lg:pl-64">
            <DashboardHeader />

            <div className="p-8 max-w-7xl mx-auto">
               <div className="mb-10 flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
                  <div>
                     <h1 className="text-3xl font-black tracking-tight text-zinc-900">
                        Báo cáo doanh thu
                     </h1>
                     <p className="mt-1 text-zinc-500 font-medium">
                        Theo dõi hiệu suất tài chính, hoa hồng đối tác và dòng tiền giao dịch.
                     </p>
                  </div>
                  <button className="inline-flex items-center gap-1.5 rounded-xl bg-rose-600 px-4 py-2.5 text-sm font-bold text-white shadow-xs hover:bg-rose-700 transition-all cursor-pointer">
                     <Download className="h-4 w-4" /> Xuất báo cáo (CSV)
                  </button>
               </div>

               {/* Stats Overview */}
               <div className="grid gap-6 md:grid-cols-3 mb-8">
                  <div className="bg-white p-6 rounded-3xl border border-zinc-150/70 shadow-2xs">
                     <p className="text-sm font-semibold text-zinc-400 uppercase tracking-wider">
                        Tổng doanh thu hệ thống
                     </p>
                     <h3 className="text-3xl font-black text-zinc-950 mt-1">$482,950</h3>
                     <span className="flex items-center gap-1 text-xs font-bold text-emerald-600 mt-2">
                        <ArrowUpRight className="h-3 w-3" /> +8.4% so với tháng trước
                     </span>
                  </div>
                  <div className="bg-white p-6 rounded-3xl border border-zinc-150/70 shadow-2xs">
                     <p className="text-sm font-semibold text-zinc-400 uppercase tracking-wider">
                        Doanh thu hoa hồng (10%)
                     </p>
                     <h3 className="text-3xl font-black text-zinc-950 mt-1">$48,295</h3>
                     <span className="flex items-center gap-1 text-xs font-bold text-emerald-600 mt-2">
                        <ArrowUpRight className="h-3 w-3" /> +10.2% so với tháng trước
                     </span>
                  </div>
                  <div className="bg-white p-6 rounded-3xl border border-zinc-150/70 shadow-2xs">
                     <p className="text-sm font-semibold text-zinc-400 uppercase tracking-wider">
                        Giá trị giao dịch trung bình
                     </p>
                     <h3 className="text-3xl font-black text-zinc-950 mt-1">$149.50</h3>
                     <span className="flex items-center gap-1 text-xs font-bold text-rose-600 mt-2">
                        -0.5% so với tháng trước
                     </span>
                  </div>
               </div>

               {/* Chart Placeholder */}
               <div className="bg-white p-8 rounded-3xl border border-zinc-150 shadow-xs flex flex-col items-center justify-center min-h-80 text-center">
                  <BarChart3 className="h-16 w-16 text-zinc-350 mb-4" />
                  <h3 className="text-lg font-bold text-zinc-900">Biểu đồ doanh thu hệ thống</h3>
                  <p className="text-zinc-500 text-sm mt-1 max-w-md">
                     Mô đun phân tích biểu đồ doanh thu chi tiết đang được đồng bộ hóa dữ liệu thời
                     gian thực từ Spring Boot.
                  </p>
               </div>
            </div>
         </main>
      </div>
   );
}
