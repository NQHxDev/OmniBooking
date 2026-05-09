import { Check } from "lucide-react";
import Link from "next/link";

export default function PaymentFeatures() {
   return (
      <section className="py-16 bg-white border-t border-zinc-100">
         <div className="mx-auto max-w-[1100px] px-4">
            <h2 className="text-3xl font-bold text-[#1a1a1a] mb-6">
               Thanh toán đơn giản và minh bạch
            </h2>
            <p className="text-zinc-500 max-w-2xl mb-12">
               Quý vị chọn cách nhận thanh toán từ khách, chúng tôi sẽ xử lý phần còn lại.
            </p>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-x-12 gap-y-10">
               <PaymentFeature
                  title="Tự động hóa thanh toán"
                  desc="Chúng tôi thay Quý vị xử lý thanh toán của khách, giải quyết các rắc rối về thẻ tín dụng và gian lận."
               />
               <PaymentFeature
                  title="Thanh toán hằng ngày"
                  desc="Nhận thanh toán nhanh hơn! Chúng tôi sẽ gửi tiền cho Quý vị 24 giờ sau khi khách trả phòng."
               />
               <PaymentFeature
                  title="Doanh thu được đảm bảo"
                  desc="Bất cứ khi nào khách thanh toán online cho đặt phòng yêu cầu trả trước, Quý vị chắc chắn sẽ nhận được tiền."
               />
               <PaymentFeature
                  title="Giải pháp toàn diện"
                  desc="Tiết kiệm thời gian quản lý tài chính với tính năng hóa đơn theo nhóm và rà soát đối chiếu."
               />
               <PaymentFeature
                  title="Kiểm soát dòng tiền"
                  desc="Chọn phương thức và thời gian nhận thanh toán dựa trên các lựa chọn hiện có theo khu vực"
               />
               <PaymentFeature
                  title="Giảm thiểu rủi ro"
                  desc="Chúng tôi giúp Quý vị tuân thủ các thay đổi liên quan đến quy định pháp luật, giảm thiểu rủi ro gian lận."
               />
            </div>

            <div className="mt-16">
               <Link
                  href="/become-a-host/register"
                  className="inline-block rounded-md bg-[#006ce4] px-6 py-3 text-sm font-bold text-white hover:bg-[#0057b7] transition-all shadow-md active:scale-95"
               >
                  Kiếm thêm thu nhập ngay hôm nay
               </Link>
            </div>
         </div>
      </section>
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
