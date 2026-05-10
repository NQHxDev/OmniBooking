"use client";

import { Link } from "@/i18n/routing";
import { useSearchParams } from "next/navigation";
import { Mail, ArrowRight, ChevronLeft, CheckCircle2 } from "lucide-react";
import { useState, useEffect } from "react";
import { Loader2 } from "lucide-react";
import { authService } from "@/lib/api/services/authService";
import { useTranslations } from "next-intl";
import AuthBranding from "@/components/AuthBranding";

export default function ForgotPasswordPage() {
   const t = useTranslations("Auth");
   const te = useTranslations("Errors");
   const searchParams = useSearchParams();

   const [email, setEmail] = useState("");
   const [loading, setLoading] = useState(false);
   const [submitted, setSubmitted] = useState(false);
   const [error, setError] = useState("");

   const isValidEmail = (emailStr: string) => {
      return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailStr);
   };

   useEffect(() => {
      const emailParam = searchParams.get("email");
      if (emailParam) {
         setTimeout(() => setEmail(emailParam), 0);
      }
   }, [searchParams]);

   const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      setLoading(true);
      setError("");

      try {
         await authService.forgotPassword(email);
         setSubmitted(true);
      } catch (err: unknown) {
         const error = err as { message?: string; errorCode?: string };
         let errorMessage = error?.message || "Đã có lỗi xảy ra";

         if (error?.errorCode) {
            errorMessage = te(error.errorCode as string);
         }

         setError(errorMessage);
      } finally {
         setLoading(false);
      }
   };

   return (
      <div className="flex min-h-screen bg-white font-sans text-[#1a1a1a]">
         <AuthBranding />

         {/* Right Side: Form */}
         <div className="flex w-full flex-col lg:w-1/2 justify-center items-center px-8 bg-white">
            <div className="w-full max-w-[420px]">
               <Link
                  href="/auth/login"
                  className="mb-8 flex items-center gap-2 text-sm font-medium text-zinc-500 hover:text-[#006ce4] transition-colors"
               >
                  <ChevronLeft className="h-4 w-4" /> {t("backToLogin")}
               </Link>

               {!submitted ? (
                  <>
                     <div className="mb-10">
                        <h1 className="text-3xl font-bold tracking-tight">
                           {t("forgotPasswordTitle")}
                        </h1>
                        <p className="mt-2 text-zinc-500">{t("forgotPasswordSub")}</p>
                     </div>

                     {error && (
                        <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-100 text-red-600 text-sm">
                           {error}
                        </div>
                     )}

                     <form onSubmit={handleSubmit} className="space-y-6">
                        <div>
                           <label
                              htmlFor="email"
                              className="mb-1.5 block text-sm font-semibold text-zinc-700"
                           >
                              {t("emailLabel")}
                           </label>
                           <div className="relative">
                              <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                              <input
                                 id="email"
                                 type="email"
                                 required
                                 placeholder="name@company.com"
                                 value={email}
                                 onChange={(e) => setEmail(e.target.value)}
                                 className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4] focus:ring-4 focus:ring-blue-50"
                              />
                           </div>
                        </div>

                        <button
                           type="submit"
                           disabled={loading || !isValidEmail(email)}
                           className="group flex w-full items-center justify-center gap-2 rounded-xl bg-[#006ce4] py-4 text-sm font-bold text-white shadow-xl shadow-blue-200 transition-all hover:bg-[#0057b7] active:scale-[0.98] cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                           {loading ? (
                              <Loader2 className="h-5 w-5 animate-spin" />
                           ) : (
                              <>
                                 {t("sendResetLink")}
                                 <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
                              </>
                           )}
                        </button>
                     </form>
                  </>
               ) : (
                  <div className="text-center">
                     <div className="inline-flex h-20 w-20 items-center justify-center rounded-full bg-green-50 mb-6">
                        <CheckCircle2 className="h-10 w-10 text-green-500" />
                     </div>
                     <h2 className="text-2xl font-bold mb-3">{t("successForgot")}</h2>
                     <p className="text-zinc-500 mb-8">{t("successForgotDesc")}</p>
                     <Link
                        href="/auth/login"
                        className="inline-flex items-center gap-2 text-[#006ce4] font-bold hover:underline"
                     >
                        {t("backToLogin")} <ArrowRight className="h-4 w-4" />
                     </Link>
                  </div>
               )}
            </div>
         </div>
      </div>
   );
}
