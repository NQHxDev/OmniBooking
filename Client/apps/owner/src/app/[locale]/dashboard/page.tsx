"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
   Users,
   Building2,
   Wallet,
   CalendarCheck,
   Check,
   X,
   ExternalLink,
   Clock,
   TrendingUp,
   TrendingDown,
} from "lucide-react";
import { toast } from "sonner";
import DashboardSidebar from "@/components/owner/DashboardSidebar";
import DashboardHeader from "@/components/owner/DashboardHeader";

interface PendingProperty {
   id: string;
   name: string;
   type: string;
   location: string;
   partnerName: string;
   registeredDate: string;
}

export default function OwnerDashboard() {
   const t = useTranslations("Owner.dashboard");

   // Interactive state for pending review list
   const [pendingProperties, setPendingProperties] = useState<PendingProperty[]>([
      {
         id: "prop-01",
         name: "Ocean View Luxury Resort",
         type: "Resort",
         location: "Đà Nẵng, Việt Nam",
         partnerName: "Nguyễn Văn A",
         registeredDate: "2026-06-01",
      },
      {
         id: "prop-02",
         name: "Dalat Cozy Pine Homestay",
         type: "Homestay",
         location: "Đà Lạt, Lâm Đồng",
         partnerName: "Trần Thị B",
         registeredDate: "2026-06-02",
      },
      {
         id: "prop-03",
         name: "Saigon Sky Studio Loft",
         type: "Apartment",
         location: "Quận 1, TP. Hồ Chí Minh",
         partnerName: "Lê Hoàng C",
         registeredDate: "2026-06-02",
      },
   ]);

   const [stats, setStats] = useState({
      users: 12458,
      properties: 1842,
      revenue: 482950,
      bookings: 3240,
   });

   const handleApprove = (id: string, name: string) => {
      setPendingProperties((prev) => prev.filter((p) => p.id !== id));
      setStats((prev) => ({
         ...prev,
         properties: prev.properties + 1,
      }));
      toast.success(t("pendingApproval.approvedSuccess") || "Đã phê duyệt cơ sở!", {
         description: `${name} hiện đã hoạt động trên hệ thống.`,
         icon: <Check className="h-4 w-4 text-emerald-500" />,
      });
   };

   const handleReject = (id: string, name: string) => {
      setPendingProperties((prev) => prev.filter((p) => p.id !== id));
      toast.error(t("pendingApproval.rejectedSuccess") || "Đã từ chối cơ sở!", {
         description: `Đã gửi thông báo từ chối cho chủ sở hữu ${name}.`,
         icon: <X className="h-4 w-4 text-rose-500" />,
      });
   };

   return (
      <div className="min-h-screen bg-zinc-50/50 font-sans">
         <DashboardSidebar />

         <main className="lg:pl-64">
            <DashboardHeader />

            <div className="p-8 max-w-7xl mx-auto">
               {/* Welcome & Info */}
               <div className="mb-10 animate-in fade-in slide-in-from-top-4 duration-300">
                  <h1 className="text-3xl font-black tracking-tight text-zinc-900">{t("title")}</h1>
                  <p className="mt-1.5 text-zinc-500 font-medium">{t("welcome")}</p>
               </div>

               {/* Stats Grid */}
               <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4 animate-in fade-in slide-in-from-bottom-4 duration-500 delay-100">
                  {/* Total Users */}
                  <div className="group relative rounded-3xl border border-zinc-150/70 bg-white p-6 shadow-sm hover:shadow-md transition-all duration-300">
                     <div className="flex items-center justify-between">
                        <div className="rounded-2xl bg-blue-50 p-3 text-blue-600 transition-colors group-hover:bg-blue-600 group-hover:text-white duration-300">
                           <Users className="h-6 w-6" />
                        </div>
                        <span className="flex items-center gap-1 text-[11px] font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full">
                           <TrendingUp className="h-3 w-3" /> +12%
                        </span>
                     </div>
                     <div className="mt-4">
                        <p className="text-sm font-semibold text-zinc-400 uppercase tracking-wider">
                           {t("stats.totalUsers")}
                        </p>
                        <h3 className="text-3xl font-black text-zinc-950 mt-1">
                           {stats.users.toLocaleString()}
                        </h3>
                     </div>
                  </div>

                  {/* Total Properties */}
                  <div className="group relative rounded-3xl border border-zinc-150/70 bg-white p-6 shadow-sm hover:shadow-md transition-all duration-300">
                     <div className="flex items-center justify-between">
                        <div className="rounded-2xl bg-rose-50 p-3 text-rose-600 transition-colors group-hover:bg-rose-600 group-hover:text-white duration-300">
                           <Building2 className="h-6 w-6" />
                        </div>
                        {pendingProperties.length > 0 && (
                           <span className="flex items-center gap-1 text-[11px] font-bold text-rose-600 bg-rose-50 px-2.5 py-1 rounded-full animate-pulse">
                              <Clock className="h-3 w-3" /> {pendingProperties.length} pending
                           </span>
                        )}
                     </div>
                     <div className="mt-4">
                        <p className="text-sm font-semibold text-zinc-400 uppercase tracking-wider">
                           {t("stats.totalProperties")}
                        </p>
                        <h3 className="text-3xl font-black text-zinc-950 mt-1">
                           {stats.properties.toLocaleString()}
                        </h3>
                     </div>
                  </div>

                  {/* System Revenue */}
                  <div className="group relative rounded-3xl border border-zinc-150/70 bg-white p-6 shadow-sm hover:shadow-md transition-all duration-300">
                     <div className="flex items-center justify-between">
                        <div className="rounded-2xl bg-emerald-50 p-3 text-emerald-600 transition-colors group-hover:bg-emerald-600 group-hover:text-white duration-300">
                           <Wallet className="h-6 w-6" />
                        </div>
                        <span className="flex items-center gap-1 text-[11px] font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full">
                           <TrendingUp className="h-3 w-3" /> +8.4%
                        </span>
                     </div>
                     <div className="mt-4">
                        <p className="text-sm font-semibold text-zinc-400 uppercase tracking-wider">
                           {t("stats.systemRevenue")}
                        </p>
                        <h3 className="text-3xl font-black text-zinc-950 mt-1">
                           ${stats.revenue.toLocaleString()}
                        </h3>
                     </div>
                  </div>

                  {/* Active Bookings */}
                  <div className="group relative rounded-3xl border border-zinc-150/70 bg-white p-6 shadow-sm hover:shadow-md transition-all duration-300">
                     <div className="flex items-center justify-between">
                        <div className="rounded-2xl bg-violet-50 p-3 text-violet-600 transition-colors group-hover:bg-violet-600 group-hover:text-white duration-300">
                           <CalendarCheck className="h-6 w-6" />
                        </div>
                        <span className="flex items-center gap-1 text-[11px] font-bold text-rose-600 bg-rose-50 px-2.5 py-1 rounded-full">
                           <TrendingDown className="h-3 w-3" /> -1.2%
                        </span>
                     </div>
                     <div className="mt-4">
                        <p className="text-sm font-semibold text-zinc-400 uppercase tracking-wider">
                           {t("stats.activeBookings")}
                        </p>
                        <h3 className="text-3xl font-black text-zinc-950 mt-1">
                           {stats.bookings.toLocaleString()}
                        </h3>
                     </div>
                  </div>
               </div>

               {/* Main section: Approvals */}
               <div className="mt-12 animate-in fade-in slide-in-from-bottom-4 duration-500 delay-200">
                  <div className="mb-6 flex items-center justify-between px-2">
                     <div>
                        <h2 className="text-xl font-bold text-zinc-900">
                           {t("pendingApproval.title")}
                        </h2>
                        <p className="text-sm text-zinc-500 mt-1">
                           {t("pendingApproval.subtitle")}
                        </p>
                     </div>
                     <button className="text-sm font-bold text-rose-600 hover:underline hover:text-rose-700 cursor-pointer">
                        {t("pendingApproval.viewAll")}
                     </button>
                  </div>

                  {/* Approvals Table */}
                  <div className="overflow-hidden rounded-3xl border border-zinc-150 bg-white shadow-xs">
                     {pendingProperties.length === 0 ? (
                        <div className="flex flex-col items-center justify-center p-12 text-center">
                           <div className="flex h-16 w-16 items-center justify-center rounded-full bg-emerald-50 text-emerald-600 mb-4">
                              <Check className="h-8 w-8" />
                           </div>
                           <h3 className="text-lg font-bold text-zinc-900">Hoàn thành nhiệm vụ!</h3>
                           <p className="text-zinc-500 text-sm mt-1 max-w-sm">
                              Tất cả các cơ sở lưu trú đăng ký mới đã được kiểm duyệt và xử lý thành
                              công.
                           </p>
                        </div>
                     ) : (
                        <div className="overflow-x-auto">
                           <table className="w-full border-collapse text-left text-sm text-zinc-500">
                              <thead className="bg-zinc-50/75 border-b border-zinc-100 text-xs font-bold uppercase tracking-wider text-zinc-400">
                                 <tr>
                                    <th scope="col" className="px-6 py-4">
                                       {t("pendingApproval.propertyColumn")}
                                    </th>
                                    <th scope="col" className="px-6 py-4">
                                       {t("pendingApproval.typeColumn")}
                                    </th>
                                    <th scope="col" className="px-6 py-4">
                                       {t("pendingApproval.dateColumn")}
                                    </th>
                                    <th scope="col" className="px-6 py-4 text-right">
                                       {t("pendingApproval.actionColumn")}
                                    </th>
                                 </tr>
                              </thead>
                              <tbody className="divide-y divide-zinc-100 font-medium">
                                 {pendingProperties.map((prop) => (
                                    <tr
                                       key={prop.id}
                                       className="hover:bg-zinc-50/50 transition-colors"
                                    >
                                       <td className="px-6 py-4">
                                          <div className="flex flex-col">
                                             <span className="text-sm font-bold text-zinc-900 flex items-center gap-1.5">
                                                {prop.name}
                                                <button
                                                   title="Xem hồ sơ pháp lý"
                                                   className="text-zinc-400 hover:text-rose-500 transition-colors cursor-pointer"
                                                >
                                                   <ExternalLink className="h-3.5 w-3.5" />
                                                </button>
                                             </span>
                                             <span className="text-[11px] text-zinc-400 mt-0.5">
                                                Đối tác: {prop.partnerName} • Vị trí:{" "}
                                                {prop.location}
                                             </span>
                                          </div>
                                       </td>
                                       <td className="px-6 py-4">
                                          <span className="inline-flex items-center rounded-md bg-zinc-100 px-2 py-1 text-xs font-semibold text-zinc-600">
                                             {prop.type}
                                          </span>
                                       </td>
                                       <td className="px-6 py-4 text-zinc-400">
                                          {prop.registeredDate}
                                       </td>
                                       <td className="px-6 py-4 text-right">
                                          <div className="flex justify-end gap-2">
                                             <button
                                                onClick={() => handleReject(prop.id, prop.name)}
                                                className="inline-flex items-center gap-1 rounded-xl border border-zinc-200 bg-white px-3 py-1.5 text-xs font-bold text-zinc-600 hover:bg-rose-50 hover:text-rose-600 hover:border-rose-200 transition-all cursor-pointer"
                                             >
                                                <X className="h-3.5 w-3.5" />
                                                {t("pendingApproval.rejectBtn")}
                                             </button>
                                             <button
                                                onClick={() => handleApprove(prop.id, prop.name)}
                                                className="inline-flex items-center gap-1 rounded-xl bg-rose-600 px-3.5 py-1.5 text-xs font-bold text-white shadow-xs hover:bg-rose-700 active:scale-[0.98] transition-all cursor-pointer"
                                             >
                                                <Check className="h-3.5 w-3.5" />
                                                {t("pendingApproval.approveBtn")}
                                             </button>
                                          </div>
                                       </td>
                                    </tr>
                                 ))}
                              </tbody>
                           </table>
                        </div>
                     )}
                  </div>
               </div>
            </div>
         </main>
      </div>
   );
}
