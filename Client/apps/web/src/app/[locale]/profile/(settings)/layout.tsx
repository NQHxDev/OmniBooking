"use client";

import { useTranslations } from "next-intl";
import { User, Lock, Users, Settings, CreditCard, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import ProfileNavbar from "@/components/ProfileNavbar";

export default function ProfileSettingsLayout({ children }: { children: React.ReactNode }) {
   const tProfile = useTranslations("Profile");
   const tDetails = useTranslations("Profile.details");
   const pathname = usePathname();

   const sidebarItems = [
      { icon: <User className="h-5 w-5" />, label: tDetails("title"), href: "/profile/details" },
      {
         icon: <Lock className="h-5 w-5" />,
         label: tProfile("items.security"),
         href: "/profile/security",
      },
      {
         icon: <Users className="h-5 w-5" />,
         label: tProfile("items.guests"),
         href: "/profile/guests",
      },
      {
         icon: <Settings className="h-5 w-5" />,
         label: tProfile("items.general"),
         href: "/profile/settings",
      },
      {
         icon: <CreditCard className="h-5 w-5" />,
         label: tProfile("items.paymentMethods"),
         href: "/profile/payments",
      },
      {
         icon: <ShieldCheck className="h-5 w-5" />,
         label: tProfile("items.privacy"),
         href: "/profile/privacy",
      },
   ];

   return (
      <div className="min-h-screen bg-white">
         <ProfileNavbar />
         <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 font-sans">
            <div className="grid grid-cols-1 gap-12 lg:grid-cols-12">
               {/* Sidebar - Cố định không đổi khi chuyển trang */}
               <aside className="lg:col-span-3">
                  <nav className="flex flex-col rounded-xl border border-zinc-200 overflow-hidden shadow-sm bg-white">
                     {sidebarItems.map((item) => {
                        const isActive = pathname.includes(item.href);
                        return (
                           <Link
                              key={item.href}
                              href={item.href}
                              className={`flex items-center gap-3 px-4 py-4 text-[13px] font-medium transition-colors border-b border-zinc-100 last:border-0 ${
                                 isActive
                                    ? "bg-blue-50/50 text-[#006ce4] border-l-4 border-l-[#006ce4]"
                                    : "text-zinc-600 hover:bg-zinc-50"
                              }`}
                           >
                              <span className={isActive ? "text-[#006ce4]" : "text-zinc-400"}>
                                 {item.icon}
                              </span>
                              {item.label}
                           </Link>
                        );
                     })}
                  </nav>
               </aside>

               {/* Main Content Area - Chỉ phần này thay đổi khi chuyển trang */}
               <div className="lg:col-span-9">{children}</div>
            </div>
         </main>
      </div>
   );
}
