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
               className="group cursor-pointer rounded-3xl bg-white p-6 border border-zinc-100 shadow-xs hover:shadow-md hover:border-zinc-200 active:scale-[0.99] transition-all duration-300"
            >
               <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-zinc-400 uppercase tracking-wider">
                     {t(stat.labelKey)}
                  </span>
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl border text-[#003580] bg-[#003580]/5 border-[#003580]/10 transition-transform group-hover:scale-110 duration-300">
                     <stat.icon className="h-5 w-5" />
                  </div>
               </div>
               <div className="mt-4 flex items-center justify-between">
                  <span className="text-2xl font-bold text-zinc-950">
                     {formatValue(stat.value)}
                  </span>
                  <div
                     className={`flex items-center gap-0.5 text-[11px] font-semibold px-2 py-0.5 rounded-lg border ${
                        stat.labelKey === "ratingScore"
                           ? "text-zinc-500 bg-zinc-50 border-zinc-100"
                           : stat.isUp
                             ? "text-emerald-700 bg-emerald-50 border-emerald-100"
                             : "text-rose-700 bg-rose-50 border-rose-100"
                     }`}
                  >
                     {stat.labelKey !== "ratingScore" &&
                        (stat.isUp ? (
                           <ArrowUpRight className="h-3 w-3 stroke-[2.5]" />
                        ) : (
                           <ArrowDownRight className="h-3 w-3 stroke-[2.5]" />
                        ))}
                     {formatValue(stat.change)}
                  </div>
               </div>
            </div>
         ))}
      </div>
   );
}
