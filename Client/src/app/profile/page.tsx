"use client";

import { useEffect, useState } from "react";
import { useAuthStore } from "@/store/useAuthStore";
import {
   User,
   ShieldCheck,
   Settings,
   Bell,
   Users,
   Heart,
   Star,
   Briefcase,
   HelpCircle,
   ShieldAlert,
   Home,
   CreditCard as PaymentIcon,
   History,
   Mail,
   Lock,
   ChevronRight,
   Plane,
   Car,
   Building,
   Gift,
   MessageSquare,
} from "lucide-react";
import Link from "next/link";
import Navbar from "@/components/Navbar";

export default function ProfilePage() {
   const [mounted, setMounted] = useState(false);
   const user = useAuthStore((state) => state.user);

   useEffect(() => {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setMounted(true);
   }, []);

   if (!mounted) return null;

   return (
      <div className="min-h-screen bg-[#f5f5f5]">
         <Navbar />

         {/* Top Blue Header Section */}
         <section className="bg-[#003580] pt-8 pb-16 text-white">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
               <div className="flex items-center gap-4">
                  <div className="h-16 w-16 rounded-full bg-blue-600 flex items-center justify-center text-white text-2xl font-bold border-2 border-white shadow-lg overflow-hidden">
                     {user?.fullName?.charAt(0) || user?.username?.charAt(0)}
                  </div>
                  <div>
                     <h1 className="text-2xl font-bold">
                        Xin chào: {user?.fullName || user?.username}
                     </h1>
                     <p className="text-sm font-medium text-yellow-400 mt-1 italic">Genius Cấp 1</p>
                  </div>
               </div>
            </div>
         </section>

         {/* Main Content Area (Negative Margin to overlap header) */}
         <main className="mx-auto max-w-7xl px-4 -mt-10 sm:px-6 lg:px-8 pb-20">
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
               {/* Left Column: Main Rewards & Alerts */}
               <div className="lg:col-span-8 space-y-6">
                  {/* Genius Rewards Card */}
                  <div className="rounded-xl bg-white p-6 shadow-sm border border-zinc-200">
                     <div className="flex flex-col sm:flex-row justify-between items-start gap-4 mb-6">
                        <div>
                           <h2 className="text-xl font-bold text-zinc-900">
                              Bạn có 3 tặng thưởng Genius
                           </h2>
                           <p className="text-sm text-zinc-500 mt-1">
                              Tận hưởng tặng thưởng và giảm giá cho một số chỗ nghỉ và xe thuê trên
                              toàn cầu.
                           </p>
                        </div>
                        <div className="flex gap-2">
                           <span className="rounded-md bg-yellow-400 px-3 py-1 text-[10px] font-bold text-[#003580] uppercase">
                              Cấp 1
                           </span>
                           <span className="rounded-md bg-zinc-100 px-3 py-1 text-[10px] font-bold text-zinc-400 uppercase">
                              Cấp 2
                           </span>
                        </div>
                     </div>

                     <div className="flex gap-4 overflow-x-auto pb-4 scrollbar-hide">
                        <RewardItem
                           icon={<Building className="h-6 w-6 text-blue-600" />}
                           label="Giảm 10% khi lưu trú"
                        />
                        <RewardItem
                           icon={<Car className="h-6 w-6 text-blue-600" />}
                           label="Giảm giá 10% cho xe thuê"
                        />
                        <RewardItem
                           icon={<Plane className="h-6 w-6 text-orange-500" />}
                           label="Thông báo giá vé máy bay"
                        />
                        <RewardItem
                           icon={<Building className="h-6 w-6 text-zinc-400" />}
                           label="Giảm 10-15% khi lưu trú"
                           locked
                        />
                     </div>

                     <div className="mt-4 pt-4 border-t border-zinc-100">
                        <Link href="#" className="text-sm font-bold text-[#006ce4] hover:underline">
                           Tìm hiểu thêm về tặng thưởng
                        </Link>
                     </div>
                  </div>

                  {/* Flight Price Alert Card */}
                  <div className="rounded-xl bg-white p-6 shadow-sm border border-zinc-200 flex items-center justify-between">
                     <div className="flex gap-4">
                        <div className="h-12 w-12 rounded-lg bg-blue-50 flex items-center justify-center shrink-0">
                           <Bell className="h-6 w-6 text-blue-600" />
                        </div>
                        <div>
                           <h3 className="font-bold text-zinc-900 leading-tight">
                              Thông báo giá vé máy bay
                           </h3>
                           <p className="text-sm text-zinc-500 mt-1">
                              Theo dõi giá cho đường bay và ngày mong muốn trên ứng dụng
                              OmniBooking.com.
                           </p>
                           <button className="mt-3 rounded-md bg-[#006ce4] px-4 py-2 text-sm font-bold text-white hover:bg-[#0057b7] transition-all">
                              Tải ứng dụng
                           </button>
                        </div>
                     </div>
                     <ChevronRight className="h-6 w-6 text-zinc-300 hidden sm:block" />
                  </div>
               </div>

               {/* Right Column: Progress */}
               <div className="lg:col-span-4 space-y-6">
                  <div className="rounded-xl bg-white p-6 shadow-sm border border-zinc-200">
                     <div className="flex items-center gap-3 mb-4">
                        <div className="h-10 w-10 rounded-full bg-blue-900 flex items-center justify-center text-white">
                           <Star className="h-5 w-5 fill-yellow-400 text-yellow-400" />
                        </div>
                        <h3 className="font-bold text-sm">
                           Bạn còn 5 đơn đặt nữa để lên Genius Cấp 2
                        </h3>
                     </div>
                     <Link href="#" className="text-sm font-bold text-[#006ce4] hover:underline">
                        Kiểm tra tiến độ của bạn
                     </Link>
                  </div>

                  <div className="rounded-xl bg-white p-6 shadow-sm border border-zinc-200">
                     <p className="text-sm text-zinc-600">
                        Chưa có Tín dụng hay voucher <span className="font-bold ml-2">0</span>
                     </p>
                     <div className="mt-4 pt-4 border-t border-zinc-100">
                        <Link href="#" className="text-sm font-bold text-[#006ce4] hover:underline">
                           Xem chi tiết
                        </Link>
                     </div>
                  </div>
               </div>
            </div>

            {/* Settings Grid Sections */}
            <div className="mt-8 grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
               <SectionCard title="Thông tin thanh toán">
                  <SectionItem
                     icon={<Gift className="h-5 w-5" />}
                     label="Tặng thưởng & Ví"
                     href="/wallet"
                  />
                  <SectionItem
                     icon={<PaymentIcon className="h-5 w-5" />}
                     label="Phương thức thanh toán"
                     href="/payments"
                  />
                  <SectionItem
                     icon={<History className="h-5 w-5" />}
                     label="Giao dịch"
                     href="/transactions"
                  />
               </SectionCard>

               <SectionCard title="Quản lý tài khoản">
                  <SectionItem
                     icon={<User className="h-5 w-5" />}
                     label="Thông tin cá nhân"
                     href="/profile/details"
                  />
                  <SectionItem
                     icon={<Lock className="h-5 w-5" />}
                     label="Cài đặt bảo mật"
                     href="/security"
                  />
                  <SectionItem
                     icon={<Users className="h-5 w-5" />}
                     label="Người đi cùng"
                     href="/guests"
                  />
               </SectionCard>

               <SectionCard title="Cài đặt">
                  <SectionItem
                     icon={<Settings className="h-5 w-5" />}
                     label="Cài đặt chung"
                     href="/settings"
                  />
                  <SectionItem
                     icon={<Mail className="h-5 w-5" />}
                     label="Cài đặt email"
                     href="/settings/email"
                  />
               </SectionCard>

               <SectionCard title="Hoạt động du lịch">
                  <SectionItem
                     icon={<Briefcase className="h-5 w-5" />}
                     label="Chuyến đi và đơn đặt"
                     href="/bookings"
                  />
                  <SectionItem
                     icon={<Heart className="h-5 w-5" />}
                     label="Danh sách đã lưu"
                     href="/wishlist"
                  />
                  <SectionItem
                     icon={<MessageSquare className="h-5 w-5" />}
                     label="Đánh giá của tôi"
                     href="/reviews"
                  />
               </SectionCard>

               <SectionCard title="Trợ giúp">
                  <SectionItem
                     icon={<HelpCircle className="h-5 w-5" />}
                     label="Liên hệ dịch vụ khách hàng"
                     href="/help"
                  />
                  <SectionItem
                     icon={<ShieldCheck className="h-5 w-5" />}
                     label="Trung tâm thông tin bảo mật"
                     href="/safety"
                  />
                  <SectionItem
                     icon={<ShieldAlert className="h-5 w-5" />}
                     label="Giải quyết khiếu nại"
                     href="/disputes"
                  />
               </SectionCard>

               <SectionCard title="Pháp lý và quyền riêng tư">
                  <SectionItem
                     icon={<ShieldCheck className="h-5 w-5" />}
                     label="Quản lý quyền riêng tư"
                     href="/privacy"
                  />
                  <SectionItem
                     icon={<History className="h-5 w-5" />}
                     label="Hướng dẫn nội dung"
                     href="/guidelines"
                  />
               </SectionCard>

               <SectionCard title="Dành cho chủ chỗ nghỉ">
                  <SectionItem
                     icon={<Home className="h-5 w-5" />}
                     label="Đăng chỗ nghỉ"
                     href="/become-a-host"
                  />
               </SectionCard>
            </div>

            {/* Footer links simple */}
            <div className="mt-20 pt-8 border-t border-zinc-200 text-center">
               <div className="flex flex-wrap justify-center gap-4 text-xs font-medium text-zinc-500 mb-4">
                  <Link href="#" className="hover:underline">
                     Liên hệ Dịch vụ Khách hàng
                  </Link>
                  <Link href="#" className="hover:underline">
                     Chính sách Bảo mật
                  </Link>
                  <Link href="#" className="hover:underline">
                     Chính sách về Quyền con người
                  </Link>
                  <Link href="#" className="hover:underline">
                     Điều khoản dịch vụ
                  </Link>
               </div>
               <p className="text-[10px] text-zinc-400">
                  Bản quyền © 1996–2026 OmniBooking.com™. Bảo lưu mọi quyền.
               </p>
            </div>
         </main>
      </div>
   );
}

