"use client";

import { Link } from "@/i18n/routing";
import {
   LayoutDashboard,
   Building2,
   CalendarDays,
   MessageSquare,
   BarChart3,
   Settings,
   Home,
} from "lucide-react";
import { useTranslations } from "next-intl";
import { usePathname } from "next/navigation";
import { env } from "@/env";

const MENU_ITEMS = [
   { icon: LayoutDashboard, labelKey: "overview", href: "/dashboard" },
   { icon: Building2, labelKey: "properties", href: "/properties" },
   { icon: CalendarDays, labelKey: "bookings", href: "/bookings" },
   { icon: MessageSquare, labelKey: "messages", href: "/messages" },
   { icon: BarChart3, labelKey: "reports", href: "/reports" },
   { icon: Settings, labelKey: "settings", href: "/settings" },
];

export default function DashboardSidebar() {
   const t = useTranslations("Partner.sidebar");
   const pathname = usePathname();

   return (
      <aside className="fixed left-0 top-0 hidden h-screen w-64 border-r border-zinc-200 bg-white lg:block z-20">
         <div className="flex h-full flex-col p-6">
            <div className="mb-10 px-2">
               <Link
                  href="/dashboard"
                  onClick={(e) => {
                     e.preventDefault();
                     window.location.href = "/dashboard";
                  }}
                  className="text-2xl font-black tracking-tight text-[#003580]"
               >
                  OmniBooking<span className="text-[#006ce4]">.</span>
               </Link>
               <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-zinc-400 mt-1">
                  Partner Hub
               </p>
            </div>

            <nav className="flex-1 space-y-1">
               {MENU_ITEMS.map((item) => {
                  const isActive = pathname.endsWith(item.href);
                  return (
                     <Link
                        key={item.labelKey}
                        href={item.href}
                        className={`flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-bold transition-all ${
                           isActive
                              ? "bg-blue-50 text-[#006ce4]"
                              : "text-zinc-500 hover:bg-zinc-50 hover:text-zinc-900"
                        }`}
                     >
                        <item.icon
                           className={`h-5 w-5 ${isActive ? "text-[#006ce4]" : "text-zinc-400"}`}
                        />
                        {t(item.labelKey)}
                     </Link>
                  );
               })}
            </nav>

            <div className="mt-auto border-t border-zinc-100 pt-6">
               <a
                  href={env.NEXT_PUBLIC_WEB_URL}
                  className="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-bold text-zinc-500 hover:bg-zinc-50 hover:text-zinc-900 transition-all cursor-pointer"
               >
                  <Home className="h-5 w-5 text-zinc-400" />
                  {t("backToHome")}
               </a>
            </div>
         </div>
      </aside>
   );
}
