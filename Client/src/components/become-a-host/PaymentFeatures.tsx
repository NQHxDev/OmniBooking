import { Check } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";

export default function PaymentFeatures() {
   const t = useTranslations("BecomeAHost.payments");

   return (
      <section className="py-16 bg-white border-t border-zinc-100">
         <div className="mx-auto max-w-[1100px] px-4">
            <h2 className="text-3xl font-bold text-[#1a1a1a] mb-6">{t("title")}</h2>
            <p className="text-zinc-500 max-w-2xl mb-12">{t("subtitle")}</p>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-x-12 gap-y-10">
               <PaymentFeature title={t("feature1Title")} desc={t("feature1Desc")} />
               <PaymentFeature title={t("feature2Title")} desc={t("feature2Desc")} />
               <PaymentFeature title={t("feature3Title")} desc={t("feature3Desc")} />
               <PaymentFeature title={t("feature4Title")} desc={t("feature4Desc")} />
               <PaymentFeature title={t("feature5Title")} desc={t("feature5Desc")} />
               <PaymentFeature title={t("feature6Title")} desc={t("feature6Desc")} />
            </div>

            <div className="mt-16">
               <Link
                  href="/become-a-host/register"
                  className="inline-block rounded-md bg-[#006ce4] px-6 py-3 text-sm font-bold text-white hover:bg-[#0057b7] transition-all shadow-md active:scale-95"
               >
                  {t("ctaButton")}
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
