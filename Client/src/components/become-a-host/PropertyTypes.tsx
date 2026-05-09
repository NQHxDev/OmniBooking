import { Building2, Home, Hotel, Tent } from "lucide-react";

export default function PropertyTypes() {
   const types = [
      {
         icon: <Home className="h-8 w-8" />,
         title: "Căn hộ",
         description: "Chỗ nghỉ tự phục vụ, thường nằm trong tòa nhà.",
      },
      {
         icon: <Hotel className="h-8 w-8" />,
         title: "Khách sạn",
         description: "Chỗ nghỉ có các tiện nghi và dịch vụ chuyên nghiệp.",
      },
      {
         icon: <Building2 className="h-8 w-8" />,
         title: "Nhà nghỉ",
         description: "Chỗ nghỉ gia đình, ấm cúng và gần gũi.",
      },
      {
         icon: <Tent className="h-8 w-8" />,
         title: "Chỗ nghỉ độc đáo",
         description: "Thuyền, lều, nhà trên cây và nhiều hơn nữa.",
      },
   ];

   return (
      <section className="py-16 bg-white">
         <div className="mx-auto max-w-[1100px] px-4">
            <h2 className="text-3xl font-bold text-[#1a1a1a] mb-12">
               Đăng ký mọi loại hình chỗ nghỉ
            </h2>

            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
               {types.map((type, index) => (
                  <div
                     key={index}
                     className="rounded-2xl border border-zinc-100 p-8 hover:border-blue-100 hover:shadow-xl hover:shadow-blue-50 transition-all duration-300 group"
                  >
                     <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-50 text-[#006ce4] group-hover:scale-110 transition-transform">
                        {type.icon}
                     </div>
                     <h3 className="text-xl font-bold text-[#1a1a1a] mb-3">{type.title}</h3>
                     <p className="text-sm text-zinc-500 leading-relaxed">{type.description}</p>
                  </div>
               ))}
            </div>
         </div>
      </section>
   );
}
