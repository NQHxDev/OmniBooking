import Image from "next/image";
import Link from "next/link";
import { BedDouble, Calendar, Globe, Heart, Bell } from "lucide-react";
import GeniusBanner from "@/components/GeniusBanner";
import SearchBar from "@/components/SearchBar";

export default function Home() {
   return (
      <div className="flex min-h-screen flex-col bg-white">
         {/* Navigation Header */}
         <header className="bg-[#003580] text-white">
            <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
               <div className="flex items-center gap-8">
                  <h1 className="text-2xl font-bold tracking-tight">OmniBooking.com</h1>
                  <nav className="hidden space-x-6 text-sm font-medium md:flex">
                     <a
                        href="#"
                        className="flex items-center gap-2 rounded-full border border-white/30 bg-white/10 px-4 py-2"
                     >
                        <BedDouble className="h-5 w-5" />
                        Lưu trú
                     </a>
                     <a
                        href="#"
                        className="flex items-center gap-2 px-4 py-2 hover:bg-white/10 rounded-full transition-colors"
                     >
                        <Globe className="h-5 w-5" />
                        Chuyến bay
                     </a>
                     <a
                        href="#"
                        className="flex items-center gap-2 px-4 py-2 hover:bg-white/10 rounded-full transition-colors"
                     >
                        <Calendar className="h-5 w-5" />
                        Thuê xe
                     </a>
                  </nav>
               </div>
               <div className="flex items-center gap-4">
                  <button className="rounded-full p-2 hover:bg-white/10 transition-colors">
                     <Bell className="h-6 w-6" />
                  </button>
                  <button className="rounded-full p-2 hover:bg-white/10 transition-colors">
                     <Heart className="h-6 w-6" />
                  </button>
                  <div className="hidden items-center gap-4 sm:flex">
                     <button className="text-sm font-semibold hover:underline">
                        Đăng chỗ nghỉ của Quý vị
                     </button>
                     <Link
                        href="/auth/register"
                        className="rounded-sm bg-white px-4 py-1 text-sm font-bold text-[#003580] hover:bg-zinc-100"
                     >
                        Đăng ký
                     </Link>
                     <Link
                        href="/auth/login"
                        className="rounded-sm bg-white px-4 py-1 text-sm font-bold text-[#003580] hover:bg-zinc-100"
                     >
                        Đăng nhập
                     </Link>
                  </div>
               </div>
            </div>
         </header>

         {/* Hero Section */}
         <section className="bg-[#003580] pb-16 pt-12 text-white">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
               <h2 className="text-5xl font-bold leading-tight">Tìm chỗ nghỉ tiếp theo</h2>
               <p className="mt-4 text-2xl text-zinc-100">
                  Tìm ưu đãi khách sạn, chỗ nghỉ dạng nhà và nhiều hơn nữa...
               </p>
            </div>
         </section>

         {/* Search Bar Component */}
         <SearchBar />

         {/* Main Content */}
         <main className="mx-auto w-full max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
            {/* Promo Section */}
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
               <div className="relative h-64 overflow-hidden rounded-lg bg-zinc-900 group cursor-pointer shadow-md">
                  <Image
                     src="/images/hero_banner.png"
                     alt="Promo 1"
                     fill
                     className="object-cover opacity-80 group-hover:scale-105 transition-transform duration-500"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                  <div className="absolute bottom-6 left-6 text-white">
                     <h3 className="text-2xl font-bold">Giảm giá cho kỳ nghỉ hè</h3>
                     <p className="mt-2 text-sm">
                        Tiết kiệm 15% hoặc hơn khi đặt từ nay đến 30/9/2026
                     </p>
                     <button className="mt-4 rounded bg-[#006ce4] px-4 py-2 text-sm font-bold hover:bg-[#0057b7] transition-colors">
                        Tìm ưu đãi
                     </button>
                  </div>
               </div>
               <div className="relative h-64 overflow-hidden rounded-lg bg-zinc-900 group cursor-pointer shadow-md">
                  <Image
                     src="/images/hanoi.png"
                     alt="Promo 2"
                     fill
                     className="object-cover opacity-80 group-hover:scale-105 transition-transform duration-500"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                  <div className="absolute bottom-6 left-6 text-white">
                     <h3 className="text-2xl font-bold">Khám phá Hà Nội</h3>
                     <p className="mt-2 text-sm">
                        Trải nghiệm nét cổ kính và ẩm thực đường phố tuyệt vời
                     </p>
                  </div>
               </div>
            </div>

            {/* Trending Destinations */}
            <div className="mt-16">
               <h3 className="text-2xl font-bold text-black">Điểm đến đang thịnh hành</h3>
               <p className="mt-1 text-zinc-500">
                  Các lựa chọn phổ biến nhất cho du khách từ Việt Nam
               </p>

               <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
                  <div className="relative h-48 overflow-hidden rounded-lg group cursor-pointer shadow-sm">
                     <Image
                        src="/images/dalat.png"
                        alt="Da Lat"
                        fill
                        className="object-cover group-hover:scale-110 transition-transform duration-500"
                     />
                     <div className="absolute top-4 left-4 flex items-center gap-2 text-white drop-shadow-md">
                        <span className="text-xl font-bold">Đà Lạt</span>
                        <Image
                           src="https://flagcdn.com/vn.svg"
                           alt="VN Flag"
                           width={20}
                           height={15}
                        />
                     </div>
                  </div>
                  <div className="relative h-48 overflow-hidden rounded-lg group cursor-pointer shadow-sm">
                     <Image
                        src="/images/hanoi.png"
                        alt="Hanoi"
                        fill
                        className="object-cover group-hover:scale-110 transition-transform duration-500"
                     />
                     <div className="absolute top-4 left-4 flex items-center gap-2 text-white drop-shadow-md">
                        <span className="text-xl font-bold">Hà Nội</span>
                        <Image
                           src="https://flagcdn.com/vn.svg"
                           alt="VN Flag"
                           width={20}
                           height={15}
                        />
                     </div>
                  </div>
                  <div className="relative h-48 overflow-hidden rounded-lg group cursor-pointer shadow-sm bg-zinc-100 flex items-center justify-center text-zinc-400">
                     <Image
                        src="https://images.unsplash.com/photo-1528127269322-539801943592?q=80&w=2070&auto=format&fit=crop"
                        alt="Da Nang"
                        fill
                        className="object-cover group-hover:scale-110 transition-transform duration-500"
                     />
                     <div className="absolute top-4 left-4 flex items-center gap-2 text-white drop-shadow-md">
                        <span className="text-xl font-bold">Đà Nẵng</span>
                        <Image
                           src="https://flagcdn.com/vn.svg"
                           alt="VN Flag"
                           width={20}
                           height={15}
                        />
                     </div>
                  </div>
               </div>
            </div>

            {/* Loyalty/Genius Section */}
            <GeniusBanner />
         </main>

         {/* Simple Footer */}
         <footer className="mt-auto bg-zinc-100 py-12">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
               <div className="flex flex-wrap justify-between gap-8">
                  <div className="max-w-xs">
                     <h4 className="text-sm font-bold text-black uppercase tracking-wider">
                        Hỗ trợ
                     </h4>
                     <ul className="mt-4 space-y-2 text-sm text-[#006ce4]">
                        <li>
                           <a href="#" className="hover:underline">
                              Câu hỏi thường gặp về virus corona (COVID-19)
                           </a>
                        </li>
                        <li>
                           <a href="#" className="hover:underline">
                              Quản lý các chuyến đi của bạn
                           </a>
                        </li>
                        <li>
                           <a href="#" className="hover:underline">
                              Dịch vụ khách hàng
                           </a>
                        </li>
                     </ul>
                  </div>
                  <div>
                     <h4 className="text-sm font-bold text-black uppercase tracking-wider">
                        Khám phá
                     </h4>
                     <ul className="mt-4 space-y-2 text-sm text-[#006ce4]">
                        <li>
                           <a href="#" className="hover:underline">
                              Các quốc gia
                           </a>
                        </li>
                        <li>
                           <a href="#" className="hover:underline">
                              Khu vực
                           </a>
                        </li>
                        <li>
                           <a href="#" className="hover:underline">
                              Thành phố
                           </a>
                        </li>
                     </ul>
                  </div>
               </div>
               <div className="mt-12 border-t border-zinc-200 pt-8 text-center text-xs text-zinc-500">
                  <p>© 2026 OmniBooking.com. Bảo lưu mọi quyền.</p>
               </div>
            </div>
         </footer>
      </div>
   );
}
