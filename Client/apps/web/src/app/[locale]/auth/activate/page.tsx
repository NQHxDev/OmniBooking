"use client";

import { useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { useLocale } from "next-intl";
import { Lock, CheckCircle, AlertCircle, ArrowRight, Sparkles } from "lucide-react";
import { useAuthStore } from "@/store/useAuthStore";
import { authService } from "@/lib/api/services/authService";

export default function EmailActivationPage() {
   const locale = useLocale();
   const router = useRouter();
   const searchParams = useSearchParams();
   const token = searchParams.get("token");

   const { setAuth } = useAuthStore();

   // Form states
   const [password, setPassword] = useState("");
   const [confirmPassword, setConfirmPassword] = useState("");
   const [activating, setActivating] = useState(false);
   const [error, setError] = useState<string | null>(
      !token
         ? locale === "vi"
            ? "Mã xác thực không hợp lệ hoặc đã hết hạn."
            : "Invalid or expired activation link."
         : null
   );
   const [success, setSuccess] = useState(false);

   const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      if (!token) return;

      if (password !== confirmPassword) {
         setError(locale === "vi" ? "Mật khẩu xác nhận không khớp." : "Passwords do not match.");
         return;
      }

      if (password.length < 6) {
         setError(
            locale === "vi"
               ? "Mật khẩu phải chứa ít nhất 6 ký tự."
               : "Password must be at least 6 characters."
         );
         return;
      }

      setActivating(true);
      setError(null);

      try {
         const user = await authService.activateGuest(token, password);
         setAuth(user);
         setSuccess(true);
         setTimeout(() => {
            router.push(`/${locale}/profile`);
         }, 2000);
      } catch (err) {
         console.error(err);
         const errorWithResponse = err as { response?: { data?: { message?: string } } };
         setError(
            errorWithResponse.response?.data?.message ||
               (locale === "vi"
                  ? "Kích hoạt tài khoản thất bại. Vui lòng kiểm tra lại đường dẫn trong email."
                  : "Activation failed. Please check the email link again.")
         );
         setActivating(false);
      }
   };

   return (
      <div className="min-h-screen bg-zinc-50 flex items-center justify-center p-4 font-sans">
         <div className="bg-white rounded-3xl border border-zinc-200 shadow-sm p-6 sm:p-8 max-w-md w-full space-y-6">
            {/* Header */}
            <div className="text-center space-y-2">
               <div className="inline-flex p-3 bg-blue-50 rounded-full border border-blue-100 mb-2">
                  <Sparkles className="h-8 w-8 text-blue-600" />
               </div>
               <h1 className="text-2xl font-extrabold text-zinc-950">
                  {locale === "vi" ? "Kích hoạt tài khoản thành viên" : "Activate Membership"}
               </h1>
               <p className="text-xs text-zinc-500 font-medium">
                  {locale === "vi"
                     ? "Thiết lập mật khẩu của bạn bên dưới để kích hoạt tài khoản OmniBooking."
                     : "Set your password below to activate your OmniBooking account."}
               </p>
            </div>

            {error && (
               <div className="bg-red-50 border border-red-200 rounded-xl p-3.5 flex items-start gap-2.5 text-red-700 text-xs font-semibold animate-in fade-in duration-200">
                  <AlertCircle className="h-4.5 w-4.5 text-red-500 shrink-0 mt-0.5" />
                  <span>{error}</span>
               </div>
            )}

            {success ? (
               <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-6 text-center space-y-3 animate-in fade-in duration-300">
                  <CheckCircle className="h-10 w-10 text-emerald-600 mx-auto" />
                  <h3 className="text-sm font-bold text-emerald-950">
                     {locale === "vi"
                        ? "Kích hoạt tài khoản thành viên thành công!"
                        : "Activation Successful!"}
                  </h3>
                  <p className="text-xs text-emerald-800/80 leading-relaxed font-semibold">
                     {locale === "vi"
                        ? "Hệ thống đang tự động đăng nhập và đưa bạn tới trang cá nhân..."
                        : "Logging you in and redirecting to your profile dashboard..."}
                  </p>
               </div>
            ) : (
               <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="space-y-1.5">
                     <label className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider block">
                        {locale === "vi" ? "Mật khẩu mới" : "New Password"}
                     </label>
                     <div className="relative">
                        <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-400">
                           <Lock className="h-4.5 w-4.5" />
                        </span>
                        <input
                           type="password"
                           required
                           placeholder="******"
                           value={password}
                           onChange={(e) => setPassword(e.target.value)}
                           className="w-full pl-9 pr-4 py-3 border border-zinc-200 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-hidden transition-all text-xs font-bold"
                        />
                     </div>
                  </div>

                  <div className="space-y-1.5">
                     <label className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider block">
                        {locale === "vi" ? "Xác nhận mật khẩu mới" : "Confirm New Password"}
                     </label>
                     <div className="relative">
                        <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-400">
                           <Lock className="h-4.5 w-4.5" />
                        </span>
                        <input
                           type="password"
                           required
                           placeholder="******"
                           value={confirmPassword}
                           onChange={(e) => setConfirmPassword(e.target.value)}
                           className="w-full pl-9 pr-4 py-3 border border-zinc-200 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-hidden transition-all text-xs font-bold"
                        />
                     </div>
                  </div>

                  <button
                     type="submit"
                     disabled={activating || !token}
                     className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3.5 rounded-xl transition-all cursor-pointer shadow-md text-xs flex items-center justify-center gap-2 hover:shadow-lg disabled:opacity-50"
                  >
                     {activating ? (
                        <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                     ) : (
                        <>
                           <span>
                              {locale === "vi" ? "Kích hoạt & Đăng nhập" : "Activate & Log In"}
                           </span>
                           <ArrowRight className="h-4.5 w-4.5" />
                        </>
                     )}
                  </button>
               </form>
            )}
         </div>
      </div>
   );
}
