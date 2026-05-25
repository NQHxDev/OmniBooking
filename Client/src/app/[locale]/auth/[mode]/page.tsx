"use client";

import { Link, useRouter } from "@/i18n/routing";
import { useParams, useSearchParams } from "next/navigation";
import {
   Mail,
   Lock,
   User as UserIcon,
   ArrowRight,
   ChevronLeft,
   Loader2,
   Eye,
   EyeOff,
} from "lucide-react";

import { useState, useRef, useEffect } from "react";
import { useAuthStore, type User as UserType } from "@/store/useAuthStore";
import { authService, type ApiResponse } from "@/lib/api/services/authService";
import { toast } from "sonner";
import { useTranslations } from "next-intl";
import AuthBranding from "@/components/AuthBranding";
import Turnstile, { type TurnstileRef } from "@/components/Turnstile";
import { env } from "@/env";

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
      rememberMe: false,
   });
   const [loading, setLoading] = useState(false);
   const [showPassword, setShowPassword] = useState(false);
   const [error, setError] = useState("");
   const [showOtp, setShowOtp] = useState(false);
   const [otpCode, setOtpCode] = useState("");
   const [turnstileToken, setTurnstileToken] = useState<string | null>(null);
   const turnstileRef = useRef<TurnstileRef>(null);

   const [debouncedPassword, setDebouncedPassword] = useState("");
   const [isPasswordTyping, setIsPasswordTyping] = useState(false);

   const getMissingRequirements = (pw: string) => {
      const missing = [];
      if (pw.length < 6) missing.push("passwordMinLength");
      if (!/[A-Z]/.test(pw)) missing.push("passwordRequireUppercase");
      if (!/[a-z]/.test(pw)) missing.push("passwordRequireLowercase");
      if (!/[0-9]/.test(pw)) missing.push("passwordRequireNumber");
      if (!/[^A-Za-z0-9]/.test(pw)) missing.push("passwordRequireSpecial");
      return missing;
   };

   useEffect(() => {
      if (isLogin || !formData.password) return;

      const timer = setTimeout(() => {
         setDebouncedPassword(formData.password);
         setIsPasswordTyping(false);
      }, 500); // 500ms debounce

      return () => clearTimeout(timer);
   }, [formData.password, isLogin]);

   const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const { name, value, type, checked } = e.target;
      setFormData({
         ...formData,
         [name]: type === "checkbox" ? checked : value,
      });

      if (name === "password" && !isLogin) {
         if (!value) {
            setDebouncedPassword("");
            setIsPasswordTyping(false);
         } else {
            setIsPasswordTyping(true);
         }
      }
   };

   const handleToggle = (login: boolean) => {
      setError("");
      setTurnstileToken(null);
      turnstileRef.current?.reset();
      router.push(`/auth/${login ? "login" : "register"}`, { scroll: false });
   };

   const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      setLoading(true);
      setError("");

      try {
         if (showOtp) {
            const finalUser = await authService.loginWith2FA({
               email: formData.email,
               password: formData.password,
               code: otpCode,
               rememberMe: formData.rememberMe,
            });
            setAuth(finalUser as UserType);
            const targetUrl = callbackUrl || "/";
            router.push(targetUrl);
            router.refresh();
            return;
         }

         if (!isLogin) {
            const missing = getMissingRequirements(formData.password);
            if (missing.length > 0) {
               setError(t("passwordMissingRequirements") + missing.map((m) => t(m)).join(", "));
               setLoading(false);
               return;
            }
         }

         if (!turnstileToken) {
            setError(te("AUTH_018"));
            setLoading(false);
            return;
         }

         const result = isLogin
            ? await authService.login({
                 email: formData.email,
                 password: formData.password,
                 rememberMe: formData.rememberMe,
                 turnstileToken,
              })
            : await authService.register({
                 email: formData.email,
                 password: formData.password,
                 fullName: formData.fullName,
                 rememberMe: formData.rememberMe,
                 turnstileToken,
              });

         if (isLogin) {
            setAuth(result as UserType);
            const targetUrl = callbackUrl || "/";
            router.push(targetUrl);
            router.refresh();
         } else {
            // Handle Async Registration via SSE
            const registerResponse = result as ApiResponse<UserType>;
            const requestId = registerResponse.requestId;

            if (requestId) {
               // Ensure the URL always goes through /api/v1 context path to avoid 404
               const host = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1/";
               const baseUrl = host.includes("/api/v1")
                  ? host.split("/api/v1")[0]
                  : host.replace(/\/$/, "");
               const fullSseUrl = `${baseUrl}/api/v1/auth/subscribe/${requestId}`;

               const eventSource = new EventSource(fullSseUrl);
               let sseCompleted = false;

               eventSource.addEventListener("REGISTRATION_COMPLETE", async (e) => {
                  try {
                     sseCompleted = true;
                     const userData = JSON.parse(e.data);
                     const accessToken = userData.accessToken;

                     // 1. Finalize registration to set HttpOnly cookies
                     const finalUserData = await authService.finalizeRegistration(accessToken);

                     // 2. Set local auth state
                     setAuth(finalUserData);
                     eventSource.close();

                     toast.success(t("successRegister"), {
                        description: t("successRegisterDesc"),
                        duration: 6000,
                     });

                     const targetUrl = callbackUrl || "/";
                     router.push(targetUrl);
                     router.refresh();
                  } catch (err) {
                     console.error("Failed to finalize registration session:", err);
                     setError(te("GEN_999"));
                     setLoading(false);
                     eventSource.close();
                  }
               });

               eventSource.onerror = (err) => {
                  // Only show error if we haven't successfully completed the registration flow
                  if (!sseCompleted) {
                     console.error("SSE Error:", err);
                     setError(te("GEN_999"));
                     setLoading(false);
                  }
                  eventSource.close();
               };
            }
         }
      } catch (err: unknown) {
         // Reset Turnstile on error
         turnstileRef.current?.reset();
         setTurnstileToken(null);

         const error = err as { message?: string; errorCode?: string };

         if (error?.errorCode === "AUTH_010") {
            setShowOtp(true);
            setError("");
            return;
         }

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

                  {!showOtp && (
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
                  )}

                  <div className="mb-8">
                     <h1 className="text-3xl font-bold tracking-tight">
                        {showOtp
                           ? t("twoFactorLoginTitle")
                           : isLogin
                             ? t("loginTitle")
                             : t("registerTitle")}
                     </h1>
                     <p className="mt-2 text-zinc-500">
                        {showOtp
                           ? t("twoFactorLoginSub")
                           : isLogin
                             ? t("loginSub")
                             : t("registerSub")}
                     </p>
                  </div>

                  {error && (
                     <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-100 text-red-600 text-sm">
                        {error}
                     </div>
                  )}

                  <form onSubmit={handleSubmit} className="space-y-5">
                     {showOtp ? (
                        <div>
                           <label
                              htmlFor="otpCode"
                              className="mb-1.5 block text-sm font-semibold text-zinc-700 cursor-pointer"
                           >
                              {t("otpLabel")}
                           </label>
                           <div className="relative">
                              <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                              <input
                                 id="otpCode"
                                 name="otpCode"
                                 type="text"
                                 required
                                 maxLength={8}
                                 placeholder={t("otpPlaceholder")}
                                 value={otpCode}
                                 onChange={(e) => setOtpCode(e.target.value)}
                                 className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4] focus:ring-4 focus:ring-blue-50 tracking-[0.2em] font-mono text-center"
                              />
                           </div>
                        </div>
                     ) : (
                        <>
                           {!isLogin && (
                              <div>
                                 <label
                                    htmlFor="fullName"
                                    className="mb-1.5 block text-sm font-semibold text-zinc-700 cursor-pointer"
                                 >
                                    {t("fullNameLabel")}
                                 </label>
                                 <div className="relative">
                                    <UserIcon className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
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
                              <div className="mb-1.5">
                                 <label
                                    htmlFor="password"
                                    className="text-sm font-bold text-zinc-700 cursor-pointer"
                                 >
                                    {t("passwordLabel")}
                                 </label>
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
                              {!isLogin &&
                                 formData.password &&
                                 !isPasswordTyping &&
                                 (() => {
                                    const missingList = getMissingRequirements(debouncedPassword);
                                    if (missingList.length === 0) return null;
                                    return (
                                       <div className="mt-2.5 text-xs text-red-500 bg-red-50/60 border border-red-100 rounded-xl p-3.5 space-y-1.5 animate-in fade-in slide-in-from-top-1 duration-200">
                                          <span className="font-bold text-red-700">
                                             {t("passwordMissingRequirements")}
                                          </span>
                                          <ul className="list-disc list-inside mt-1 space-y-1 pl-1 text-red-600 font-medium">
                                             {missingList.map((reqKey) => (
                                                <li key={reqKey}>{t(reqKey)}</li>
                                             ))}
                                          </ul>
                                       </div>
                                    );
                                 })()}
                           </div>

                           <div className="flex items-center justify-between">
                              <label className="flex items-center gap-2 cursor-pointer group">
                                 <div className="relative flex items-center justify-center h-4.5 w-4.5 rounded border border-zinc-300 bg-zinc-50 transition-all group-hover:border-[#006ce4]">
                                    <input
                                       type="checkbox"
                                       name="rememberMe"
                                       checked={formData.rememberMe}
                                       onChange={handleChange}
                                       className="peer absolute inset-0 opacity-0 cursor-pointer"
                                    />
                                    <div className="h-2.5 w-2.5 rounded-[1px] bg-[#006ce4] opacity-0 transition-opacity peer-checked:opacity-100" />
                                 </div>
                                 <span className="text-sm font-medium text-zinc-600 group-hover:text-zinc-900 transition-colors">
                                    {t("rememberMe")}
                                 </span>
                              </label>

                              {isLogin && (
                                 <Link
                                    href={{
                                       pathname: "/auth/forgot-password",
                                       query: { email: formData.email },
                                    }}
                                    className="text-[11px] font-bold uppercase tracking-wider text-[#006ce4] hover:text-[#0057b7] cursor-pointer transition-colors"
                                 >
                                    {t("forgotPassword")}
                                 </Link>
                              )}
                           </div>
                        </>
                     )}

                     {!showOtp && (
                        <Turnstile
                           ref={turnstileRef}
                           siteKey={env.NEXT_PUBLIC_TURNSTILE_SITE_KEY}
                           onVerify={(token) => setTurnstileToken(token)}
                           onError={() => setTurnstileToken(null)}
                           onExpire={() => setTurnstileToken(null)}
                           theme="light"
                        />
                     )}

                     <button
                        type="submit"
                        disabled={loading}
                        className="group flex w-full items-center justify-center gap-2 rounded-xl bg-[#006ce4] py-4 text-sm font-bold text-white shadow-xl shadow-blue-200 transition-all hover:bg-[#0057b7] active:scale-[0.98] disabled:opacity-70 cursor-pointer"
                     >
                        {loading ? (
                           <Loader2 className="h-5 w-5 animate-spin" />
                        ) : (
                           <>
                              {showOtp
                                 ? t("verify2FAButton")
                                 : isLogin
                                   ? t("loginButton")
                                   : t("registerButton")}
                              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
                           </>
                        )}
                     </button>

                     {showOtp && (
                        <button
                           type="button"
                           onClick={() => {
                              setShowOtp(false);
                              setOtpCode("");
                              setError("");
                           }}
                           className="flex w-full items-center justify-center gap-2 rounded-xl border border-zinc-200 bg-white py-3.5 text-sm font-bold shadow-sm hover:bg-zinc-50 hover:border-zinc-300 transition-all active:scale-[0.98] cursor-pointer"
                        >
                           {t("backToCredentials")}
                        </button>
                     )}
                  </form>

                  {!showOtp && (
                     <>
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
                              onClick={() => handleOAuthLogin("zalo")}
                              disabled={loading}
                              className="flex items-center justify-center gap-2 rounded-xl bg-[#0068ff] py-3.5 text-sm font-bold text-white shadow-sm hover:bg-[#0055d4] transition-all active:scale-[0.98] disabled:opacity-70 cursor-pointer"
                           >
                              {loading ? (
                                 <Loader2 className="h-5 w-5 animate-spin text-white" />
                              ) : (
                                 <>
                                    <ZaloIcon className="h-5 w-5" />
                                    Zalo
                                 </>
                              )}
                           </button>
                        </div>
                     </>
                  )}
               </div>
            </div>
         </div>
      </div>
   );
}

function ZaloIcon({ className }: { className?: string }) {
   return (
      <svg viewBox="0 0 48 48" className={className} xmlns="http://www.w3.org/2000/svg">
         <path
            fill="currentColor"
            d="M38.86 21c-2.3-5.2-12.86-9-14.86-9S9.26 15.8 7 21a11.16 11.16 0 0 0 0 8c2.26 5.2 12.86 9 14.86 9a38.42 38.42 0 0 0 6.64-.6A22.5 22.5 0 0 1 31.83 40c2.8 1.44 5 .51 5-2a13.31 13.31 0 0 0-1.28-4.63A14 14 0 0 0 38.86 29a11.16 11.16 0 0 0 0-8Zm-15 10a7.07 7.07 0 0 1-5.1-2.14 7.21 7.21 0 0 1-2.12-5.11A7.23 7.23 0 0 1 23.86 16.5a7.22 7.22 0 0 1 7.22 7.25 7.24 7.24 0 0 1-7.22 7.25Z"
         />
      </svg>
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
