"use client";

import { useEffect, useState } from "react";
import { useAuthStore } from "@/store/useAuthStore";
import { authService } from "@/lib/api/services/authService";
import GlobalLoadingOverlay from "@/components/GlobalLoadingOverlay";

export default function AppInitializer({ children }: { children: React.ReactNode }) {
   const { isLoggedIn, setAuth, logout, isReady, setReady } = useAuthStore();
   const [mounted, setMounted] = useState(false);

   useEffect(() => {
      const timer = setTimeout(() => setMounted(true), 0);
      return () => clearTimeout(timer);
   }, []);

   useEffect(() => {
      const syncSession = async () => {
         // Đặt timeout tối đa 2.5 giây để giải phóng loading screen trong mọi tình huống
         const timeoutId = setTimeout(() => {
            setReady(true);
         }, 2500);

         try {
            if (isLoggedIn) {
               const freshUser = await authService.refresh();
               if (freshUser) {
                  setAuth(freshUser);
               }
            } else {
               // Thử refresh xem có session cookie từ trước không
               const freshUser = await authService.refresh();
               if (freshUser) {
                  setAuth(freshUser);
               }
            }
         } catch (error: unknown) {
            const err = error as { status?: number; message?: string };
            if (err?.status === 401 || err?.status === 403) {
               // Hết hạn phiên đăng nhập
               logout();
            }
         } finally {
            clearTimeout(timeoutId);
            setReady(true);
         }
      };

      if (mounted) {
         syncSession();
      }
   }, [isLoggedIn, mounted, setAuth, logout, setReady]);

   return (
      <>
         <GlobalLoadingOverlay isVisible={!isReady} />
         {children}
      </>
   );
}
