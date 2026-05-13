"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Fingerprint, Smartphone, LogOut, Trash2, Loader2, Lock } from "lucide-react";
import { passkeyService, Passkey } from "@/services/passkeyService";
import { motion, AnimatePresence } from "framer-motion";
import { startRegistration } from "@simplewebauthn/browser";
import { toast } from "sonner";
import { useLocale } from "next-intl";

export default function SecurityPage() {
   const [isRegistering, setIsRegistering] = useState(false);
   const [hasPasskey, setHasPasskey] = useState(false);
   const [isLoadingStatus, setIsLoadingStatus] = useState(true);
   const [passkeys, setPasskeys] = useState<Passkey[]>([]);
   const [showPasskeyManager, setShowPasskeyManager] = useState(false);

   const t = useTranslations("Profile.Security");
   const tDetails = useTranslations("Profile.details");
   const locale = useLocale();

   const fetchPasskeyData = async () => {
      try {
         setIsLoadingStatus(true);
         const status = await passkeyService.checkStatus();
         setHasPasskey(status);

         if (status) {
            const passkeyData = await passkeyService.listPasskeys();
            setPasskeys(passkeyData);
         }
      } catch (err) {
         console.error("Failed to fetch passkey data:", err);
      } finally {
         setIsLoadingStatus(false);
      }
   };

   useEffect(() => {
      const timer = setTimeout(() => fetchPasskeyData(), 0);
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
         if (err instanceof Error && err.name === "NotAllowedError") return;
         toast.error(t("passkey.setupError"));
      } finally {
         setIsRegistering(false);
      }
   };

   const handleDeletePasskey = async (id: string) => {
      try {
         await passkeyService.deletePasskey(id);
         toast.success(t("manager.deleteSuccess"));
         fetchPasskeyData();
      } catch {
         toast.error(t("passkey.deleteError"));
      }
   };

   return (
      <div className="animate-in fade-in slide-in-from-bottom-2 duration-500">
         <div className="mb-8">
            <h1 className="text-3xl font-bold text-zinc-900 tracking-tight">{t("title")}</h1>
            <p className="text-[15px] text-zinc-500 mt-2">{t("subtitle")}</p>
         </div>

         <div className="space-y-4">
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

            <SecurityCard
               icon={<Smartphone className="h-6 w-6 text-zinc-400" />}
               title={t("twoFactor.title")}
               description={t("twoFactor.desc")}
               action={
                  <button className="text-sm font-bold text-[#006ce4] hover:bg-blue-50 px-4 py-2 rounded-md transition-all cursor-pointer">
                     {t("twoFactor.setup")}
                  </button>
               }
            />

            <SecurityCard
               icon={<Lock className="h-6 w-6 text-zinc-400" />}
               title={t("password.title")}
               description={t("password.desc")}
               action={
                  <button className="text-sm font-bold text-[#006ce4] hover:bg-blue-50 px-4 py-2 rounded-md transition-all cursor-pointer">
                     {t("password.action")}
                  </button>
               }
            />

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
   title: string;
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
