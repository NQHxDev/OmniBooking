"use client";

import { useState, useRef } from "react";
import {
   X,
   ShieldCheck,
   Loader2,
   Smartphone,
   Download,
   QrCode,
   Key,
   Copy,
   Check,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { QRCodeSVG } from "qrcode.react";
import { securityService } from "@/lib/api/services/securityService";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

interface TwoFactorSetupModalProps {
   isOpen: boolean;
   onClose: () => void;
   onSuccess: (backupCodes: string[]) => void;
}

export default function TwoFactorSetupModal({
   isOpen,
   onClose,
   onSuccess,
}: TwoFactorSetupModalProps) {
   const [step, setStep] = useState(1);
   const [loading, setLoading] = useState(false);
   const [secretKey, setSecretKey] = useState("");
   const [qrCodeUri, setQrCodeUri] = useState("");
   const [otp, setOtp] = useState<string[]>(new Array(6).fill(""));
   const [backupCodes, setBackupCodes] = useState<string[]>([]);
   const [isCopied, setIsCopied] = useState(false);
   const [hasSavedBackups, setHasSavedBackups] = useState(false);

   const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

   const t = useTranslations("Profile.Security.twoFactor");
   const tErrors = useTranslations("Errors");

   const handleStartSetup = async () => {
      try {
         setLoading(true);
         const data = await securityService.setup2FA();
         setSecretKey(data.secretKey);
         setQrCodeUri(data.qrCodeUri);
         setStep(2);
      } catch (err: unknown) {
         const apiError = err as { errorCode?: string };
         const msg = apiError.errorCode ? tErrors(apiError.errorCode) : t("modal.errorOccurred");
         toast.error(msg);
      } finally {
         setLoading(false);
      }
   };

   const handleCopyKey = () => {
      navigator.clipboard.writeText(secretKey);
      setIsCopied(true);
      toast.success(t("modal.copySuccess"));
      setTimeout(() => setIsCopied(false), 2000);
   };

   const handleCopyBackupCodes = () => {
      navigator.clipboard.writeText(backupCodes.join("\n"));
      toast.success(t("modal.copySuccess"));
   };

   const handleOtpChange = (target: HTMLInputElement, index: number) => {
      const value = target.value.replace(/[^0-9]/g, "");
      if (value.length <= 1) {
         const newOtp = [...otp];
         newOtp[index] = value;
         setOtp(newOtp);

         if (value && index < 5) {
            inputRefs.current[index + 1]?.focus();
         }
      }
   };

   const handleOtpKeyDown = (e: React.KeyboardEvent<HTMLInputElement>, index: number) => {
      if (e.key === "Backspace" && !otp[index] && index > 0) {
         inputRefs.current[index - 1]?.focus();
      }
   };

   const handleOtpPaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
      e.preventDefault();
      const pastedData = e.clipboardData
         .getData("text")
         .replace(/[^0-9]/g, "")
         .substring(0, 6);
      if (pastedData) {
         const newOtp = [...otp];
         pastedData.split("").forEach((char, index) => {
            if (index < 6) newOtp[index] = char;
         });
         setOtp(newOtp);

         const nextIndex = Math.min(pastedData.length, 5);
         inputRefs.current[nextIndex]?.focus();
      }
   };

   const handleVerifyAndActivate = async () => {
      try {
         setLoading(true);
         const code = otp.join("");
         const codes = await securityService.enable2FA(code);
         setBackupCodes(codes);
         setStep(4);
      } catch (error: unknown) {
         const apiError = error as { errorCode?: string };
         const msg = apiError.errorCode ? tErrors(apiError.errorCode) : t("modal.invalidCode");
         toast.error(msg);
      } finally {
         setLoading(false);
      }
   };

   const handleFinish = () => {
      onSuccess(backupCodes);
      setStep(1);
      setOtp(new Array(6).fill(""));
      setBackupCodes([]);
      setHasSavedBackups(false);
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
                  onClick={step === 4 ? undefined : onClose}
                  className="absolute inset-0 bg-zinc-900/60 backdrop-blur-sm"
               />

               {/* Modal Content */}
               <motion.div
                  initial={{ opacity: 0, scale: 0.95, y: 20 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.95, y: 20 }}
                  className="relative w-full max-w-[480px] overflow-hidden rounded-2xl bg-white shadow-2xl"
               >
                  {/* Top Decoration */}
                  <div className="h-1.5 w-full bg-[#003580]" />

                  {step !== 4 && (
                     <button
                        onClick={onClose}
                        className="absolute right-4 top-5 p-2 text-zinc-400 hover:text-zinc-600 transition-colors"
                     >
                        <X className="h-5 w-5" />
                     </button>
                  )}

                  <div className="px-6 sm:px-8 pb-8 pt-10">
                     <div className="flex flex-col items-center">
                        <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-blue-50">
                           <ShieldCheck className="h-7 w-7 text-[#003580]" />
                        </div>

                        <h2 className="text-xl font-bold text-zinc-900 tracking-tight text-center">
                           {t("modal.title")}
                        </h2>

                        {/* STEP 1: Download Authenticator App */}
                        {step === 1 && (
                           <div className="mt-6 w-full space-y-4">
                              <div className="rounded-lg bg-zinc-50 p-4 border border-zinc-150">
                                 <h3 className="text-sm font-bold text-zinc-800 flex items-center gap-2">
                                    <Download className="h-4 w-4 text-blue-600" />
                                    {t("modal.step1Title")}
                                 </h3>
                                 <p className="mt-2 text-xs text-zinc-500 leading-relaxed">
                                    {t("modal.step1Desc")}
                                 </p>
                              </div>

                              <div className="grid grid-cols-3 gap-2.5">
                                 <div className="flex flex-col items-center justify-center p-3 rounded-lg border border-zinc-100 bg-white shadow-sm text-center">
                                    <div className="text-[10px] font-bold text-zinc-700">
                                       Google
                                    </div>
                                    <div className="text-[9px] text-zinc-400 mt-1">
                                       Authenticator
                                    </div>
                                 </div>
                                 <div className="flex flex-col items-center justify-center p-3 rounded-lg border border-zinc-100 bg-white shadow-sm text-center">
                                    <div className="text-[10px] font-bold text-zinc-700">
                                       Microsoft
                                    </div>
                                    <div className="text-[9px] text-zinc-400 mt-1">
                                       Authenticator
                                    </div>
                                 </div>
                                 <div className="flex flex-col items-center justify-center p-3 rounded-lg border border-zinc-100 bg-white shadow-sm text-center">
                                    <div className="text-[10px] font-bold text-zinc-700">Authy</div>
                                    <div className="text-[9px] text-zinc-400 mt-1">by Twilio</div>
                                 </div>
                              </div>

                              <button
                                 onClick={handleStartSetup}
                                 disabled={loading}
                                 className="mt-4 flex w-full items-center justify-center rounded-lg bg-[#003580] py-3 text-sm font-bold text-white transition-all hover:bg-[#002b66] disabled:opacity-50"
                              >
                                 {loading ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                 ) : (
                                    t("modal.next")
                                 )}
                              </button>
                           </div>
                        )}

                        {/* STEP 2: Scan QR or copy Secret Key */}
                        {step === 2 && (
                           <div className="mt-6 w-full space-y-4">
                              <div className="rounded-lg bg-zinc-50 p-4 border border-zinc-150">
                                 <h3 className="text-sm font-bold text-zinc-800 flex items-center gap-2">
                                    <QrCode className="h-4 w-4 text-blue-600" />
                                    {t("modal.step2Title")}
                                 </h3>
                                 <p className="mt-2 text-xs text-zinc-500 leading-relaxed">
                                    {t("modal.step2Desc")}
                                 </p>
                              </div>

                              <div className="flex justify-center p-4 rounded-xl border border-zinc-200 bg-white shadow-inner">
                                 {qrCodeUri && (
                                    <QRCodeSVG
                                       value={qrCodeUri}
                                       size={180}
                                       level="M"
                                       includeMargin={false}
                                    />
                                 )}
                              </div>

                              <div className="space-y-1.5">
                                 <label className="text-[11px] font-bold text-zinc-500 tracking-wider uppercase">
                                    {t("modal.step2Secret")}
                                 </label>
                                 <div className="flex items-center gap-2 rounded-lg bg-zinc-50 border border-zinc-200 p-2.5">
                                    <Key className="h-4 w-4 text-zinc-400 shrink-0" />
                                    <code className="text-xs font-mono font-bold text-zinc-700 break-all select-all flex-1 tracking-wider">
                                       {secretKey}
                                    </code>
                                    <button
                                       onClick={handleCopyKey}
                                       className="p-1.5 hover:bg-zinc-200 rounded-md transition-colors shrink-0 text-zinc-500 hover:text-zinc-800"
                                    >
                                       {isCopied ? (
                                          <Check className="h-3.5 w-3.5 text-green-600" />
                                       ) : (
                                          <Copy className="h-3.5 w-3.5" />
                                       )}
                                    </button>
                                 </div>
                              </div>

                              <div className="flex gap-3 pt-2">
                                 <button
                                    onClick={() => setStep(1)}
                                    className="flex-1 py-3 text-xs font-bold text-zinc-500 border border-zinc-200 rounded-lg hover:bg-zinc-50 transition-colors"
                                 >
                                    {t("modal.back")}
                                 </button>
                                 <button
                                    onClick={() => setStep(3)}
                                    className="flex-1 py-3 text-xs font-bold text-white bg-[#003580] rounded-lg hover:bg-[#002b66] transition-colors"
                                 >
                                    {t("modal.next")}
                                 </button>
                              </div>
                           </div>
                        )}

                        {/* STEP 3: Verification */}
                        {step === 3 && (
                           <div className="mt-6 w-full space-y-4">
                              <div className="rounded-lg bg-zinc-50 p-4 border border-zinc-150 text-center">
                                 <h3 className="text-sm font-bold text-zinc-800 flex items-center justify-center gap-2">
                                    <Smartphone className="h-4 w-4 text-blue-600" />
                                    {t("modal.step3Title")}
                                 </h3>
                                 <p className="mt-2 text-xs text-zinc-500 leading-relaxed">
                                    {t("modal.step3Desc")}
                                 </p>
                              </div>

                              <div className="flex justify-center gap-2">
                                 {otp.map((digit, idx) => (
                                    <input
                                       key={idx}
                                       ref={(el) => {
                                          inputRefs.current[idx] = el;
                                       }}
                                       type="text"
                                       inputMode="numeric"
                                       maxLength={1}
                                       value={digit}
                                       onChange={(e) => handleOtpChange(e.target, idx)}
                                       onKeyDown={(e) => handleOtpKeyDown(e, idx)}
                                       onPaste={handleOtpPaste}
                                       className="h-14 w-11 sm:w-12 rounded-lg border-2 border-zinc-200 bg-white text-center text-xl font-bold text-zinc-850 focus:border-[#003580] focus:ring-0 transition-all outline-none"
                                    />
                                 ))}
                              </div>

                              <div className="flex gap-3 pt-4">
                                 <button
                                    onClick={() => setStep(2)}
                                    className="flex-1 py-3 text-xs font-bold text-zinc-500 border border-zinc-200 rounded-lg hover:bg-zinc-50 transition-colors"
                                 >
                                    {t("modal.back")}
                                 </button>
                                 <button
                                    onClick={handleVerifyAndActivate}
                                    disabled={loading || otp.some((d) => !d)}
                                    className="flex-1 py-3 text-xs font-bold text-white bg-[#003580] rounded-lg hover:bg-[#002b66] disabled:opacity-40 transition-colors flex items-center justify-center gap-2"
                                 >
                                    {loading && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                                    {t("modal.step3Activate")}
                                 </button>
                              </div>
                           </div>
                        )}

                        {/* STEP 4: Backup Codes */}
                        {step === 4 && (
                           <div className="mt-6 w-full space-y-4">
                              <div className="rounded-lg bg-zinc-50 p-4 border border-zinc-150">
                                 <h3 className="text-sm font-bold text-zinc-800 flex items-center gap-2">
                                    <ShieldCheck className="h-4 w-4 text-green-600" />
                                    {t("modal.step4Title")}
                                 </h3>
                                 <p className="mt-2 text-xs text-zinc-500 leading-relaxed">
                                    {t("modal.step4Desc")}
                                 </p>
                              </div>

                              <div className="rounded-xl border border-zinc-200 p-4 bg-zinc-50 shadow-inner">
                                 <div className="flex items-center justify-between mb-3 border-b border-zinc-200 pb-2">
                                    <span className="text-xs font-bold text-zinc-600">
                                       {t("modal.backupCodesHeading")}
                                    </span>
                                    <button
                                       onClick={handleCopyBackupCodes}
                                       className="text-[11px] font-bold text-[#006ce4] hover:underline flex items-center gap-1 transition-all"
                                    >
                                       <Copy className="h-3 w-3" />
                                       {t("modal.copySuccess")}
                                    </button>
                                 </div>
                                 <div className="grid grid-cols-2 gap-2">
                                    {backupCodes.map((code, index) => (
                                       <code
                                          key={index}
                                          className="text-xs font-mono font-bold text-zinc-700 bg-white px-2 py-1.5 rounded border border-zinc-150 text-center select-all"
                                       >
                                          {code}
                                       </code>
                                    ))}
                                 </div>
                              </div>

                              <div className="flex items-start gap-2.5 p-1">
                                 <input
                                    id="confirm-saved"
                                    type="checkbox"
                                    checked={hasSavedBackups}
                                    onChange={(e) => setHasSavedBackups(e.target.checked)}
                                    className="mt-1 h-4 w-4 rounded border-zinc-300 text-[#003580] focus:ring-[#003580]"
                                 />
                                 <label
                                    htmlFor="confirm-saved"
                                    className="text-xs text-zinc-650 leading-relaxed select-none cursor-pointer"
                                 >
                                    {t("modal.step4ConfirmCheckbox")}
                                 </label>
                              </div>

                              <button
                                 onClick={handleFinish}
                                 disabled={!hasSavedBackups}
                                 className="w-full py-3 text-sm font-bold text-white bg-[#003580] hover:bg-[#002b66] disabled:opacity-40 rounded-lg transition-colors flex items-center justify-center"
                              >
                                 {t("modal.step4Finish")}
                              </button>
                           </div>
                        )}
                     </div>
                  </div>
               </motion.div>
            </div>
         )}
      </AnimatePresence>
   );
}
