"use client";

import { useState, useEffect } from "react";
import {
   LayoutDashboard,
   Building2,
   CalendarDays,
   MessageSquare,
   BarChart3,
   Settings,
   LogOut,
   Bell,
   Search,
   TrendingUp,
   Users,
   DollarSign,
   Star,
   ChevronRight,
   Plus,
   MoreVertical,
   ArrowUpRight,
   ArrowDownRight,
} from "lucide-react";
import Link from "next/link";
import Image from "next/image";

export default function PartnerDashboard() {
   const [mounted, setMounted] = useState(false);

   useEffect(() => {
      const timer = setTimeout(() => setMounted(true), 0);
      return () => clearTimeout(timer);
   }, []);

   if (!mounted) return null;

   return (
      <div className="min-h-screen bg-[#f8fafc] flex font-sans">
         {/* Sidebar */}
         <aside className="w-64 bg-[#003580] text-white flex flex-col fixed inset-y-0 z-50">
            <div className="p-6">
               <div className="flex items-center gap-2 mb-8">
                  <span className="text-xl font-bold tracking-tight">OmniPartner</span>
               </div>

               <nav className="space-y-1">
                  <NavItem icon={<LayoutDashboard size={20} />} label="Tổng quan" active />
                  <NavItem icon={<Building2 size={20} />} label="Chỗ nghỉ của tôi" />
                  <NavItem icon={<CalendarDays size={20} />} label="Đặt phòng & Lịch" />
                  <NavItem icon={<MessageSquare size={20} />} label="Tin nhắn" badge="3" />
                  <NavItem icon={<BarChart3 size={20} />} label="Báo cáo doanh thu" />
               </nav>
            </div>

            <div className="mt-auto p-6 border-t border-blue-900/50">
               <nav className="space-y-1">
                  <NavItem icon={<Settings size={20} />} label="Cài đặt" />
                  <NavItem icon={<LogOut size={20} />} label="Đăng xuất" />
               </nav>
               <div className="mt-6 flex items-center gap-3 p-3 bg-blue-900/30 rounded-xl">
                  <div className="h-10 w-10 rounded-full bg-blue-500 flex items-center justify-center font-bold">
                     NQ
                  </div>
                  <div className="flex-1 min-w-0">
                     <p className="text-sm font-bold truncate">NQH Host</p>
                     <p className="text-xs text-blue-300 truncate">Pro Partner</p>
                  </div>
               </div>
            </div>
         </aside>

         {/* Main Content */}
         <main className="flex-1 ml-64">
            {/* Header */}
            <header className="h-20 bg-white border-b border-zinc-200 flex items-center justify-between px-8 sticky top-0 z-40">
               <div className="relative w-96">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-400" size={18} />
                  <input
                     type="text"
                     placeholder="Tìm kiếm đặt phòng, khách hàng..."
                     className="w-full pl-10 pr-4 py-2 bg-zinc-100 border-none rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm transition-all"
                  />
               </div>

               <div className="flex items-center gap-4">
                  <button className="relative p-2 text-zinc-500 hover:bg-zinc-100 rounded-lg transition-colors">
                     <Bell size={20} />
                     <span className="absolute top-2 right-2 h-2 w-2 bg-red-500 rounded-full border-2 border-white"></span>
                  </button>
                  <div className="h-8 w-px bg-zinc-200 mx-2"></div>
                  <button className="flex items-center gap-2 bg-[#006ce4] text-white px-4 py-2 rounded-lg font-bold text-sm hover:bg-[#0057b7] transition-all shadow-lg shadow-blue-100">
                     <Plus size={18} />
                     Thêm chỗ nghỉ
                  </button>
               </div>
            </header>

            {/* Dashboard Content */}
            <div className="p-8 space-y-8">
               {/* Welcome Section */}
               <div className="flex items-end justify-between">
                  <div>
                     <h1 className="text-3xl font-bold text-zinc-900">Chào mừng trở lại, NQH!</h1>
                     <p className="text-zinc-500 mt-1">Đây là những gì đang diễn ra với chỗ nghỉ của bạn hôm nay.</p>
                  </div>
                  <div className="flex gap-2 bg-white p-1 rounded-lg border border-zinc-200 shadow-sm">
                     <button className="px-4 py-1.5 text-sm font-bold bg-[#006ce4] text-white rounded-md transition-all">7 ngày qua</button>
                     <button className="px-4 py-1.5 text-sm font-medium text-zinc-500 hover:bg-zinc-50 rounded-md transition-all">30 ngày qua</button>
                  </div>
               </div>

               {/* Stats Grid */}
               <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                  <StatCard
                     icon={<DollarSign className="text-emerald-600" />}
                     label="Tổng doanh thu"
                     value="45.280.000đ"
                     trend="+12.5%"
                     isPositive={true}
                  />
                  <StatCard
                     icon={<CalendarDays className="text-blue-600" />}
                     label="Đơn đặt phòng"
                     value="128"
                     trend="+8.2%"
                     isPositive={true}
                  />
                  <StatCard
                     icon={<Users className="text-purple-600" />}
                     label="Khách hàng mới"
                     value="42"
                     trend="-3.1%"
                     isPositive={false}
                  />
                  <StatCard
                     icon={<Star className="text-amber-500" />}
                     label="Đánh giá trung bình"
                     value="4.9/5.0"
                     trend="+0.2"
                     isPositive={true}
                  />
               </div>

               {/* Middle Section: Chart and Recent Activity */}
               <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                  {/* Chart Placeholder */}
                  <div className="lg:col-span-2 bg-white p-6 rounded-2xl border border-zinc-200 shadow-sm">
                     <div className="flex items-center justify-between mb-8">
                        <h2 className="text-lg font-bold text-zinc-900 flex items-center gap-2">
                           <TrendingUp size={20} className="text-[#006ce4]" />
                           Hiệu suất kinh doanh
                        </h2>
                        <button className="text-zinc-400 hover:text-zinc-900 transition-colors">
                           <MoreVertical size={20} />
                        </button>
                     </div>
                     <div className="aspect-[2/1] bg-gradient-to-b from-blue-50/50 to-transparent rounded-xl border border-dashed border-blue-100 flex items-center justify-center relative overflow-hidden">
                        <div className="absolute inset-0 flex items-end px-4 gap-4">
                           <div className="flex-1 bg-blue-500/20 rounded-t-lg h-[40%] transition-all hover:h-[50%] cursor-pointer"></div>
                           <div className="flex-1 bg-blue-500/20 rounded-t-lg h-[60%] transition-all hover:h-[70%] cursor-pointer"></div>
                           <div className="flex-1 bg-blue-500/20 rounded-t-lg h-[45%] transition-all hover:h-[55%] cursor-pointer"></div>
                           <div className="flex-1 bg-[#006ce4] rounded-t-lg h-[80%] transition-all hover:h-[90%] cursor-pointer relative">
                              <div className="absolute -top-10 left-1/2 -translate-x-1/2 bg-zinc-900 text-white text-[10px] py-1 px-2 rounded font-bold whitespace-nowrap shadow-xl">
                                 12.450.000đ
                              </div>
                           </div>
                           <div className="flex-1 bg-blue-500/20 rounded-t-lg h-[55%] transition-all hover:h-[65%] cursor-pointer"></div>
                           <div className="flex-1 bg-blue-500/20 rounded-t-lg h-[30%] transition-all hover:h-[40%] cursor-pointer"></div>
                           <div className="flex-1 bg-blue-500/20 rounded-t-lg h-[65%] transition-all hover:h-[75%] cursor-pointer"></div>
                        </div>
                        <p className="text-blue-600 font-bold text-sm z-10 bg-white/80 backdrop-blur px-4 py-2 rounded-full border border-blue-100 shadow-sm">
                           Biểu đồ doanh thu hàng tuần
                        </p>
                     </div>
                  </div>

                  {/* Recent Activity */}
                  <div className="bg-white p-6 rounded-2xl border border-zinc-200 shadow-sm">
                     <h2 className="text-lg font-bold text-zinc-900 mb-6">Đơn đặt gần đây</h2>
                     <div className="space-y-6">
                        <ActivityItem
                           name="Lê Văn An"
                           property="Căn hộ Studio Vinhome"
                           time="2 giờ trước"
                           amount="1.200.000đ"
                           status="Confirmed"
                        />
                        <ActivityItem
                           name="Trần Thị Bình"
                           property="Penthouse Landmark 81"
                           time="5 giờ trước"
                           amount="5.500.000đ"
                           status="Pending"
                        />
                        <ActivityItem
                           name="Nguyễn Công Thành"
                           property="Villa Đà Lạt View"
                           time="Hôm qua"
                           amount="3.800.000đ"
                           status="Confirmed"
                        />
                        <ActivityItem
                           name="Phạm Minh Tuấn"
                           property="Căn hộ Studio Vinhome"
                           time="Hôm qua"
                           amount="1.200.000đ"
                           status="Cancelled"
                        />
                     </div>
                     <button className="w-full mt-8 py-3 text-sm font-bold text-[#006ce4] border border-blue-50 rounded-xl hover:bg-blue-50 transition-all">
                        Xem tất cả đơn đặt
                     </button>
                  </div>
               </div>

               {/* Bottom Section: My Properties */}
               <div className="bg-white p-8 rounded-2xl border border-zinc-200 shadow-sm">
                  <div className="flex items-center justify-between mb-8">
                     <h2 className="text-xl font-bold text-zinc-900">Chỗ nghỉ của bạn</h2>
                     <Link href="#" className="text-sm font-bold text-[#006ce4] hover:underline flex items-center gap-1">
                        Quản lý tất cả <ChevronRight size={16} />
                     </Link>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                     <PropertyItem
                        image="https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800&q=80"
                        name="Căn hộ Studio Vinhome Central Park"
                        location="Bình Thạnh, TP. HCM"
                        price="1.200.000đ"
                        status="Đang kinh doanh"
                     />
                     <PropertyItem
                        image="https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800&q=80"
                        name="Penthouse Landmark 81 - City View"
                        location="Bình Thạnh, TP. HCM"
                        price="5.500.000đ"
                        status="Đang kinh doanh"
                     />
                     <div className="border-2 border-dashed border-zinc-200 rounded-2xl flex flex-col items-center justify-center p-8 hover:border-[#006ce4] hover:bg-blue-50/50 transition-all group cursor-pointer">
                        <div className="h-12 w-12 bg-zinc-100 text-zinc-400 rounded-full flex items-center justify-center mb-4 group-hover:bg-[#006ce4] group-hover:text-white transition-all">
                           <Plus size={24} />
                        </div>
                        <p className="font-bold text-zinc-900">Thêm chỗ nghỉ mới</p>
                        <p className="text-sm text-zinc-500 text-center mt-1">Bắt đầu kiếm thêm thu nhập từ không gian của bạn</p>
                     </div>
                  </div>
               </div>
            </div>
         </main>
      </div>
   );
}

