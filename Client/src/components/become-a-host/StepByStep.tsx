import { Smartphone } from "lucide-react";

export default function StepByStep() {
   return (
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
