"use client";

import { useTranslations } from "next-intl";
import { TrendingUp, Users, DollarSign, Star } from "lucide-react";

export default function PropertyStats() {
   const t = useTranslations("Partner.propertyDetail.stats");

   // Giả lập một vài thông số đẹp mắt và trực quan cho đối tác quản lý
   const stats = [
      {
         name: t("occupancyRate"),
         value: "78.4%",
         change: "+4.2%",
         isPositive: true,
         icon: TrendingUp,
         colorClass: "text-[#003580] bg-[#003580]/5 border-[#003580]/10",
      },
      {
         name: t("monthlyBookings"),
         value: "142",
         change: "+12%",
         isPositive: true,
         icon: Users,
         colorClass: "text-[#003580] bg-[#003580]/5 border-[#003580]/10",
      },
      {
         name: t("estimatedRevenue"),
         value: "$14,820",
         change: "+8.5%",
         isPositive: true,
         icon: DollarSign,
         colorClass: "text-[#003580] bg-[#003580]/5 border-[#003580]/10",
      },
      {
         name: t("ratingScore"),
         value: "4.8 / 5",
         change: "92 đánh giá",
         isPositive: true,
         icon: Star,
         colorClass: "text-[#003580] bg-[#003580]/5 border-[#003580]/10",
      },
   ];

   return (
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4 mb-8">
         {stats.map((stat, idx) => (
            <div
               key={idx}
               className="rounded-3xl border border-zinc-100 bg-white p-6 shadow-xs transition-all hover:shadow-md"
            >
               <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-zinc-400 uppercase tracking-wider">
                     {stat.name}
                  </span>
                  <div
                     className={`flex h-10 w-10 items-center justify-center rounded-xl border ${stat.colorClass}`}
                  >
                     <stat.icon className="h-5 w-5" />
                  </div>
               </div>
               <div className="mt-4 flex items-center justify-between">
                  <span className="text-2xl font-bold text-zinc-950">{stat.value}</span>
                  <span
                     className={`text-[11px] font-semibold px-2 py-0.5 rounded-lg border ${
                        stat.name === t("ratingScore")
                           ? "text-zinc-500 bg-zinc-50 border-zinc-100"
                           : "text-emerald-700 bg-emerald-50 border-emerald-100"
                     }`}
                  >
                     {stat.change}
                  </span>
               </div>
            </div>
         ))}
      </div>
   );
}
