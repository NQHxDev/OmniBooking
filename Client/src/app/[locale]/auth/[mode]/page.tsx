"use client";

import { Link } from "@/i18n/routing";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import {
   Mail,
   Lock,
   User,
   ArrowRight,
   ChevronLeft,
   Apple,
   Loader2,
   Eye,
   EyeOff,
} from "lucide-react";

import { useState } from "react";
import { useAuthStore } from "@/store/useAuthStore";
import { authService } from "@/lib/api/services/authService";
import { toast } from "sonner";
import { useTranslations } from "next-intl";
import AuthBranding from "@/components/AuthBranding";

export default function AuthPage() {
   const t = useTranslations("Auth");
   const tc = useTranslations("Common");
   const te = useTranslations("Errors");
   const params = useParams();
   const router = useRouter();
   const mode = params?.mode as string;
   const searchParams = useSearchParams();
   const isLogin = mode === "login";
   const callbackUrl = searchParams.get("callbackUrl");

   const setAuth = useAuthStore((state) => state.setAuth);

   const [formData, setFormData] = useState({
      email: "",
      password: "",
      fullName: "",
   });
   const [loading, setLoading] = useState(false);
   const [showPassword, setShowPassword] = useState(false);
   const [error, setError] = useState("");

   const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      setFormData({ ...formData, [e.target.name]: e.target.value });
   };

   const handleToggle = (login: boolean) => {
      setError("");
      router.push(`/auth/${login ? "login" : "register"}`, { scroll: false });
   };

   const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      setLoading(true);
      setError("");

      try {
         const result = isLogin
            ? await authService.login({ email: formData.email, password: formData.password })
            : await authService.register({
                 email: formData.email,
                 password: formData.password,
                 fullName: formData.fullName,
              });

         if (result) {
            setAuth(result);
            if (!isLogin) {
               toast.success(t("successRegister"), {
                  description: t("successRegisterDesc"),
                  duration: 6000,
               });
            }

            const targetUrl = callbackUrl || "/";
            router.push(targetUrl);
            router.refresh();
         }
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

   const handleOAuthLogin = async (provider: string) => {
      setLoading(true);
      try {
         const response = await authService.getOAuth2Url(provider);
         if (response && response.data) {
            window.location.href = response.data;
         }
      } catch (err) {
         const error = err as { message?: string; errorCode?: string };
         let errorMessage = error?.message || `${provider} login failed`;
         if (error?.errorCode) {
            errorMessage = te(error.errorCode);
         }
         toast.error(errorMessage);
         setLoading(false);
      }
   };

   return (
      <div className="flex min-h-screen bg-white font-sans text-[#1a1a1a]">
         <AuthBranding />

         {/* Right Side: Auth Form */}
         <div className="flex w-full flex-col lg:w-1/2 h-screen overflow-y-auto custom-scrollbar bg-white">
            <div className="flex flex-1 flex-col items-center px-8 py-12 lg:py-20">
               <div className="w-full max-w-[420px]">
                  <Link
                     href="/"
                     className="mb-8 hidden items-center gap-2 text-sm font-medium text-zinc-500 hover:text-[#006ce4] lg:flex transition-colors"
                  >
                     <ChevronLeft className="h-4 w-4" /> {t("backToHome")}
                  </Link>

                  <div className="mb-10 flex p-1 bg-zinc-100 rounded-xl">
                     <button
                        onClick={() => handleToggle(true)}
                        className={`flex-1 py-2.5 text-sm font-bold rounded-lg transition-all duration-300 ${isLogin ? "bg-white text-[#006ce4] shadow-sm" : "text-zinc-500 hover:text-zinc-700"}`}
                     >
                        {tc("login")}
                     </button>
                     <button
                        onClick={() => handleToggle(false)}
                        className={`flex-1 py-2.5 text-sm font-bold rounded-lg transition-all duration-300 ${!isLogin ? "bg-white text-[#006ce4] shadow-sm" : "text-zinc-500 hover:text-zinc-700"}`}
                     >
                        {tc("register")}
                     </button>
                  </div>

                  <div className="mb-8">
                     <h1 className="text-3xl font-bold tracking-tight">
                        {isLogin ? t("loginTitle") : t("registerTitle")}
                     </h1>
                     <p className="mt-2 text-zinc-500">
                        {isLogin ? t("loginSub") : t("registerSub")}
                     </p>
                  </div>

                  {error && (
                     <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-100 text-red-600 text-sm">
                        {error}
                     </div>
                  )}

                  <form onSubmit={handleSubmit} className="space-y-5">
                     {!isLogin && (
                        <div>
                           <label
                              htmlFor="fullName"
                              className="mb-1.5 block text-sm font-semibold text-zinc-700 cursor-pointer"
                           >
                              {t("fullNameLabel")}
                           </label>
                           <div className="relative">
                              <User className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                              <input
                                 id="fullName"
                                 name="fullName"
                                 type="text"
                                 required
                                 placeholder={t("fullNamePlaceholder")}
                                 value={formData.fullName}
                                 onChange={handleChange}
                                 className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4] focus:ring-4 focus:ring-blue-50"
                              />
                           </div>
                        </div>
                     )}

                     <div>
                        <label
                           htmlFor="email"
                           className="mb-1.5 block text-sm font-semibold text-zinc-700 cursor-pointer"
                        >
                           {t("emailLabel")}
                        </label>
                        <div className="relative">
                           <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                           <input
                              id="email"
                              name="email"
                              type="email"
                              required
                              placeholder="name@company.com"
                              value={formData.email}
                              onChange={handleChange}
                              className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4] focus:ring-4 focus:ring-blue-50"
                           />
                        </div>
                     </div>

                     <div>
                        <div className="flex justify-between items-end mb-1.5">
                           <label
                              htmlFor="password"
                              className="text-sm font-bold text-zinc-700 cursor-pointer"
                           >
                              {t("passwordLabel")}
                           </label>
                           {isLogin && (
                              <Link
                                 href={{
                                    pathname: "/auth/forgot-password",
                                    query: { email: formData.email },
                                 }}
                                 className="text-[11px] font-bold uppercase tracking-wider text-[#006ce4] hover:text-[#0057b7] cursor-pointer"
                              >
                                 {t("forgotPassword")}
                              </Link>
                           )}
                        </div>
                        <div className="relative">
                           <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                           <input
                              id="password"
                              name="password"
                              type={showPassword ? "text" : "password"}
                              required
                              placeholder="••••••••"
                              value={formData.password}
                              onChange={handleChange}
                              className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4] focus:ring-4 focus:ring-blue-50"
                           />
                           <button
                              type="button"
                              onClick={() => setShowPassword(!showPassword)}
                              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-600 transition-colors cursor-pointer"
                           >
                              {showPassword ? (
                                 <EyeOff className="h-4 w-4" />
                              ) : (
                                 <Eye className="h-4 w-4" />
                              )}
                           </button>
                        </div>
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
                              {isLogin ? t("loginButton") : t("registerButton")}
                              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
                           </>
                        )}
                     </button>
                  </form>

                  <div className="relative my-8 text-center">
                     <span className="relative z-10 bg-white px-4 text-xs font-bold uppercase tracking-widest text-zinc-400">
                        {t("orContinueWith")}
                     </span>
                     <div className="absolute inset-0 top-1/2 border-t border-zinc-100"></div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                     <button
                        type="button"
                        onClick={() => handleOAuthLogin("google")}
                        disabled={loading}
                        className="flex items-center justify-center gap-2 rounded-xl border border-zinc-200 bg-white py-3.5 text-sm font-bold shadow-sm hover:bg-zinc-50 hover:border-zinc-300 transition-all active:scale-[0.98] disabled:opacity-70 cursor-pointer"
                     >
                        {loading ? (
                           <Loader2 className="h-5 w-5 animate-spin text-[#006ce4]" />
                        ) : (
                           <>
                              <GoogleIcon className="h-5 w-5" />
                              Google
                           </>
                        )}
                     </button>
                     <button
                        type="button"
                        onClick={() => handleOAuthLogin("apple")}
                        disabled={loading}
                        className="flex items-center justify-center gap-2 rounded-xl bg-black py-3.5 text-sm font-bold text-white shadow-sm hover:bg-zinc-800 transition-all active:scale-[0.98] disabled:opacity-70 cursor-pointer"
                     >
                        {loading ? (
                           <Loader2 className="h-5 w-5 animate-spin text-white" />
                        ) : (
                           <>
                              <Apple className="h-5 w-5" />
                              Apple
                           </>
                        )}
                     </button>
                  </div>
               </div>
            </div>
         </div>
      </div>
   );
}

function GoogleIcon({ className }: { className?: string }) {
   return (
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" className={className}>
         <path
            fill="#FFC107"
            d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z"
         />
         <path
            fill="#FF3D00"
            d="m6.306 14.691 6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691z"
         />
         <path
            fill="#4CAF50"
            d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238A11.91 11.91 0 0 1 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z"
         />
         <path
            fill="#1976D2"
            d="M43.611 20.083H42V20H24v8h11.303a12.04 12.04 0 0 1-4.087 5.571l.003-.002 6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z"
         />
      </svg>
   );
}
