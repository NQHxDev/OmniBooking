import { Building2, Home, Hotel, Tent } from "lucide-react";
import { useTranslations } from "next-intl";

export default function PropertyTypes() {
   const t = useTranslations("BecomeAHost.propertyTypes");

   const types = [
      {
         icon: <Home className="h-8 w-8" />,
         title: t("apartment"),
         description: t("apartmentDesc"),
      },
      {
         icon: <Hotel className="h-8 w-8" />,
         title: t("hotel"),
         description: t("hotelDesc"),
      },
      {
         icon: <Building2 className="h-8 w-8" />,
         title: t("guesthouse"),
         description: t("guesthouseDesc"),
      },
      {
         icon: <Tent className="h-8 w-8" />,
         title: t("unique"),
         description: t("uniqueDesc"),
      },
   ];

   return (
      <section className="py-16 bg-white">
         <div className="mx-auto max-w-[1100px] px-4">
            <h2 className="text-3xl font-bold text-[#1a1a1a] mb-12">{t("title")}</h2>

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