function NavItem({ icon, label, active = false, badge = "" }: { icon: React.ReactNode; label: string; active?: boolean; badge?: string }) {
   return (
      <button
         className={`w-full flex items-center justify-between px-4 py-3 rounded-xl transition-all ${active ? "bg-[#006ce4] text-white shadow-lg shadow-blue-900/20" : "text-blue-100 hover:bg-blue-900/50"
            }`}
      >
         <div className="flex items-center gap-3">
            {icon}
            <span className="text-sm font-medium">{label}</span>
         </div>
         {badge && (
            <span className="h-5 w-5 bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center">
               {badge}
            </span>
         )}
      </button>
   );
}

function StatCard({ icon, label, value, trend, isPositive }: { icon: React.ReactNode; label: string; value: string; trend: string; isPositive: boolean }) {
   return (
      <div className="bg-white p-6 rounded-2xl border border-zinc-200 shadow-sm hover:shadow-md transition-all">
         <div className="flex items-center justify-between mb-4">
            <div className="h-10 w-10 bg-zinc-50 rounded-xl flex items-center justify-center">
               {icon}
            </div>
            <div className={`flex items-center gap-1 text-xs font-bold px-2 py-1 rounded-full ${isPositive ? "bg-emerald-50 text-emerald-600" : "bg-red-50 text-red-600"}`}>
               {isPositive ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}
               {trend}
            </div>
         </div>
         <p className="text-zinc-500 text-xs font-medium uppercase tracking-wider">{label}</p>
         <p className="text-2xl font-bold text-zinc-900 mt-1">{value}</p>
      </div>
   );
}

