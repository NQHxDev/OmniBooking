"use client";

import { Link } from "@/i18n/routing";
import { useSearchParams } from "next/navigation";
import { Lock, ArrowRight, CheckCircle2, Eye, EyeOff } from "lucide-react";
import { useState } from "react";
import { Loader2 } from "lucide-react";
import { authService } from "@/lib/api/services/authService";
import { useTranslations } from "next-intl";
import AuthBranding from "@/components/AuthBranding";
import { toast } from "sonner";

export default function ResetPasswordPage() {
   const t = useTranslations("Auth");
   const tc = useTranslations("Common");
   const te = useTranslations("Errors");
   const searchParams = useSearchParams();
   const token = searchParams.get("token");

   const [password, setPassword] = useState("");
   const [confirmPassword, setConfirmPassword] = useState("");
   const [showPassword, setShowPassword] = useState(false);
   const [loading, setLoading] = useState(false);
   const [logoutAll, setLogoutAll] = useState(false);
   const [strength, setStrength] = useState(0);
   const [success, setSuccess] = useState(false);
   const [error, setError] = useState("");

   const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      if (!token) {
         setError("Token không hợp lệ.");
         return;
      }

      if (password !== confirmPassword) {
         setError("Mật khẩu xác nhận không khớp.");
         return;
      }

      setLoading(true);
      setError("");

      try {
         await authService.resetPassword({ token, newPassword: password, logoutAll });
         setSuccess(true);
         toast.success(t("successReset"));
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

   if (!token) {
      return (
         <div className="flex min-h-screen items-center justify-center bg-white p-8">
            <div className="text-center">
               <h1 className="text-2xl font-bold text-red-600 mb-4">Link không hợp lệ</h1>
               <p className="text-zinc-500 mb-6">
                  Đường dẫn đặt lại mật khẩu không chính xác hoặc đã hết hạn.
               </p>
               <Link
                  href="/auth/forgot-password"
                  className="text-[#006ce4] font-bold hover:underline"
               >
                  Yêu cầu link mới
               </Link>
            </div>
         </div>
      );
   }

   return (
      <div className="flex min-h-screen bg-white font-sans text-[#1a1a1a]">
         <AuthBranding />

         {/* Right Side: Form */}
         <div className="flex w-full flex-col lg:w-1/2 justify-center items-center px-8 bg-white">
            <div className="w-full max-w-[420px]">
               {!success ? (
                  <>
                     <div className="mb-10">
                        <h1 className="text-3xl font-bold tracking-tight">
                           {t("resetPasswordTitle")}
                        </h1>
                        <p className="mt-2 text-zinc-500">{t("resetPasswordSub")}</p>
                     </div>

                     {error && (
                        <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-100 text-red-600 text-sm">
                           {error}
                        </div>
                     )}

                     <form onSubmit={handleSubmit} className="space-y-5">
                        <div>
                           <label className="mb-1.5 block text-sm font-semibold text-zinc-700">
                              {t("newPasswordLabel")}
                           </label>
                           <div className="relative">
                              <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                              <input
                                 type={showPassword ? "text" : "password"}
                                 required
                                 placeholder="••••••••"
                                 value={password}
                                 onChange={(e) => {
                                    const val = e.target.value;
                                    setPassword(val);
                                    // Calculate strength
                                    let s = 0;
                                    if (val.length >= 8) s++;
                                    if (/[A-Z]/.test(val)) s++;
                                    if (/[0-9]/.test(val)) s++;
                                    if (/[^A-Za-z0-9]/.test(val)) s++;
                                    setStrength(s);
                                 }}
                                 className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4] focus:ring-4 focus:ring-blue-50"
                              />
                              <button
                                 type="button"
                                 onClick={() => setShowPassword(!showPassword)}
                                 className="absolute right-3.5 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-600 cursor-pointer"
                              >
                                 {showPassword ? (
                                    <EyeOff className="h-4 w-4" />
                                 ) : (
                                    <Eye className="h-4 w-4" />
                                 )}
                              </button>
                           </div>
                           {password && (
                              <div className="mt-2 flex gap-1">
                                 {[1, 2, 3, 4].map((level) => (
                                    <div
                                       key={level}
                                       className={`h-1 flex-1 rounded-full transition-all duration-500 ${
                                          strength >= level
                                             ? level <= 1
                                                ? "bg-red-400"
                                                : level <= 2
                                                  ? "bg-yellow-400"
                                                  : level <= 3
                                                    ? "bg-blue-400"
                                                    : "bg-emerald-400"
                                             : "bg-zinc-100"
                                       }`}
                                    />
                                 ))}
                              </div>
                           )}
                        </div>

                        <div>
                           <label className="mb-1.5 block text-sm font-semibold text-zinc-700">
                              {t("confirmPasswordLabel")}
                           </label>
                           <div className="relative">
                              <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                              <input
                                 type="password"
                                 required
                                 placeholder="••••••••"
                                 value={confirmPassword}
                                 onChange={(e) => setConfirmPassword(e.target.value)}
                                 className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4] focus:ring-4 focus:ring-blue-50"
                              />
                           </div>
                        </div>

                        <div className="flex items-center gap-3 py-3">
                           <label className="relative flex cursor-pointer items-center group">
                              <input
                                 id="logoutAll"
                                 type="checkbox"
                                 checked={logoutAll}
                                 onChange={(e) => setLogoutAll(e.target.checked)}
                                 className="peer sr-only"
                              />
                              <div className="h-6 w-6 rounded-lg border-2 border-zinc-200 bg-white transition-all duration-300 peer-checked:border-[#006ce4] peer-checked:bg-[#006ce4] group-hover:border-zinc-300 peer-focus:ring-4 peer-focus:ring-blue-50 flex items-center justify-center">
                                 <svg
                                    className={`h-3.5 w-3.5 text-white transition-all duration-300 ${logoutAll ? "scale-100 opacity-100" : "scale-0 opacity-0"}`}
                                    fill="none"
                                    viewBox="0 0 24 24"
                                    stroke="currentColor"
                                    strokeWidth="4"
                                 >
                                    <path
                                       strokeLinecap="round"
                                       strokeLinejoin="round"
                                       d="M5 13l4 4L19 7"
                                    />
                                 </svg>
                              </div>
                              <span className="ml-3 text-sm font-medium text-zinc-600 select-none group-hover:text-zinc-800 transition-colors">
                                 {t("logoutAllDevices")}
                              </span>
                           </label>
                        </div>

                        <button
                           type="submit"
                           disabled={loading}
                           className="group flex w-full items-center justify-center gap-2 rounded-xl bg-[#006ce4] py-4 text-sm font-bold text-white shadow-xl shadow-blue-200 transition-all hover:bg-[#0057b7] active:scale-[0.98] disabled:opacity-70 cursor-pointer"
                        >
                           {loading ? (
                              <Loader2 className="h-5 w-5 animate-spin" />
                           ) : (
                              <>
                                 {t("resetPasswordButton")}
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
                     <h2 className="text-2xl font-bold mb-3">{t("successReset")}</h2>
                     <p className="text-zinc-500 mb-8">{t("successResetDesc")}</p>
                     <Link
                        href="/auth/login"
                        className="inline-flex items-center gap-2 text-[#006ce4] font-bold hover:underline"
                     >
                        {tc("login")} <ArrowRight className="h-4 w-4" />
                     </Link>
                  </div>
               )}
            </div>
         </div>
      </div>
   );
}
