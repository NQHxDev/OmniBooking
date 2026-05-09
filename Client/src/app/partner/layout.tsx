"use client";

import { useAuthStore } from "@/store/useAuthStore";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { Loader2 } from "lucide-react";

export default function PartnerLayout({ children }: { children: React.ReactNode }) {
   const { user, isLoggedIn } = useAuthStore();
   const router = useRouter();

   // Derive authorized state directly from store
   const isAuthorized = isLoggedIn && user?.roles.includes("ROLE_PARTNER");

   useEffect(() => {
      // Chờ cho đến khi trạng thái auth được hydrate từ storage
      if (isLoggedIn === false) {
         router.replace("/auth/login?callbackUrl=/partner/dashboard");
      } else if (isLoggedIn === true && !user?.roles.includes("ROLE_PARTNER")) {
         router.replace("/");
      }
   }, [isLoggedIn, user, router]);

   if (!isAuthorized) {
      return (
         <div className="flex h-screen w-full items-center justify-center bg-white">
            <div className="flex flex-col items-center gap-4">
               <Loader2 className="h-10 w-10 animate-spin text-[#006ce4]" />
               <p className="text-zinc-500 font-medium animate-pulse">
                  Đang xác thực quyền truy cập...
               </p>
            </div>
         </div>
      );
   }

   return <>{children}</>;
}