function ActivityItem({ name, property, time, amount, status }: { name: string; property: string; time: string; amount: string; status: string }) {
   return (
      <div className="flex items-start justify-between">
         <div className="flex gap-3">
            <div className="h-10 w-10 rounded-full bg-zinc-100 flex items-center justify-center font-bold text-zinc-500 text-sm">
               {name.charAt(0)}
            </div>
            <div>
               <p className="text-sm font-bold text-zinc-900">{name}</p>
               <p className="text-xs text-zinc-500 truncate w-40">{property}</p>
               <p className="text-[10px] text-zinc-400 mt-1">{time}</p>
            </div>
         </div>
         <div className="text-right">
            <p className="text-sm font-bold text-zinc-900">{amount}</p>
            <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${status === "Confirmed" ? "bg-emerald-50 text-emerald-600" :
               status === "Pending" ? "bg-amber-50 text-amber-600" : "bg-red-50 text-red-600"
               }`}>
               {status}
            </span>
         </div>
      </div>
   );
}

function PropertyItem({ image, name, location, price, status }: { image: string; name: string; location: string; price: string; status: string }) {
   return (
      <div className="group cursor-pointer">
         <div className="relative aspect-[16/10] rounded-2xl overflow-hidden mb-4 shadow-sm group-hover:shadow-xl transition-all duration-500">
            <Image 
               src={image} 
               alt={name} 
               fill 
               className="object-cover group-hover:scale-110 transition-all duration-700" 
            />
            <div className="absolute top-3 left-3 bg-white/90 backdrop-blur px-3 py-1 rounded-full text-[10px] font-bold text-[#006ce4] border border-blue-100">
               {status}
            </div>
         </div>
         <h3 className="font-bold text-zinc-900 group-hover:text-[#006ce4] transition-colors">{name}</h3>
         <div className="flex items-center justify-between mt-2">
            <p className="text-xs text-zinc-500">{location}</p>
            <p className="text-sm font-bold text-zinc-900">{price}<span className="text-[10px] font-medium text-zinc-500">/đêm</span></p>
         </div>
      </div>
   );
}
