"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuthStore } from "@/store/useAuthStore";
import { authService } from "@/lib/api/services/authService";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";

export default function AuthCallbackPage() {
   const router = useRouter();
   const searchParams = useSearchParams();
   const setAuth = useAuthStore((state) => state.setAuth);
   const t = useTranslations("Auth");
   const te = useTranslations("Errors");
   const [error, setError] = useState<string | null>(null);

   useEffect(() => {
      const handleCallback = async () => {
         const errorParam = searchParams.get("error");
         if (errorParam) {
            setError(te("OAUTH2_AUTHENTICATION_FAILED"));
            toast.error(te("OAUTH2_AUTHENTICATION_FAILED"));
            setTimeout(() => router.push("/auth/login"), 3000);
            return;
         }

         try {
            // The server has already set the HttpOnly cookies.
            // We just need to refresh to get the user data into our Zustand store.
            const user = await authService.refresh();
            if (user) {
               setAuth(user);
               toast.success(t("loginSuccess"));
               router.push("/");
               router.refresh();
            } else {
               throw new Error("Failed to get user data");
            }
         } catch (err) {
            console.error("Auth callback error:", err);
            setError(te("OAUTH2_AUTHENTICATION_FAILED"));
            toast.error(te("OAUTH2_AUTHENTICATION_FAILED"));
            setTimeout(() => router.push("/auth/login"), 3000);
         }
      };

      handleCallback();
   }, [router, searchParams, setAuth, t, te]);

   return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-white p-4 text-center">
         <div className="w-full max-w-md space-y-6">
            {!error ? (
               <>
                  <div className="relative flex justify-center">
                     <div className="absolute h-24 w-24 animate-ping rounded-full bg-blue-100 opacity-75"></div>
                     <div className="relative flex h-24 w-24 items-center justify-center rounded-full bg-blue-50 shadow-inner">
                        <Loader2 className="h-10 w-10 animate-spin text-[#006ce4]" />
                     </div>
                  </div>
                  <div className="space-y-2">
                     <h1 className="text-2xl font-bold tracking-tight text-zinc-900">
                        {t("verifyingLogin")}
                     </h1>
                     <p className="text-zinc-500">{t("callbackSubtext")}</p>
                  </div>
               </>
            ) : (
               <div className="space-y-4">
                  <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-red-100 text-red-600">
                     <svg
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                        strokeWidth={2}
                        stroke="currentColor"
                        className="w-8 h-8"
                     >
                        <path
                           strokeLinecap="round"
                           strokeLinejoin="round"
                           d="M6 18L18 6M6 6l12 12"
                        />
                     </svg>
                  </div>
                  <h1 className="text-xl font-bold text-zinc-900">{t("loginFailed")}</h1>
                  <p className="text-zinc-500">{error}</p>
                  <p className="text-sm text-zinc-400">{t("redirectingToLogin")}</p>
               </div>
            )}
         </div>
      </div>
   );
}
