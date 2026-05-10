import { TrendingUp, Users, DollarSign, Star, ArrowUpRight, ArrowDownRight } from "lucide-react";

const STATS = [
   {
      label: "Doanh thu tháng",
      value: "42.5M",
      change: "+12.5%",
      isUp: true,
      icon: DollarSign,
      color: "bg-green-50 text-green-600",
   },
   {
      label: "Lượt đặt phòng",
      value: "156",
      change: "+8.2%",
      isUp: true,
      icon: TrendingUp,
      color: "bg-blue-50 text-blue-600",
   },
   {
      label: "Khách hàng mới",
      value: "48",
      change: "-2.4%",
      isUp: false,
      icon: Users,
      color: "bg-purple-50 text-purple-600",
   },
   {
      label: "Điểm đánh giá",
      value: "4.9",
      change: "+0.1",
      isUp: true,
      icon: Star,
      color: "bg-yellow-50 text-yellow-600",
   },
];

export default function DashboardStats() {
   return (
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
         {STATS.map((stat) => (
            <div
               key={stat.label}
               className="group rounded-3xl bg-white p-6 border border-zinc-100 shadow-sm hover:shadow-md transition-all duration-300"
            >
               <div className="flex items-center justify-between mb-4">
                  <div
                     className={`rounded-2xl ${stat.color} p-3 transition-transform group-hover:scale-110 duration-300`}
                  >
                     <stat.icon className="h-6 w-6" />
                  </div>
                  <div
                     className={`flex items-center gap-1 rounded-full px-2 py-1 text-[11px] font-bold ${
                        stat.isUp ? "bg-green-50 text-green-600" : "bg-red-50 text-red-600"
                     }`}
                  >
                     {stat.isUp ? (
                        <ArrowUpRight className="h-3 w-3" />
                     ) : (
                        <ArrowDownRight className="h-3 w-3" />
                     )}
                     {stat.change}
                  </div>
               </div>
               <div>
                  <p className="text-sm font-bold text-zinc-400">{stat.label}</p>
                  <p className="text-2xl font-black text-zinc-900 mt-1">{stat.value}</p>
               </div>
            </div>
         ))}
      </div>
   );
}
