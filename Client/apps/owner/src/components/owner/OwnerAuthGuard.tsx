"use client";

import { useAuthStore } from "@/store/useAuthStore";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";
import { env } from "@/env";

export default function OwnerAuthGuard({ children }: { children: React.ReactNode }) {
   const t = useTranslations("Owner.authGuard");
   const { user, isLoggedIn, isReady } = useAuthStore();
   const router = useRouter();

   // Derive authorized state directly from store
   const isAuthorized =
      isLoggedIn &&
      user?.roles &&
      (user.roles.includes("ROLE_ADMIN") || user.roles.includes("ROLE_OWNER"));

   useEffect(() => {
      // Chỉ kiểm tra phân quyền khi AuthStore đã được khởi tạo/đồng bộ xong
      if (!isReady) return;

      if (!isAuthorized) {
         toast.error(t("denied"), {
            description: t("noPermission"),
         });
         router.push(env.NEXT_PUBLIC_WEB_URL);
      }
   }, [isReady, isAuthorized, router, t]);

   if (!isAuthorized) {
      return (
         <div className="flex h-screen w-full items-center justify-center bg-white">
            <div className="flex flex-col items-center gap-4 text-center px-4">
               <div className="relative">
                  <Loader2 className="h-12 w-12 animate-spin text-[#006ce4]" />
               </div>
               <div>
                  <p className="text-zinc-900 font-bold text-lg">{t("verifying")}</p>
                  <p className="text-zinc-500 text-sm mt-1">{t("waiting")}</p>
               </div>
            </div>
         </div>
      );
   }

   return <>{children}</>;
}
