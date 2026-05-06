"use client";

import { useEffect, useState } from "react";
import {
   Check,
   ArrowRight,
   Building2,
   Home,
   Hotel,
   Tent,
   ShieldCheck,
   Globe2,
   Smartphone,
   ChevronRight,
} from "lucide-react";
import Link from "next/link";
import PartnerNavbar from "@/components/PartnerNavbar";

export default function BecomeAHostPage() {
   const [mounted, setMounted] = useState(false);

   useEffect(() => {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setMounted(true);
   }, []);

   if (!mounted) return null;

   return (
      <div className="min-h-screen bg-white font-sans selection:bg-blue-100 selection:text-blue-900">
         <PartnerNavbar />

         {/* Hero Section: 60/40 Split */}
         <section className="bg-[#003580] text-white">
            <div className="mx-auto max-w-[1100px] px-4 py-16 sm:px-6 lg:px-8">
               <div className="grid grid-cols-1 gap-12 lg:grid-cols-12 items-center">
                  {/* Left Column: Headline */}
                  <div className="lg:col-span-7">
                     <h1 className="text-3xl font-bold leading-tight sm:text-4xl lg:text-5xl">
                        Đăng <span className="text-[#006ce4]">chỗ nghỉ</span> của bạn trên
                        OmniBooking.com
                     </h1>
                     <p className="mt-8 text-lg text-blue-100 max-w-xl leading-relaxed">
                        Dù việc cho thuê chỗ nghỉ là nghề tay trái hay công việc toàn thời gian, hãy
                        đăng ký chỗ nghỉ ngay hôm nay để tiếp cận hàng triệu khách du lịch.
                     </p>
                  </div>

                  {/* Right Column: Registration Card */}
                  <div className="lg:col-span-5">
                     <div className="rounded-lg bg-white p-8 shadow-2xl text-zinc-900">
                        <h2 className="text-2xl font-bold leading-tight">Đăng ký miễn phí</h2>
                        <ul className="mt-6 space-y-4">
                           <li className="flex items-start gap-3">
                              <Check className="h-5 w-5 text-[#008009] shrink-0 mt-0.5" />
                              <span className="text-sm font-medium text-zinc-700">
                                 45% chủ chỗ nghỉ nhận được đơn đặt phòng đầu tiên trong vòng một
                                 tuần
                              </span>
                           </li>
                           <li className="flex items-start gap-3">
                              <Check className="h-5 w-5 text-[#008009] shrink-0 mt-0.5" />
                              <span className="text-sm font-medium text-zinc-700">
                                 Đăng ký nhanh chóng và dễ dàng
                              </span>
                           </li>
                           <li className="flex items-start gap-3">
                              <Check className="h-5 w-5 text-[#008009] shrink-0 mt-0.5" />
                              <span className="text-sm font-medium text-zinc-700">
                                 Chúng tôi không thu phí khi bạn chưa có đơn đặt phòng
                              </span>
                           </li>
                        </ul>

                        <div className="mt-8 pt-6 border-t border-zinc-100">
                           <button className="flex w-full items-center justify-center gap-2 rounded-md bg-[#006ce4] py-4 text-lg font-bold text-white hover:bg-[#0057b7] transition-all active:scale-[0.98] shadow-lg shadow-blue-100 group">
                              Bắt đầu ngay
                              <ArrowRight className="h-5 w-5 transition-transform group-hover:translate-x-1" />
                           </button>
                           <p className="mt-4 text-center text-xs text-zinc-400">
                              Bằng cách tiếp tục, bạn đồng ý với các Điều khoản và Điều kiện của
                              chúng tôi.
                           </p>
                        </div>
                     </div>
                  </div>
               </div>
            </div>
         </section>

         {/* Property Types Section */}
         <section className="py-16 bg-white">
            <div className="mx-auto max-w-[1100px] px-4">
               <h2 className="text-3xl font-bold text-[#1a1a1a] mb-12">
                  Đăng ký mọi loại hình chỗ nghỉ
               </h2>

               <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
                  <PropertyCard
                     icon={<Building2 className="h-10 w-10 text-[#006ce4]" />}
                     title="Căn hộ"
                     description="Chỗ nghỉ tự phục vụ, có bếp và khu vực sinh hoạt riêng."
                  />
                  <PropertyCard
                     icon={<Home className="h-10 w-10 text-[#006ce4]" />}
                     title="Nhà"
                     description="Nhà nguyên căn, biệt thự, nhà nghỉ dưỡng gia đình."
                  />
                  <PropertyCard
                     icon={<Hotel className="h-10 w-10 text-[#006ce4]" />}
                     title="Khách sạn & B&B"
                     description="Khách sạn, nhà nghỉ, homestay có phục vụ bữa sáng."
                  />
                  <PropertyCard
                     icon={<Tent className="h-10 w-10 text-[#006ce4]" />}
                     title="Chỗ nghỉ độc đáo"
                     description="Thuyền, lều, nhà trên cây và các trải nghiệm mới lạ."
                  />
               </div>
            </div>
         </section>

         {/* Financial Control Section */}
         <section className="py-16 bg-white border-t border-zinc-100">
            <div className="mx-auto max-w-[1100px] px-4">
               <p className="text-[11px] text-zinc-500 mb-8">
                  *Áp dụng cho đặt phòng mà khách thực hiện qua ứng dụng di động. Phiên bản web sẽ
                  sớm ra mắt.
               </p>

               <h2 className="text-4xl font-bold text-[#1a1a1a] leading-tight max-w-2xl mb-16">
                  Kiểm soát tài chính với dịch vụ Thanh toán bởi OmniBooking.com
               </h2>

               <div className="grid grid-cols-1 md:grid-cols-2 gap-x-16 gap-y-12">
                  <PaymentFeature
                     title="Thanh toán dễ dàng"
                     desc={
                        <span>
                           Chúng tôi{" "}
                           <span className="text-[#006ce4] font-medium">
                              xử lý toàn bộ quy trình thanh toán
                           </span>{" "}
                           thay Quý vị, giúp Quý vị có thêm thời gian để phát triển kinh doanh.
                        </span>
                     }
                  />
                  <PaymentFeature
                     title="Thanh toán hằng ngày tại một số thị trường nhất định"
                     desc="Nhận thanh toán nhanh hơn! Chúng tôi sẽ gửi tiền cho Quý vị 24 giờ sau khi khách trả phòng."
                  />
                  <PaymentFeature
                     title="Doanh thu được đảm bảo hơn"
                     desc="Bất cứ khi nào khách thanh toán online cho đặt phòng yêu cầu trả trước, Quý vị chắc chắn sẽ nhận được tiền."
                  />
                  <PaymentFeature
                     title="Giải pháp toàn diện khi đăng nhiều chỗ nghỉ"
                     desc={
                        <span>
                           Tiết kiệm thời gian quản lý tài chính với tính năng{" "}
                           <span className="text-[#006ce4] font-medium">hóa đơn theo nhóm</span> và{" "}
                           <span className="text-[#006ce4] font-medium">rà soát đối chiếu</span>.
                        </span>
                     }
                  />
                  <PaymentFeature
                     title="Kiểm soát dòng tiền tốt hơn"
                     desc="Chọn phương thức và thời gian nhận thanh toán dựa trên các lựa chọn hiện có theo khu vực"
                  />
                  <PaymentFeature
                     title="Giảm thiểu rủi ro"
                     desc={
                        <span>
                           Chúng tôi giúp Quý vị tuân thủ các thay đổi liên quan đến quy định pháp
                           luật, cũng như{" "}
                           <span className="text-[#006ce4] font-medium">giảm thiểu rủi ro</span>{" "}
                           gian lận và bồi hoàn.
                        </span>
                     }
                  />
               </div>

               <div className="mt-16">
                  <button className="rounded-md bg-[#006ce4] px-6 py-3 text-sm font-bold text-white hover:bg-[#0057b7] transition-all shadow-md active:scale-95">
                     Kiếm thêm thu nhập ngay hôm nay
                  </button>
               </div>
            </div>
         </section>

         {/* Why Join Us Section */}
         <section className="py-16 bg-zinc-50">
            <div className="mx-auto max-w-[1100px] px-4 text-center">
               <h2 className="text-3xl font-bold text-[#1a1a1a]">
                  Host an tâm đón khách - Đã có chúng tôi hỗ trợ
               </h2>
               <p className="mt-4 text-zinc-500 mb-16">
                  Mọi công cụ và sự hỗ trợ bạn cần để kinh doanh thành công
               </p>

               <div className="grid grid-cols-1 gap-12 lg:grid-cols-3">
                  <FeatureItem
                     icon={<ShieldCheck className="h-10 w-10 text-[#006ce4]" />}
                     title="An toàn tuyệt đối"
                     points={[
                        "Xác thực khách hàng nghiêm ngặt",
                        "Chính sách bảo vệ chủ nhà",
                        "Hỗ trợ giải quyết khiếu nại",
                     ]}
                  />
                  <FeatureItem
                     icon={<Globe2 className="h-10 w-10 text-[#006ce4]" />}
                     title="Tiếp cận toàn cầu"
                     points={[
                        "Hiển thị trên 43 ngôn ngữ",
                        "Tiếp cận 28 triệu lượt khách",
                        "Quảng bá trên Google và Facebook",
                     ]}
                  />
                  <FeatureItem
                     icon={<Smartphone className="h-10 w-10 text-[#006ce4]" />}
                     title="Quản lý thông minh"
                     points={[
                        "Ứng dụng Pulse tiện lợi",
                        "Đồng bộ lịch tự động",
                        "Báo cáo doanh thu chi tiết",
                     ]}
                  />
               </div>
            </div>
         </section>

         {/* Steps Illustration Section */}
         <section className="py-16 bg-white border-t border-zinc-100">
            <div className="mx-auto max-w-[1100px] px-4">
               <div className="flex flex-col lg:flex-row items-center gap-16">
                  <div className="flex-1">
                     <h2 className="text-4xl font-bold text-[#1a1a1a] leading-tight">
                        Cách hoạt động của việc đăng ký chỗ nghỉ
                     </h2>
                     <div className="mt-12 space-y-10">
                        <StepItem
                           num="1"
                           title="Đăng ký tài khoản"
                           desc="Chỉ mất 15 phút để hoàn tất hồ sơ chỗ nghỉ của bạn."
                        />
                        <StepItem
                           num="2"
                           title="Tải lên thông tin"
                           desc="Thêm hình ảnh, giá và ngày còn trống để khách có thể đặt ngay."
                        />
                        <StepItem
                           num="3"
                           title="Bắt đầu đón khách"
                           desc="Nhận đơn đặt đầu tiên và quản lý thông qua ứng dụng của chúng tôi."
                        />
                     </div>
                  </div>
                  <div className="flex-1 w-full max-w-md lg:max-w-none">
                     <div className="aspect-[4/3] rounded-3xl bg-blue-50 flex items-center justify-center border border-blue-100 shadow-inner relative overflow-hidden">
                        <div className="absolute top-0 right-0 p-8">
                           <div className="h-32 w-32 bg-blue-200/50 rounded-full blur-3xl animate-pulse"></div>
                        </div>
                        <Smartphone className="h-48 w-48 text-blue-600/20" />
                        <div className="absolute inset-0 flex items-center justify-center p-12">
                           <div className="bg-white p-6 rounded-2xl shadow-2xl w-full max-w-xs transform -rotate-3 transition-transform hover:rotate-0">
                              <div className="h-2 w-12 bg-zinc-100 rounded mb-4"></div>
                              <div className="h-4 w-full bg-blue-50 rounded mb-2"></div>
                              <div className="h-4 w-2/3 bg-blue-50 rounded"></div>
                              <div className="mt-6 flex justify-end">
                                 <div className="h-8 w-24 bg-[#006ce4] rounded-lg"></div>
                              </div>
                           </div>
                        </div>
                     </div>
                  </div>
               </div>
            </div>
         </section>

         {/* Simple Footer */}
         <footer className="py-12 border-t border-zinc-100 bg-white">
            <div className="mx-auto max-w-[1100px] px-4 text-center">
               <div className="flex flex-wrap justify-center gap-6 text-sm font-medium text-[#006ce4] mb-8">
                  <Link href="#" className="hover:underline">
                     Về chúng tôi
                  </Link>
                  <Link href="#" className="hover:underline">
                     Điều khoản & Điều kiện
                  </Link>
                  <Link href="#" className="hover:underline">
                     Chính sách bảo mật
                  </Link>
                  <Link href="#" className="hover:underline">
                     Trợ giúp
                  </Link>
               </div>
               <p className="text-xs text-zinc-400">
                  © 1996-2026 OmniBooking.com™. Bảo lưu mọi quyền.
               </p>
            </div>
         </footer>
      </div>
   );
}

