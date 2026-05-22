"use client";

import { TrendingUp, Users, DollarSign, Star, ArrowUpRight, ArrowDownRight } from "lucide-react";
import { useTranslations, useLocale } from "next-intl";
import { type PartnerStatsResponse } from "@/lib/api/services/partnerService";

interface DashboardStatsProps {
   stats: PartnerStatsResponse | null;
}

export default function DashboardStats({ stats }: DashboardStatsProps) {
   const t = useTranslations("Partner.dashboard.stats");
   const locale = useLocale();

   const statsConfig = [
      {
         labelKey: "monthlyRevenue",
         value: stats?.monthlyRevenue ?? "$0",
         change: stats?.monthlyRevenueChange ?? "0.0%",
         isUp: stats?.monthlyRevenueUp ?? true,
         icon: DollarSign,
      },
      {
         labelKey: "totalBookings",
         value: stats?.totalBookings ?? "0",
         change: stats?.totalBookingsChange ?? "0.0%",
         isUp: stats?.totalBookingsUp ?? true,
         icon: TrendingUp,
      },
      {
         labelKey: "newCustomers",
         value: stats?.newCustomers ?? "0",
         change: stats?.newCustomersChange ?? "0.0%",
         isUp: stats?.newCustomersUp ?? true,
         icon: Users,
      },
      {
         labelKey: "ratingScore",
         value: stats?.ratingScore ?? "4.9",
         change: stats?.ratingScoreChange ?? "+0.0%",
         isUp: stats?.ratingScoreUp ?? true,
         icon: Star,
      },
   ];

   const formatValue = (val: string) => {
      if (locale === "vi") {
         return val.replace(/\./g, ",");
      }
      return val;
   };

   return (
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
         {statsConfig.map((stat) => (
            <div
               key={stat.labelKey}
               className="group cursor-pointer rounded-3xl bg-white p-6 border border-zinc-100 shadow-sm hover:shadow-md hover:border-zinc-200 active:scale-[0.99] transition-all duration-300"
            >
               <div className="flex items-center justify-between mb-4">
                  <div className="rounded-2xl bg-zinc-900 text-zinc-50 border border-zinc-800 p-3 transition-transform group-hover:scale-110 duration-300">
                     <stat.icon className="h-6 w-6" />
                  </div>
                  <div
                     className={`flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-bold border ${
                        stat.isUp
                           ? "bg-emerald-50/60 text-emerald-700 border-emerald-100"
                           : "bg-rose-50/60 text-rose-700 border-rose-100"
                     }`}
                  >
                     {stat.isUp ? (
                        <ArrowUpRight className="h-3 w-3 stroke-[2.5]" />
                     ) : (
                        <ArrowDownRight className="h-3 w-3 stroke-[2.5]" />
                     )}
                     {formatValue(stat.change)}
                  </div>
               </div>
               <div>
                  <p className="text-sm font-bold text-zinc-400">{t(stat.labelKey)}</p>
                  <p className="text-2xl font-black text-zinc-900 mt-1">
                     {formatValue(stat.value)}
                  </p>
               </div>
            </div>
         ))}
      </div>
   );
}
