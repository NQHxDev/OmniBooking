"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Fingerprint, Smartphone, LogOut, Trash2, Loader2, Lock, Check } from "lucide-react";
import { passkeyService, Passkey } from "@/services/passkeyService";
import { motion, AnimatePresence } from "framer-motion";
import { startRegistration } from "@simplewebauthn/browser";
import { toast } from "sonner";
import { useLocale } from "next-intl";
import { securityService } from "@/lib/api/services/securityService";
import SecurityOTPModal from "@/components/SecurityOTPModal";
import TwoFactorSetupModal from "@/components/TwoFactorSetupModal";
import { profileService } from "@/lib/api/services/profileService";
import PasswordModal from "@/components/PasswordModal";

export default function SecurityPage() {
   const [isRegistering, setIsRegistering] = useState(false);
   const [hasPasskey, setHasPasskey] = useState(false);
   const [isLoadingStatus, setIsLoadingStatus] = useState(true);
   const [passkeys, setPasskeys] = useState<Passkey[]>([]);
   const [showPasskeyManager, setShowPasskeyManager] = useState(false);

   // Trạng thái 2FA
   const [twoFactorStatus, setTwoFactorStatus] = useState<"UNSET" | "DISABLED" | "ENABLED">(
      "UNSET"
   );
   const [isLoading2FA, setIsLoading2FA] = useState(true);
   const [is2FASetupOpen, setIs2FASetupOpen] = useState(false);
   const [isDisable2FAOpen, setIsDisable2FAOpen] = useState(false);
   const [disableCode, setDisableCode] = useState("");
   const [isDisabling, setIsDisabling] = useState(false);
   const [twoFactorAction, setTwoFactorAction] = useState<"DISABLE" | "REMOVE" | "REACTIVATE">(
      "DISABLE"
   );

   const is2FAEnabled = twoFactorStatus === "ENABLED";

   // Trạng thái cho Security Step-up
   const [isOtpModalOpen, setIsOtpModalOpen] = useState(false);

   // Trạng thái Mật khẩu
   const [hasPassword, setHasPassword] = useState<boolean | null>(null);
   const [isLoadingPasswordStatus, setIsLoadingPasswordStatus] = useState(true);
   const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
   const [pendingAction, setPendingAction] = useState<{
      type: "register" | "delete";
      data?: string;
   } | null>(null);

   const t = useTranslations("Profile.Security");
   const tDetails = useTranslations("Profile.details");
   const tErrors = useTranslations("Errors");
   const locale = useLocale();

   const fetchPasskeyData = async () => {
      try {
         setIsLoadingStatus(true);
         const status = await passkeyService.checkStatus();
         setHasPasskey(status);

         if (status) {
            const passkeyData = await passkeyService.listPasskeys();
            setPasskeys(passkeyData);
         } else {
            setPasskeys([]);
            setShowPasskeyManager(false);
         }
      } catch (err) {
         console.error("Failed to fetch passkey data:", err);
      } finally {
         setIsLoadingStatus(false);
      }
   };

   const fetch2FAStatus = async () => {
      try {
         setIsLoading2FA(true);
         const status = await securityService.get2FAStatus();
         setTwoFactorStatus(status);
      } catch (err) {
         console.error("Failed to fetch 2FA status:", err);
      } finally {
         setIsLoading2FA(false);
      }
   };

   const fetchPasswordStatus = async () => {
      try {
         setIsLoadingPasswordStatus(true);
         const profile = await profileService.getMyProfile();
         setHasPassword(profile.hasPassword);
      } catch (err) {
         console.error("Failed to fetch password status:", err);
      } finally {
         setIsLoadingPasswordStatus(false);
      }
   };

   useEffect(() => {
      const timer = setTimeout(() => {
         fetchPasskeyData();
         fetch2FAStatus();
         fetchPasswordStatus();
      }, 0);
      return () => clearTimeout(timer);
   }, []);

   const getDeviceLabel = () => {
      if (typeof window === "undefined") return "Device";
      const ua = window.navigator.userAgent;
      let browser = "Browser";
      if (ua.includes("Chrome")) browser = "Chrome";
      else if (ua.includes("Safari") && !ua.includes("Chrome")) browser = "Safari";
      else if (ua.includes("Firefox")) browser = "Firefox";
      else if (ua.includes("Edg")) browser = "Edge";

      let os = "Device";
      if (ua.includes("Windows")) os = "Windows";
      else if (ua.includes("Mac OS")) os = "Mac OS X";
      else if (ua.includes("Android")) os = "Android";
      else if (ua.includes("iPhone") || ua.includes("iPad")) os = "iOS";
      else if (ua.includes("Linux")) os = "Linux";

      return `${browser} ${os}`;
   };

   const handleSecurityStepUp = async (action: "register" | "delete", data?: string) => {
      try {
         await securityService.requestOTP();
         setPendingAction({ type: action, data });
         setIsOtpModalOpen(true);
      } catch {
         toast.error("Không thể khởi tạo xác thực bảo mật. Vui lòng thử lại.");
      }
   };

   const handleRegisterPasskey = async () => {
      try {
         setIsRegistering(true);
         const options = await passkeyService.getRegistrationOptions();

         const regResp = await startRegistration({
            optionsJSON: {
               challenge: options.challenge,
               rp: { name: options.rpName, id: options.rpId },
               user: {
                  id: options.userId,
                  name: options.username,
                  displayName: options.userDisplayName,
               },
               pubKeyCredParams: [
                  { alg: -7, type: "public-key" },
                  { alg: -257, type: "public-key" },
               ],
               authenticatorSelection: {
                  userVerification: "preferred",
                  residentKey: "preferred",
               },
               attestation: "none",
            },
         });

         await passkeyService.verifyRegistration({
            ...regResp,
            label: getDeviceLabel(),
         });

         toast.success(t("messages.setupSuccess"));
         fetchPasskeyData();
      } catch (err: unknown) {
         const apiError = err as { errorCode?: string; name?: string };
         if (apiError.errorCode === "AUTH_009") {
            handleSecurityStepUp("register");
            return;
         }

         if (apiError.name === "NotAllowedError") return;

         const errorMessage = apiError.errorCode
            ? tErrors(apiError.errorCode)
            : t("passkey.setupError");
         toast.error(errorMessage);
      } finally {
         setIsRegistering(false);
      }
   };

   const handleDeletePasskey = async (id: string) => {
      try {
         await passkeyService.deletePasskey(id);

         const updatedPasskeys = passkeys.filter((pk) => pk.id !== id);
         setPasskeys(updatedPasskeys);

         if (updatedPasskeys.length === 0) {
            setHasPasskey(false);
            setShowPasskeyManager(false);
         }

         toast.success(t("manager.deleteSuccess"));
         fetchPasskeyData();
      } catch (err: unknown) {
         const apiError = err as { errorCode?: string };
         if (apiError.errorCode === "AUTH_009") {
            handleSecurityStepUp("delete", id);
            return;
         }

         const errorMessage = apiError.errorCode
            ? tErrors(apiError.errorCode)
            : t("passkey.deleteError");
         toast.error(errorMessage);
      }
   };

   const handleOtpSuccess = () => {
      setIsOtpModalOpen(false);
      if (pendingAction?.type === "register") {
         handleRegisterPasskey();
      } else if (pendingAction?.type === "delete" && pendingAction.data) {
         handleDeletePasskey(pendingAction.data);
      }
      setPendingAction(null);
   };

   return (
      <div className="animate-in fade-in slide-in-from-bottom-2 duration-500">
         <div className="mb-8">
            <h1 className="text-3xl font-bold text-zinc-900 tracking-tight">{t("title")}</h1>
            <p className="text-[15px] text-zinc-500 mt-2">{t("subtitle")}</p>
         </div>

         <div className="space-y-4">
            {/* Passkey Card */}
            <SecurityCard
               icon={<Fingerprint className="h-6 w-6 text-blue-600" />}
               title={t("passkey.title")}
               description={t("passkey.desc")}
               action={
                  isLoadingStatus ? (
                     <Loader2 className="h-5 w-5 animate-spin text-zinc-300" />
                  ) : hasPasskey ? (
                     <button
                        onClick={() => setShowPasskeyManager(!showPasskeyManager)}
                        className="text-sm font-bold text-[#006ce4] hover:bg-blue-50 px-4 py-2 rounded-md transition-all cursor-pointer"
                     >
                        {showPasskeyManager ? tDetails("cancel") : t("passkey.manage")}
                     </button>
                  ) : (
                     <button
                        onClick={handleRegisterPasskey}
                        disabled={isRegistering}
                        className="text-sm font-bold text-[#006ce4] hover:bg-blue-50 px-4 py-2 rounded-md transition-all flex items-center gap-2 cursor-pointer"
                     >
                        {isRegistering && <Loader2 className="h-4 w-4 animate-spin" />}
                        {t("passkey.setup")}
                     </button>
                  )
               }
            >
               <AnimatePresence>
                  {showPasskeyManager && (
                     <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: "auto", opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        className="mt-6 border-t border-zinc-100 pt-6"
                     >
                        <div className="flex items-center justify-between mb-4">
                           <h4 className="text-sm font-bold text-zinc-900">
                              {t("manager.registeredDevices")}
                           </h4>
                        </div>
                        <div className="space-y-3">
                           {passkeys.map((pk) => (
                              <div
                                 key={pk.id}
                                 className="flex items-center justify-between p-4 rounded-lg bg-zinc-50 border border-zinc-200"
                              >
                                 <div className="flex items-center gap-4">
                                    <div className="h-10 w-10 rounded-full bg-white flex items-center justify-center border border-zinc-100">
                                       <Smartphone className="h-5 w-5 text-zinc-400" />
                                    </div>
                                    <div>
                                       <p className="text-sm font-bold text-zinc-900">
                                          {pk.label || t("manager.unknownDevice")}
                                       </p>
                                       <p className="text-[11px] text-zinc-400 mt-1">
                                          {t("manager.lastUsed")}:{" "}
                                          {pk.lastUsedAt
                                             ? new Date(pk.lastUsedAt).toLocaleString(
                                                  locale === "vi" ? "vi-VN" : "en-US"
                                               )
                                             : t("manager.neverUsed")}
                                       </p>
                                    </div>
                                 </div>
                                 <button
                                    onClick={() => handleDeletePasskey(pk.id)}
                                    className="p-2 text-zinc-400 hover:text-red-500 hover:bg-red-50 rounded-md transition-colors"
                                 >
                                    <Trash2 className="h-4 w-4" />
                                 </button>
                              </div>
                           ))}
                           <button
                              onClick={handleRegisterPasskey}
                              disabled={isRegistering}
                              className="w-full py-3 mt-2 border-2 border-dashed border-zinc-200 rounded-lg text-xs font-bold text-zinc-500 hover:border-[#006ce4] hover:text-[#006ce4] transition-all flex items-center justify-center gap-2 cursor-pointer"
                           >
                              {isRegistering ? (
                                 <Loader2 className="h-3.5 w-3.5 animate-spin" />
                              ) : (
                                 "+"
                              )}
                              {t("manager.addNewDevice")}
                           </button>
                        </div>
                     </motion.div>
                  )}
               </AnimatePresence>
            </SecurityCard>

            {/* Two-Factor Authentication Card */}
            <SecurityCard
               icon={
                  <Smartphone
                     className={`h-6 w-6 ${is2FAEnabled ? "text-green-600" : "text-zinc-400"}`}
                  />
               }
               title={
                  <div className="flex items-center gap-2">
                     <span>{t("twoFactor.title")}</span>
                     {twoFactorStatus === "ENABLED" && (
                        <Check className="h-4.5 w-4.5 text-green-600 stroke-[3px] shrink-0" />
                     )}
                     {twoFactorStatus === "DISABLED" && (
                        <span className="text-[10px] font-bold text-orange-650 bg-orange-50 px-1.5 py-0.5 rounded border border-orange-150 shrink-0 animate-in fade-in duration-300">
                           {t("twoFactor.temporarilyDisabled")}
                        </span>
                     )}
                  </div>
               }
               description={t("twoFactor.desc")}
               action={
                  isLoading2FA ? (
                     <Loader2 className="h-5 w-5 animate-spin text-zinc-300" />
                  ) : twoFactorStatus === "ENABLED" ? (
                     <div className="flex items-center gap-2.5">
                        <button
                           onClick={() => {
                              setTwoFactorAction("DISABLE");
                              setIsDisable2FAOpen(true);
                           }}
                           className="text-sm font-bold text-zinc-655 hover:bg-zinc-50 px-4 py-2 rounded-md transition-all cursor-pointer border border-zinc-200"
                        >
                           {t("twoFactor.disable")}
                        </button>
                        <button
                           onClick={() => {
                              setTwoFactorAction("REMOVE");
                              setIsDisable2FAOpen(true);
                           }}
                           className="p-2 text-red-600 hover:bg-red-50 rounded-md transition-all cursor-pointer border border-red-100"
                           title={t("twoFactor.remove")}
                        >
                           <Trash2 className="h-4.5 w-4.5" />
                        </button>
                     </div>
                  ) : twoFactorStatus === "DISABLED" ? (
                     <div className="flex items-center gap-2.5">
                        <button
                           onClick={() => {
                              setTwoFactorAction("REACTIVATE");
                              setIsDisable2FAOpen(true);
                           }}
                           className="text-sm font-bold text-[#006ce4] hover:bg-blue-50 px-4 py-2 rounded-md transition-all cursor-pointer"
                        >
                           {t("twoFactor.reactivate")}
                        </button>
                        <button
                           onClick={() => {
                              setTwoFactorAction("REMOVE");
                              setIsDisable2FAOpen(true);
                           }}
                           className="p-2 text-red-650 hover:bg-red-50 rounded-md transition-all cursor-pointer border border-red-100"
                           title={t("twoFactor.remove")}
                        >
                           <Trash2 className="h-4.5 w-4.5" />
                        </button>
                     </div>
                  ) : (
                     <button
                        onClick={() => setIs2FASetupOpen(true)}
                        className="text-sm font-bold text-[#006ce4] hover:bg-blue-50 px-4 py-2 rounded-md transition-all cursor-pointer"
                     >
                        {t("twoFactor.setup")}
                     </button>
                  )
               }
            />

            {/* Password Card */}
            <SecurityCard
               icon={<Lock className="h-6 w-6 text-zinc-400" />}
               title={t("password.title")}
               description={hasPassword === false ? t("password.descCreate") : t("password.desc")}
               action={
                  isLoadingPasswordStatus ? (
                     <div className="flex h-9 w-20 items-center justify-center">
                        <Loader2 className="h-4 w-4 animate-spin text-[#006ce4]" />
                     </div>
                  ) : (
                     <button
                        onClick={() => setIsPasswordModalOpen(true)}
                        className="text-sm font-bold text-[#006ce4] hover:bg-blue-50 px-4 py-2 rounded-md transition-all cursor-pointer"
                     >
                        {hasPassword === false ? t("password.actionCreate") : t("password.action")}
                     </button>
                  )
               }
            />

            {/* Active Sessions Card */}
            <SecurityCard
               icon={<LogOut className="h-6 w-6 text-orange-500" />}
               title={t("sessions.title")}
               description={t("sessions.desc")}
               action={
                  <button className="text-sm font-bold text-[#006ce4] hover:bg-blue-50 px-4 py-2 rounded-md transition-all cursor-pointer">
                     {t("sessions.logout")}
                  </button>
               }
            />

            {/* Delete Account Card */}
            <SecurityCard
               icon={<Trash2 className="h-6 w-6 text-red-500" />}
               title={t("deleteAccount.title")}
               description={t("deleteAccount.desc")}
               action={
                  <button className="text-sm font-bold text-red-600 hover:bg-red-50 px-4 py-2 rounded-md transition-all cursor-pointer">
                     {t("deleteAccount.action")}
                  </button>
               }
            />
         </div>

         {/* Security OTP Verification modal */}
         <SecurityOTPModal
            isOpen={isOtpModalOpen}
            onClose={() => {
               setIsOtpModalOpen(false);
               setPendingAction(null);
            }}
            onSuccess={handleOtpSuccess}
         />

         {/* 2FA Setup multi-step modal */}
         <TwoFactorSetupModal
            isOpen={is2FASetupOpen}
            onClose={() => setIs2FASetupOpen(false)}
            onSuccess={() => {
               setIs2FASetupOpen(false);
               fetch2FAStatus();
            }}
         />

         {/* Password Setup & Change modal */}
         <PasswordModal
            isOpen={isPasswordModalOpen}
            onClose={() => setIsPasswordModalOpen(false)}
            hasPassword={!!hasPassword}
            onSuccess={fetchPasswordStatus}
         />

         {/* Disable 2FA Confirmation modal */}
         <AnimatePresence>
            {/* Modal Thao tác 2FA (Disable, Remove, Reactivate) */}
            {isDisable2FAOpen && (
               <div className="fixed inset-0 z-100 flex items-center justify-center p-4">
                  <motion.div
                     initial={{ opacity: 0 }}
                     animate={{ opacity: 1 }}
                     exit={{ opacity: 0 }}
                     onClick={() => {
                        setIsDisable2FAOpen(false);
                        setDisableCode("");
                     }}
                     className="absolute inset-0 bg-zinc-900/60 backdrop-blur-sm"
                  />
                  <motion.div
                     initial={{ opacity: 0, scale: 0.95, y: 20 }}
                     animate={{ opacity: 1, scale: 1, y: 0 }}
                     exit={{ opacity: 0, scale: 0.95, y: 20 }}
                     className="relative w-full max-w-[400px] overflow-hidden rounded-2xl bg-white shadow-2xl p-6 sm:p-8"
                  >
                     <div
                        className={`h-1.5 w-full absolute top-0 left-0 ${twoFactorAction === "REACTIVATE" ? "bg-[#003580]" : "bg-red-600"}`}
                     />
                     <h3 className="text-lg font-bold text-zinc-900 mt-2">
                        {twoFactorAction === "DISABLE" && t("twoFactor.disableModalTitle")}
                        {twoFactorAction === "REMOVE" && t("twoFactor.removeModalTitle")}
                        {twoFactorAction === "REACTIVATE" && t("twoFactor.reactivateModalTitle")}
                     </h3>
                     <p className="text-xs text-zinc-500 mt-2 leading-relaxed">
                        {twoFactorAction === "DISABLE" && t("twoFactor.disableModalDesc")}
                        {twoFactorAction === "REMOVE" && t("twoFactor.removeModalDesc")}
                        {twoFactorAction === "REACTIVATE" && t("twoFactor.reactivateModalDesc")}
                     </p>
                     <input
                        type="text"
                        maxLength={twoFactorAction === "REMOVE" ? 8 : 6}
                        value={disableCode}
                        onChange={(e) => setDisableCode(e.target.value.replace(/[^0-9]/g, ""))}
                        placeholder={
                           twoFactorAction === "REMOVE"
                              ? t("twoFactor.removeModalPlaceholder")
                              : t("twoFactor.disableModalPlaceholder")
                        }
                        className={`w-full h-11 border border-zinc-200 rounded-lg px-3 mt-4 text-center font-bold tracking-widest text-lg outline-none transition-colors ${twoFactorAction === "REACTIVATE" ? "focus:border-[#003580]" : "focus:border-red-600"} focus:ring-0`}
                     />
                     <div className="flex gap-3 mt-6">
                        <button
                           onClick={() => {
                              setIsDisable2FAOpen(false);
                              setDisableCode("");
                           }}
                           className="flex-1 py-2.5 text-xs font-bold text-zinc-500 border border-zinc-200 rounded-lg hover:bg-zinc-50 transition-colors"
                        >
                           {tDetails("cancel")}
                        </button>
                        <button
                           onClick={async () => {
                              try {
                                 setIsDisabling(true);
                                 if (twoFactorAction === "DISABLE") {
                                    await securityService.disable2FA(disableCode);
                                    toast.success(
                                       t("twoFactor.disableSuccess") ||
                                          "Đã vô hiệu hóa 2FA thành công!"
                                    );
                                 } else if (twoFactorAction === "REMOVE") {
                                    await securityService.remove2FA(disableCode);
                                    toast.success(
                                       t("twoFactor.removeSuccess") || "Đã gỡ bỏ hoàn toàn 2FA!"
                                    );
                                 } else if (twoFactorAction === "REACTIVATE") {
                                    await securityService.enable2FA(disableCode);
                                    toast.success(
                                       t("twoFactor.reactivateSuccess") ||
                                          "Đã kích hoạt lại 2FA thành công!"
                                    );
                                 }
                                 setIsDisable2FAOpen(false);
                                 setDisableCode("");
                                 fetch2FAStatus();
                              } catch (err: unknown) {
                                 const apiError = err as { errorCode?: string };
                                 const msg = apiError.errorCode
                                    ? tErrors(apiError.errorCode)
                                    : t("twoFactor.modal.invalidCode");
                                 toast.error(msg);
                              } finally {
                                 setIsDisabling(false);
                              }
                           }}
                           disabled={
                              isDisabling ||
                              (twoFactorAction === "REMOVE"
                                 ? disableCode.length !== 6 && disableCode.length !== 8
                                 : disableCode.length !== 6)
                           }
                           className={`flex-1 py-2.5 text-xs font-bold text-white rounded-lg disabled:opacity-40 transition-colors flex items-center justify-center gap-1.5 ${twoFactorAction === "REACTIVATE" ? "bg-[#003580] hover:bg-[#002b66]" : "bg-red-600 hover:bg-red-700"}`}
                        >
                           {isDisabling && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                           {twoFactorAction === "DISABLE" && t("twoFactor.disableModalButton")}
                           {twoFactorAction === "REMOVE" && t("twoFactor.removeModalButton")}
                           {twoFactorAction === "REACTIVATE" && t("twoFactor.reactivate")}
                        </button>
                     </div>
                  </motion.div>
               </div>
            )}
         </AnimatePresence>
      </div>
   );
}

function SecurityCard({
   icon,
   title,
   description,
   action,
   children,
}: {
   icon: React.ReactNode;
   title: React.ReactNode;
   description: string;
   action: React.ReactNode;
   children?: React.ReactNode;
}) {
   return (
      <div className="bg-white rounded-lg border border-zinc-200 shadow-sm overflow-hidden">
         <div className="p-6">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
               <div className="flex gap-4">
                  <div className="h-12 w-12 rounded-full bg-zinc-50 flex items-center justify-center shrink-0">
                     {icon}
                  </div>
                  <div>
                     <h3 className="font-bold text-zinc-900">{title}</h3>
                     <p className="text-sm text-zinc-500 mt-1 max-w-md">{description}</p>
                  </div>
               </div>
               <div className="shrink-0">{action}</div>
            </div>
            {children}
         </div>
      </div>
   );
}