function PropertyCard({
   icon,
   title,
   description,
}: {
   icon: React.ReactNode;
   title: string;
   description: string;
}) {
   return (
      <div className="flex flex-col p-8 rounded-lg border border-[#e7e7e7] bg-white transition-all hover:border-[#006ce4] hover:shadow-xl group cursor-pointer h-full">
         <div className="mb-6">{icon}</div>
         <h3 className="text-lg font-bold text-[#1a1a1a] mb-3 group-hover:text-[#006ce4] transition-colors">
            {title}
         </h3>
         <p className="text-[13px] text-[#4b4b4b] leading-relaxed mb-6 flex-1">{description}</p>
         <div className="flex items-center text-sm font-bold text-[#006ce4] group-hover:underline">
            Đăng ký ngay
            <ChevronRight className="h-4 w-4 ml-1" />
         </div>
      </div>
   );
}

function FeatureItem({
   icon,
   title,
   points,
}: {
   icon: React.ReactNode;
   title: string;
   points: string[];
}) {
   return (
      <div className="flex flex-col items-center text-center">
         <div className="mb-6 p-4 rounded-full bg-white shadow-sm border border-zinc-100">
            {icon}
         </div>
         <h3 className="text-lg font-bold text-[#1a1a1a] mb-6">{title}</h3>
         <ul className="space-y-3 text-sm text-zinc-600">
            {points.map((p, idx) => (
               <li key={idx} className="flex items-center gap-2 justify-center">
                  <Check className="h-4 w-4 text-[#008009]" />
                  {p}
               </li>
            ))}
         </ul>
      </div>
   );
}

function StepItem({ num, title, desc }: { num: string; title: string; desc: string }) {
   return (
      <div className="flex gap-6 items-start">
         <div className="h-10 w-10 rounded-full border-2 border-[#1a1a1a] flex items-center justify-center shrink-0 font-bold text-lg">
            {num}
         </div>
         <div>
            <h4 className="text-xl font-bold text-[#1a1a1a]">{title}</h4>
            <p className="mt-2 text-zinc-500 leading-relaxed">{desc}</p>
         </div>
      </div>
   );
}

function PaymentFeature({ title, desc }: { title: string; desc: React.ReactNode }) {
   return (
      <div className="flex gap-4">
         <div className="mt-1 h-6 w-6 rounded-full border border-zinc-400 flex items-center justify-center shrink-0">
            <Check className="h-4 w-4 text-zinc-600" />
         </div>
         <div>
            <h4 className="font-bold text-[#1a1a1a] text-lg mb-2">{title}</h4>
            <p className="text-zinc-600 text-sm leading-relaxed">{desc}</p>
         </div>
      </div>
   );
}
