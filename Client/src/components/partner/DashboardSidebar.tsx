import Link from "next/link";
import {
   LayoutDashboard,
   Building2,
   CalendarDays,
   MessageSquare,
   BarChart3,
   Settings,
   LogOut,
} from "lucide-react";

const MENU_ITEMS = [
   { icon: LayoutDashboard, label: "Tổng quan", href: "/partner/dashboard", active: true },
   { icon: Building2, label: "Chỗ nghỉ", href: "/partner/properties" },
   { icon: CalendarDays, label: "Đặt phòng", href: "/partner/bookings" },
   { icon: MessageSquare, label: "Tin nhắn", href: "/partner/messages" },
   { icon: BarChart3, label: "Báo cáo", href: "/partner/reports" },
   { icon: Settings, label: "Cài đặt", href: "/partner/settings" },
];

export default function DashboardSidebar() {
   return (
      <aside className="fixed left-0 top-0 hidden h-screen w-64 border-r border-zinc-200 bg-white lg:block z-20">
         <div className="flex h-full flex-col p-6">
            <div className="mb-10 px-2">
               <Link href="/" className="text-xl font-black tracking-tighter text-[#003580]">
                  OmniBooking<span className="text-[#006ce4]">.</span>
               </Link>
               <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-zinc-400 mt-1">
                  Partner Hub
               </p>
            </div>

            <nav className="flex-1 space-y-1">
               {MENU_ITEMS.map((item) => (
                  <Link
                     key={item.label}
                     href={item.href}
                     className={`flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-bold transition-all ${
                        item.active
                           ? "bg-blue-50 text-[#006ce4]"
                           : "text-zinc-500 hover:bg-zinc-50 hover:text-zinc-900"
                     }`}
                  >
                     <item.icon
                        className={`h-5 w-5 ${item.active ? "text-[#006ce4]" : "text-zinc-400"}`}
                     />
                     {item.label}
                  </Link>
               ))}
            </nav>

            <div className="mt-auto border-t border-zinc-100 pt-6">
               <button className="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-bold text-red-500 hover:bg-red-50 transition-all">
                  <LogOut className="h-5 w-5" />
                  Đăng xuất
               </button>
            </div>
         </div>
      </aside>
   );
}
