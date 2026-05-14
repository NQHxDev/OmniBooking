import { ShieldCheck, Globe2, Smartphone, Check } from "lucide-react";
import { useTranslations } from "next-intl";

export default function WhyJoinUs() {
   const t = useTranslations("BecomeAHost.whyJoin");

   return (
      <section className="py-16 bg-zinc-50">
         <div className="mx-auto max-w-[1100px] px-4 text-center">
            <h2 className="text-3xl font-bold text-[#1a1a1a]">{t("title")}</h2>
            <p className="mt-4 text-zinc-500 mb-16">{t("subtitle")}</p>

            <div className="grid grid-cols-1 gap-12 lg:grid-cols-3">
               <FeatureItem
                  icon={<ShieldCheck className="h-10 w-10 text-[#006ce4]" />}
                  title={t("safetyTitle")}
                  points={[t("safety1"), t("safety2"), t("safety3")]}
               />
               <FeatureItem
                  icon={<Globe2 className="h-10 w-10 text-[#006ce4]" />}
                  title={t("reachTitle")}
                  points={[t("reach1"), t("reach2"), t("reach3")]}
               />
               <FeatureItem
                  icon={<Smartphone className="h-10 w-10 text-[#006ce4]" />}
                  title={t("manageTitle")}
                  points={[t("manage1"), t("manage2"), t("manage3")]}
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
