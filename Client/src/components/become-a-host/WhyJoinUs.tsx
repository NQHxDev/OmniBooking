import { ShieldCheck, Globe2, Smartphone, Check } from "lucide-react";

export default function WhyJoinUs() {
   return (
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
