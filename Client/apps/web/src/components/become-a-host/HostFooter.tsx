import Link from "next/link";
import { useTranslations } from "next-intl";

export default function HostFooter() {
   const t = useTranslations("BecomeAHost.footer");

   return (
      <footer className="py-12 border-t border-zinc-100 bg-white">
         <div className="mx-auto max-w-[1100px] px-4 text-center">
            <div className="flex flex-wrap justify-center gap-6 text-sm font-medium text-[#006ce4] mb-8">
               <Link href="#" className="hover:underline">
                  {t("about")}
               </Link>
               <Link href="#" className="hover:underline">
                  {t("terms")}
               </Link>
               <Link href="#" className="hover:underline">
                  {t("privacy")}
               </Link>
               <Link href="#" className="hover:underline">
                  {t("help")}
               </Link>
            </div>
            <p className="text-xs text-zinc-400">{t("copyright")}</p>
         </div>
      </footer>
   );
}
