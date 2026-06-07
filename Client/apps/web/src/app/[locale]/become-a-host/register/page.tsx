"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { Lock, ArrowLeft, ShieldCheck, FileText, CheckCircle2 } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { partnerService } from "@/lib/api/services/partnerService";
import PartnerNavbar from "@/components/PartnerNavbar";
import { useAuthStore } from "@/store/useAuthStore";
import { getPartnerUrl } from "@omnibooking/shared";
import { env } from "@/env";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

type Step = "VERIFY" | "TERMS" | "SUCCESS";

export default function PartnerRegisterPage() {
   const router = useRouter();
   const { isLoggedIn, setAuth } = useAuthStore();
   const t = useTranslations("BecomeAHost.register");
   const [mounted, setMounted] = useState(false);
   const [step, setStep] = useState<Step>("VERIFY");
   const [code, setCode] = useState("");
   const [isAgreed, setIsAgreed] = useState(false);
   const [countdown, setCountdown] = useState(5);
   const [resendCountdown, setResendCountdown] = useState(0);
   const [isResending, setIsResending] = useState(false);
   const hasRequestedOtp = useRef(false);

   const handleSendOtp = useCallback(
      async (isManual = false) => {
         if (!isManual && hasRequestedOtp.current) return;
         if (isManual) setIsResending(true);
         else hasRequestedOtp.current = true;

         setResendCountdown(30);

         try {
            await partnerService.sendOtp();
            toast.success(t("toasts.otpSent"), {
               description: t("toasts.otpSentDesc"),
               duration: 5000,
            });
         } catch (err) {
            console.error("Failed to send OTP", err);
            if (!isManual) hasRequestedOtp.current = false;
            toast.error(t("toasts.otpSendError"));
         } finally {
            if (isManual) setIsResending(false);
         }
      },
      [t]
   );

   useEffect(() => {
      if (mounted && !isLoggedIn) {
         router.push("/auth/login?callbackUrl=/become-a-host/register");
      }
   }, [mounted, isLoggedIn, router]);

   useEffect(() => {
      const timer = setTimeout(() => setMounted(true), 0);

      if (!hasRequestedOtp.current) {
         handleSendOtp(false);
      }

      return () => clearTimeout(timer);
   }, [handleSendOtp]);

   useEffect(() => {
      let timer: NodeJS.Timeout;
      if (step === "SUCCESS" && countdown > 0) {
         timer = setInterval(() => {
            setCountdown((prev) => prev - 1);
         }, 1000);
      } else if (step === "SUCCESS" && countdown === 0) {
         window.location.href = "/";
      }
      return () => clearInterval(timer);
   }, [step, countdown, router]);

   useEffect(() => {
      let timer: NodeJS.Timeout;
      if (resendCountdown > 0) {
         timer = setInterval(() => {
            setResendCountdown((prev) => prev - 1);
         }, 1000);
      }
      return () => clearInterval(timer);
   }, [resendCountdown]);

   const handleVerify = async () => {
      try {
         await partnerService.verifyOtp(code.trim());
         setStep("TERMS");
         toast.success(t("toasts.verifySuccess"));
      } catch {
         toast.error(t("toasts.verifyError"));
      }
   };

   const handleComplete = async () => {
      if (!isAgreed) {
         toast.error(t("toasts.agreeRequired"));
         return;
      }
      try {
         const userData = await partnerService.completeRegistration();
         setAuth(userData);
         setStep("SUCCESS");
         toast.success(t("toasts.registerSuccess"));
      } catch (err) {
         console.error("Failed to complete registration", err);
         toast.error(t("toasts.registerError"));
      }
   };

   if (!mounted) return null;

   return (
      <div className="min-h-screen bg-zinc-50 font-sans selection:bg-blue-100 selection:text-blue-900">
         <PartnerNavbar />

         <main className="mx-auto max-w-2xl px-4 py-12">
            {step !== "SUCCESS" && (
               <Link
                  href="/become-a-host"
                  className="inline-flex items-center gap-2 text-sm font-medium text-zinc-500 hover:text-[#006ce4] mb-8 transition-colors group"
               >
                  <ArrowLeft className="h-4 w-4 transition-transform group-hover:-translate-x-1" />
                  {t("back")}
               </Link>
            )}

            <div className="bg-white rounded-2xl shadow-sm border border-zinc-200 overflow-hidden">
               {/* Progress Bar */}
               {step !== "SUCCESS" && (
                  <div className="h-1.5 w-full bg-zinc-100 flex">
                     <div
                        className={`h-full bg-[#006ce4] transition-all duration-500 ${
                           step === "VERIFY" ? "w-1/2" : "w-full"
                        }`}
                     ></div>
                  </div>
               )}

               <div className="p-8 sm:p-12">
                  {step === "VERIFY" && (
                     <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
                        <div className="h-12 w-12 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center mb-6">
                           <Lock className="h-6 w-6" />
                        </div>
                        <h1 className="text-2xl font-bold text-zinc-900 mb-2">
                           {t("verifyTitle")}
                        </h1>
                        <p className="text-zinc-500 mb-8">{t("verifySub")}</p>

                        <div className="space-y-6">
                           <div>
                              <label className="block text-sm font-bold text-zinc-700 mb-2">
                                 {t("otpLabel")}
                              </label>
                              <input
                                 type="text"
                                 placeholder={t("otpPlaceholder")}
                                 value={code}
                                 onChange={(e) => setCode(e.target.value.toUpperCase())}
                                 className="w-full px-4 py-3 bg-zinc-50 border border-zinc-200 rounded-xl focus:ring-2 focus:ring-[#006ce4] focus:bg-white outline-none transition-all font-mono tracking-[0.5em] uppercase text-center text-xl pl-[0.5em]"
                              />
                           </div>

                           <button
                              onClick={handleVerify}
                              className="w-full bg-[#006ce4] text-white py-4 rounded-xl font-bold hover:bg-[#0057b7] transition-all shadow-lg shadow-blue-100 active:scale-[0.98]"
                           >
                              {t("verifyButton")}
                           </button>

                           <p className="text-center text-sm text-zinc-500">
                              {t("noCode")}{" "}
                              <button
                                 onClick={() => handleSendOtp(true)}
                                 disabled={isResending || resendCountdown > 0}
                                 className="text-[#006ce4] font-bold hover:underline cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                              >
                                 {isResending
                                    ? t("sending")
                                    : resendCountdown > 0
                                      ? t("resendCountdown", { count: resendCountdown })
                                      : t("resend")}
                              </button>
                           </p>
                        </div>
                     </div>
                  )}

                  {step === "TERMS" && (
                     <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
                        <div className="h-12 w-12 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center mb-6">
                           <FileText className="h-6 w-6" />
                        </div>
                        <h1 className="text-2xl font-bold text-zinc-900 mb-2">{t("termsTitle")}</h1>
                        <p className="text-zinc-500 mb-8">{t("termsSub")}</p>

                        <div className="bg-zinc-50 border border-zinc-200 rounded-xl p-6 h-64 overflow-y-auto mb-8 text-sm text-zinc-600 leading-relaxed scrollbar-thin">
                           <div className="space-y-6">
                              <section>
                                 <h3 className="font-bold text-zinc-900 mb-2">{t("term1Title")}</h3>
                                 <p>{t("term1Content")}</p>
                              </section>
                              <section>
                                 <h3 className="font-bold text-zinc-900 mb-2">{t("term2Title")}</h3>
                                 <p>{t("term2Content")}</p>
                              </section>
                              <section>
                                 <h3 className="font-bold text-zinc-900 mb-2">{t("term3Title")}</h3>
                                 <p>{t("term3Content")}</p>
                              </section>
                              <section>
                                 <h3 className="font-bold text-zinc-900 mb-2">{t("term4Title")}</h3>
                                 <p>{t("term4Content")}</p>
                              </section>
                           </div>
                        </div>

                        <div className="space-y-6">
                           <label className="flex items-start gap-4 p-4 rounded-xl border border-zinc-200 hover:border-[#006ce4] hover:bg-blue-50/50 transition-all cursor-pointer group">
                              <div className="relative flex items-center h-5 mt-0.5">
                                 <input
                                    type="checkbox"
                                    checked={isAgreed}
                                    onChange={(e) => setIsAgreed(e.target.checked)}
                                    className="h-5 w-5 rounded border-zinc-300 text-[#006ce4] focus:ring-[#006ce4] cursor-pointer"
                                 />
                              </div>
                              <div className="flex-1">
                                 <p className="text-sm font-bold text-zinc-900">{t("agreeAll")}</p>
                                 <p className="text-xs text-zinc-500 mt-1">{t("agreeSub")}</p>
                              </div>
                           </label>

                           <button
                              disabled={!isAgreed}
                              onClick={handleComplete}
                              className="w-full bg-[#006ce4] text-white py-4 rounded-xl font-bold hover:bg-[#0057b7] transition-all disabled:opacity-50 disabled:hover:bg-[#006ce4] shadow-lg shadow-blue-100"
                           >
                              {t("completeButton")}
                           </button>
                        </div>
                     </div>
                  )}

                  {step === "SUCCESS" && (
                     <div className="text-center py-4 animate-in zoom-in-95 duration-700">
                        <div className="relative mx-auto h-24 w-24 mb-8">
                           <div className="absolute inset-0 bg-green-100 rounded-full animate-ping opacity-25"></div>
                           <div className="relative h-24 w-24 bg-green-500 text-white rounded-full flex items-center justify-center shadow-lg shadow-green-100">
                              <CheckCircle2 className="h-12 w-12" />
                           </div>
                        </div>
                        <h1 className="text-3xl font-bold text-zinc-900 mb-4">
                           {t("successTitle")}
                        </h1>
                        <p className="text-zinc-500 max-w-md mx-auto mb-6 leading-relaxed">
                           {t("successSub")}
                        </p>

                        <div className="bg-blue-50 text-[#006ce4] py-3 px-6 rounded-full inline-flex items-center gap-2 text-sm font-bold mb-10 animate-pulse">
                           <span className="relative flex h-2 w-2">
                              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                              <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
                           </span>
                           {t("redirectMessage", { count: countdown })}
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                           <Link
                              href="/"
                              className="px-6 py-4 bg-zinc-900 text-white rounded-xl font-bold hover:bg-black transition-all shadow-lg text-center"
                           >
                              {t("goHome")}
                           </Link>
                           <a
                              href={`${getPartnerUrl(window.location.origin)}/dashboard`}
                              className="px-6 py-4 bg-white border border-zinc-200 text-zinc-900 rounded-xl font-bold hover:bg-zinc-50 transition-all text-center"
                           >
                              {t("viewDashboard")}
                           </a>
                        </div>
                     </div>
                  )}
               </div>
            </div>

            {/* Footer Trust badges */}
            {step !== "SUCCESS" && (
               <div className="mt-12 flex flex-wrap items-center justify-center gap-8 opacity-50 grayscale">
                  <div className="flex items-center gap-2">
                     <ShieldCheck className="h-5 w-5" />
                     <span className="text-xs font-bold uppercase tracking-widest">
                        {t("secureSsl")}
                     </span>
                  </div>
                  <div className="flex items-center gap-2">
                     <Lock className="h-5 w-5" />
                     <span className="text-xs font-bold uppercase tracking-widest">
                        {t("privateData")}
                     </span>
                  </div>
                  <div className="flex items-center gap-2">
                     <FileText className="h-5 w-5" />
                     <span className="text-xs font-bold uppercase tracking-widest">
                        {t("verifiedPartner")}
                     </span>
                  </div>
               </div>
            )}
         </main>
      </div>
   );
}
