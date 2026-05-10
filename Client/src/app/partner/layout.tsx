"use client";

import { useAuthStore } from "@/store/useAuthStore";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

export default function PartnerLayout({ children }: { children: React.ReactNode }) {
   const { user, isLoggedIn } = useAuthStore();
   const router = useRouter();

   // Derive authorized state directly from store
   const isAuthorized = isLoggedIn && user?.roles?.includes("ROLE_PARTNER");

   useEffect(() => {
      // Nếu đã có quyền ngay lập tức thì không làm gì cả
      if (isAuthorized) return;

      // Hẹn giờ 3 giây để "kiên nhẫn" đợi Store hydrate xong (tránh lỗi F5)
      const timer = setTimeout(() => {
         // Nếu sau 3 giây mà vẫn chưa authorized thì mới đá đi
         if (!isAuthorized) {
            toast.error("Truy cập bị từ chối", {
               description: "Bạn không có quyền truy cập!",
            });
            router.push("/");
         }
      }, 3000);

      return () => clearTimeout(timer);
   }, [isAuthorized, router]);

   if (!isAuthorized) {
      return (
         <div className="flex h-screen w-full items-center justify-center bg-white">
            <div className="flex flex-col items-center gap-4 text-center px-4">
               <div className="relative">
                  <Loader2 className="h-12 w-12 animate-spin text-[#006ce4]" />
               </div>
               <div>
                  <p className="text-zinc-900 font-bold text-lg">Đang xác thực quyền truy cập</p>
                  <p className="text-zinc-500 text-sm mt-1">Vui lòng đợi trong giây lát...</p>
               </div>
            </div>
         </div>
      );
   }

   return <>{children}</>;
}