function RewardItem({
   icon,
   label,
   locked = false,
}: {
   icon: React.ReactNode;
   label: string;
   locked?: boolean;
}) {
   return (
      <div
         className={`flex flex-col items-center justify-center p-4 rounded-xl border border-zinc-100 min-w-[140px] h-40 text-center transition-all hover:shadow-md ${locked ? "bg-zinc-50 opacity-60" : "bg-white"}`}
      >
         <div className="mb-4">{icon}</div>
         <span className="text-xs font-bold text-zinc-700 leading-snug">{label}</span>
      </div>
   );
}

function SectionCard({ title, children }: { title: string; children: React.ReactNode }) {
   return (
      <div className="rounded-xl bg-white shadow-sm border border-zinc-200 flex flex-col h-full overflow-hidden">
         <div className="p-5 border-b border-zinc-100 bg-white">
            <h3 className="font-bold text-zinc-900">{title}</h3>
         </div>
         <div className="flex-1">{children}</div>
      </div>
   );
}

function SectionItem({
   icon,
   label,
   href,
}: {
   icon: React.ReactNode;
   label: string;
   href: string;
}) {
   return (
      <Link
         href={href}
         className="flex items-center justify-between px-5 py-4 hover:bg-zinc-50 transition-all border-b border-zinc-50 last:border-0 group"
      >
         <div className="flex items-center gap-3">
            <span className="text-zinc-500 group-hover:text-[#006ce4] transition-colors">
               {icon}
            </span>
            <span className="text-[13px] font-medium text-zinc-700 group-hover:text-zinc-900">
               {label}
            </span>
         </div>
         <ChevronRight className="h-4 w-4 text-zinc-300 group-hover:text-[#006ce4] transition-colors" />
      </Link>
   );
}
