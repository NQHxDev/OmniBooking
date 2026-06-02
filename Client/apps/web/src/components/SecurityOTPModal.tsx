"use client";

import { useState, useEffect, useRef } from "react";
import { X, ShieldCheck, Loader2, RefreshCcw } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { securityService } from "@/lib/api/services/securityService";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

interface SecurityOTPModalProps {
   isOpen: boolean;
   onClose: () => void;
   onSuccess: () => void;
}

export default function SecurityOTPModal({ isOpen, onClose, onSuccess }: SecurityOTPModalProps) {
   const [otp, setOtp] = useState<string[]>(new Array(6).fill(""));
   const [timer, setTimer] = useState(60);
   const [loading, setLoading] = useState(false);
   const [resending, setResending] = useState(false);
   const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

   const t = useTranslations("SecurityOTPModal");
   const tErrors = useTranslations("Errors");

   useEffect(() => {
      let interval: NodeJS.Timeout;
      if (isOpen && timer > 0) {
         interval = setInterval(() => {
            setTimer((prev) => prev - 1);
         }, 1000);
      }
      return () => clearInterval(interval);
   }, [isOpen, timer]);

   const handleChange = (target: HTMLInputElement, index: number) => {
      const value = target.value.toUpperCase();
      if (!value || /^[A-Z0-9]$/.test(value)) {
         const newOtp = [...otp];
         newOtp[index] = value;
         setOtp(newOtp);

         if (value && index < 5) {
            inputRefs.current[index + 1]?.focus();
         }
      }
   };

   const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>, index: number) => {
      if (e.key === "Backspace" && !otp[index] && index > 0) {
         inputRefs.current[index - 1]?.focus();
      }
   };

   const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
      e.preventDefault();
      const pastedData = e.clipboardData.getData("text").toUpperCase().trim();

      // Lấy 6 ký tự Alphanumeric đầu tiên
      const alphanumericData = pastedData.replace(/[^A-Z0-9]/g, "").substring(0, 6);

      if (alphanumericData) {
         const newOtp = [...otp];
         alphanumericData.split("").forEach((char, index) => {
            if (index < 6) newOtp[index] = char;
         });
         setOtp(newOtp);

         // Focus vào ô cuối cùng hoặc ô tiếp theo sau khi dán
         const nextIndex = Math.min(alphanumericData.length, 5);
         inputRefs.current[nextIndex]?.focus();
      }
   };

   const handleVerify = async () => {
      try {
         setLoading(true);
         const code = otp.join("");
         await securityService.verifyOTP(code);
         onSuccess();
         setOtp(new Array(6).fill(""));
      } catch (error: unknown) {
         const apiError = error as { errorCode?: string };
         const msg = apiError.errorCode ? tErrors(apiError.errorCode) : t("invalidOTP");
         toast.error(msg);
      } finally {
         setLoading(false);
      }
   };

   const handleResend = async () => {
      try {
         setResending(true);
         await securityService.requestOTP();
         setTimer(60);
         setOtp(new Array(6).fill(""));
         toast.success(t("successResend"));
      } catch (_error) {
         toast.error(t("errorResend"));
      } finally {
         setResending(false);
      }
   };

   return (
      <AnimatePresence>
         {isOpen && (
            <div className="fixed inset-0 z-100 flex items-center justify-center p-4 sm:p-6">
               {/* Overlay */}
               <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  onClick={onClose}
                  className="absolute inset-0 bg-zinc-900/60 backdrop-blur-sm"
               />

               {/* Modal Content */}
               <motion.div
                  initial={{ opacity: 0, scale: 0.95, y: 20 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.95, y: 20 }}
                  className="relative w-full max-w-[440px] overflow-hidden rounded-2xl bg-white shadow-2xl"
               >
                  {/* Top Bar Decoration */}
                  <div className="h-1.5 w-full bg-[#003580]" />

                  <button
                     onClick={onClose}
                     className="absolute right-4 top-5 p-2 text-zinc-400 hover:text-zinc-600 transition-colors"
                  >
                     <X className="h-5 w-5" />
                  </button>

                  <div className="px-8 pb-10 pt-12">
                     <div className="flex flex-col items-center text-center">
                        <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-blue-50">
                           <ShieldCheck className="h-8 w-8 text-[#003580]" />
                        </div>

                        <h2 className="text-2xl font-bold text-zinc-900 tracking-tight">
                           {t("title")}
                        </h2>
                        <p className="mt-3 text-[15px] leading-relaxed text-zinc-500">
                           {t("description")}
                        </p>

                        {/* OTP Inputs */}
                        <div className="mt-10 flex gap-2.5 sm:gap-3">
                           {otp.map((digit, idx) => (
                              <input
                                 key={idx}
                                 ref={(el) => {
                                    inputRefs.current[idx] = el;
                                 }}
                                 type="text"
                                 inputMode="text"
                                 maxLength={1}
                                 value={digit}
                                 onChange={(e) => handleChange(e.target, idx)}
                                 onKeyDown={(e) => handleKeyDown(e, idx)}
                                 onPaste={handlePaste}
                                 className="h-14 w-12 sm:h-16 sm:w-14 rounded-lg border-2 border-zinc-100 bg-white text-center text-2xl font-bold text-zinc-900 focus:border-[#003580] focus:ring-0 transition-all outline-none"
                              />
                           ))}
                        </div>

                        <div className="mt-10 w-full space-y-4">
                           <button
                              onClick={handleVerify}
                              disabled={loading || otp.some((d) => !d)}
                              className="relative flex w-full items-center justify-center rounded-lg bg-[#003580] py-4 text-[15px] font-bold text-white transition-all hover:bg-[#002b66] disabled:opacity-30 active:scale-[0.99]"
                           >
                              {loading ? (
                                 <Loader2 className="h-5 w-5 animate-spin" />
                              ) : (
                                 t("verifyButton")
                              )}
                           </button>

                           <div className="flex flex-col items-center gap-2">
                              {timer > 0 ? (
                                 <p className="text-sm text-zinc-400">
                                    {t("resendWait", { timer })}
                                 </p>
                              ) : (
                                 <button
                                    onClick={handleResend}
                                    disabled={resending}
                                    className="flex items-center gap-2 text-sm font-bold text-[#006ce4] hover:underline transition-all"
                                 >
                                    {resending && (
                                       <RefreshCcw className="h-3.5 w-3.5 animate-spin" />
                                    )}
                                    {t("resendLink")}
                                 </button>
                              )}
                           </div>
                        </div>
                     </div>
                  </div>

                  {/* Bottom Note */}
                  <div className="bg-zinc-50 px-8 py-5 text-center">
                     <p className="text-[12px] leading-relaxed text-zinc-400">{t("bottomNote")}</p>
                  </div>
               </motion.div>
            </div>
         )}
      </AnimatePresence>
   );
}
