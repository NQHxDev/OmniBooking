"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { Mail, Lock, User, ArrowRight, ChevronLeft, Loader2 } from "lucide-react";
import { useState } from "react";
import { useAuthStore } from "@/store/useAuthStore";
import { authService } from "@/lib/api/services/authService";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

export default function AuthPage() {
   const t = useTranslations("Auth");
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
         // Map backend error code to translation key
         const error = err as { errorCode?: string };
         const errorCode = error?.errorCode || "GEN_999";
         const errorMessage = te(errorCode) || t("uncategorizedError");
         setError(errorMessage);
      } finally {
         setLoading(false);
      }
   };

   return (
      <div className="flex min-h-screen bg-white font-sans text-[#1a1a1a]">
         {/* Left Side: Branding */}
         <div className="relative hidden w-1/2 overflow-hidden lg:block">
            <Image
               src="/images/hero_banner.png"
               alt="Auth Background"
               fill
               className="object-cover transition-transform duration-10000 hover:scale-110"
               priority
            />
            <div className="absolute inset-0 bg-gradient-to-br from-[#003580]/80 via-[#003580]/40 to-transparent" />

            <div className="absolute inset-0 flex flex-col justify-between p-16 text-white">
               <Link
                  href="/"
                  className="flex items-center gap-2 text-2xl font-black tracking-tighter"
               >
                  <span className="tracking-tight">
                     OmniBooking<span className="text-blue-400">.</span>
                  </span>
               </Link>

               <div className="max-w-xl">
                  <h2 className="text-5xl font-extrabold leading-[1.1] tracking-tight">
                     {t("loginTitle")}
                  </h2>
                  <p className="mt-6 text-lg text-white/70 leading-relaxed">{t("loginSub")}</p>
               </div>

               <div className="flex items-center gap-6 text-xs font-medium text-white/40">
                  <span>© 2026 OmniBooking™</span>
               </div>
            </div>
         </div>

         {/* Right Side: Form */}
         <div className="flex w-full flex-col lg:w-1/2">
            <div className="flex flex-1 items-center justify-center px-8 py-12">
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
                        className={`flex-1 py-2.5 text-sm font-bold rounded-lg transition-all ${isLogin ? "bg-white text-[#006ce4] shadow-sm" : "text-zinc-500"}`}
                     >
                        {t("loginButton")}
                     </button>
                     <button
                        onClick={() => handleToggle(false)}
                        className={`flex-1 py-2.5 text-sm font-bold rounded-lg transition-all ${!isLogin ? "bg-white text-[#006ce4] shadow-sm" : "text-zinc-500"}`}
                     >
                        {t("registerButton")}
                     </button>
                  </div>

                  <div className="mb-8">
                     <h1 className="text-3xl font-bold tracking-tight">
                        {isLogin ? t("loginTitle") : t("registerTitle")}
                     </h1>
                  </div>

                  {error && (
                     <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-100 text-red-600 text-sm animate-in fade-in slide-in-from-top-1">
                        {error}
                     </div>
                  )}

                  <form onSubmit={handleSubmit} className="space-y-5">
                     {!isLogin && (
                        <div>
                           <label className="mb-1.5 block text-sm font-semibold text-zinc-700">
                              {t("fullNameLabel")}
                           </label>
                           <div className="relative">
                              <User className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                              <input
                                 name="fullName"
                                 type="text"
                                 required
                                 placeholder="Nguyễn Văn A"
                                 value={formData.fullName}
                                 onChange={handleChange}
                                 className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4]"
                              />
                           </div>
                        </div>
                     )}

                     <div>
                        <label className="mb-1.5 block text-sm font-semibold text-zinc-700">
                           {t("emailLabel")}
                        </label>
                        <div className="relative">
                           <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                           <input
                              name="email"
                              type="email"
                              required
                              placeholder="name@company.com"
                              value={formData.email}
                              onChange={handleChange}
                              className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4]"
                           />
                        </div>
                     </div>

                     <div>
                        <div className="flex justify-between items-center mb-1.5">
                           <label className="text-sm font-semibold text-zinc-700">
                              {t("passwordLabel")}
                           </label>
                        </div>
                        <div className="relative">
                           <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                           <input
                              name="password"
                              type="password"
                              required
                              placeholder="••••••••"
                              value={formData.password}
                              onChange={handleChange}
                              className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4]"
                           />
                        </div>
                     </div>

                     <button
                        type="submit"
                        disabled={loading}
                        className="group flex w-full items-center justify-center gap-2 rounded-xl bg-[#006ce4] py-4 text-sm font-bold text-white shadow-xl shadow-blue-200 transition-all hover:bg-[#0057b7]"
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
               </div>
            </div>
         </div>
      </div>
   );
}
