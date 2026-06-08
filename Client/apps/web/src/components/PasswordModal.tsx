"use client";

import { useState } from "react";
import { X, Lock, Eye, EyeOff, Loader2 } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { profileService } from "@/lib/api/services/profileService";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

interface PasswordModalProps {
   isOpen: boolean;
   onClose: () => void;
   hasPassword: boolean;
   onSuccess: () => void;
}

export default function PasswordModal({
   isOpen,
   onClose,
   hasPassword,
   onSuccess,
}: PasswordModalProps) {
   const [currentPassword, setCurrentPassword] = useState("");
   const [newPassword, setNewPassword] = useState("");
   const [confirmPassword, setConfirmPassword] = useState("");

   const [showCurrent, setShowCurrent] = useState(false);
   const [showNew, setShowNew] = useState(false);
   const [showConfirm, setShowConfirm] = useState(false);

   const [loading, setLoading] = useState(false);
   const [errors, setErrors] = useState<{ [key: string]: string }>({});

   const t = useTranslations("Profile.Security.password");
   const tErrors = useTranslations("Errors");
   const tDetails = useTranslations("Profile.details");

   const validate = () => {
      const newErrors: { [key: string]: string } = {};

      if (hasPassword && !currentPassword) {
         newErrors.currentPassword = t("modal.required");
      }

      if (!newPassword) {
         newErrors.newPassword = t("modal.required");
      } else if (newPassword.length < 6) {
         newErrors.newPassword = t("modal.tooShort");
      }

      if (!confirmPassword) {
         newErrors.confirmPassword = t("modal.required");
      } else if (confirmPassword !== newPassword) {
         newErrors.confirmPassword = t("modal.mismatch");
      }

      setErrors(newErrors);
      return Object.keys(newErrors).length === 0;
   };

   const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      if (!validate()) return;

      try {
         setLoading(true);
         await profileService.changePassword({
            currentPassword: hasPassword ? currentPassword : undefined,
            newPassword,
         });

         toast.success(hasPassword ? t("modal.successChange") : t("modal.successCreate"));
         onSuccess();
         resetForm();
         onClose();
      } catch (error: unknown) {
         const apiError = error as { errorCode?: string };
         const msg = apiError.errorCode ? tErrors(apiError.errorCode) : tErrors("GEN_999");
         toast.error(msg);
      } finally {
         setLoading(false);
      }
   };

   const resetForm = () => {
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setShowCurrent(false);
      setShowNew(false);
      setShowConfirm(false);
      setErrors({});
   };

   const handleClose = () => {
      resetForm();
      onClose();
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
                  onClick={handleClose}
                  className="absolute inset-0 bg-zinc-900/60 backdrop-blur-sm"
               />

               {/* Modal Content */}
               <motion.div
                  initial={{ opacity: 0, scale: 0.95, y: 20 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.95, y: 20 }}
                  className="relative w-full max-w-[460px] overflow-hidden rounded-2xl bg-white shadow-2xl"
               >
                  {/* Top Bar Decoration */}
                  <div className="h-1.5 w-full bg-[#003580]" />

                  <button
                     onClick={handleClose}
                     className="absolute right-4 top-5 p-2 text-zinc-400 hover:text-zinc-600 transition-colors"
                  >
                     <X className="h-5 w-5" />
                  </button>

                  <div className="px-6 py-8 sm:p-8">
                     <div className="flex flex-col items-center text-center">
                        <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-blue-50">
                           <Lock className="h-7 w-7 text-[#003580]" />
                        </div>

                        <h2 className="text-2xl font-bold text-zinc-900 tracking-tight">
                           {hasPassword ? t("modal.titleChange") : t("modal.titleCreate")}
                        </h2>
                        <p className="mt-2 text-sm leading-relaxed text-zinc-500 max-w-[340px]">
                           {hasPassword ? t("modal.descChange") : t("modal.descCreate")}
                        </p>
                     </div>

                     <form onSubmit={handleSubmit} className="mt-8 space-y-5">
                        {/* Current Password Field */}
                        {hasPassword && (
                           <div className="space-y-1.5">
                              <label className="text-sm font-semibold text-zinc-700">
                                 {t("modal.currentPassword")}
                              </label>
                              <div className="relative">
                                 <input
                                    type={showCurrent ? "text" : "password"}
                                    value={currentPassword}
                                    onChange={(e) => setCurrentPassword(e.target.value)}
                                    placeholder={t("modal.placeholderCurrent")}
                                    className={`w-full rounded-lg border px-4 py-3 pr-10 text-sm outline-none transition-all placeholder:text-zinc-400 focus:ring-1 ${
                                       errors.currentPassword
                                          ? "border-red-500 focus:border-red-500 focus:ring-red-100"
                                          : "border-zinc-200 focus:border-[#003580] focus:ring-blue-100"
                                    }`}
                                 />
                                 <button
                                    type="button"
                                    onClick={() => setShowCurrent(!showCurrent)}
                                    className="absolute right-3 top-3.5 text-zinc-400 hover:text-zinc-600"
                                 >
                                    {showCurrent ? (
                                       <EyeOff className="h-4.5 w-4.5" />
                                    ) : (
                                       <Eye className="h-4.5 w-4.5" />
                                    )}
                                 </button>
                              </div>
                              {errors.currentPassword && (
                                 <p className="text-xs text-red-500 font-medium">
                                    {errors.currentPassword}
                                 </p>
                              )}
                           </div>
                        )}

                        {/* New Password Field */}
                        <div className="space-y-1.5">
                           <label className="text-sm font-semibold text-zinc-700">
                              {t("modal.newPassword")}
                           </label>
                           <div className="relative">
                              <input
                                 type={showNew ? "text" : "password"}
                                 value={newPassword}
                                 onChange={(e) => setNewPassword(e.target.value)}
                                 placeholder={t("modal.placeholderNew")}
                                 className={`w-full rounded-lg border px-4 py-3 pr-10 text-sm outline-none transition-all placeholder:text-zinc-400 focus:ring-1 ${
                                    errors.newPassword
                                       ? "border-red-500 focus:border-red-500 focus:ring-red-100"
                                       : "border-zinc-200 focus:border-[#003580] focus:ring-blue-100"
                                 }`}
                              />
                              <button
                                 type="button"
                                 onClick={() => setShowNew(!showNew)}
                                 className="absolute right-3 top-3.5 text-zinc-400 hover:text-zinc-600"
                              >
                                 {showNew ? (
                                    <EyeOff className="h-4.5 w-4.5" />
                                 ) : (
                                    <Eye className="h-4.5 w-4.5" />
                                 )}
                              </button>
                           </div>
                           {errors.newPassword && (
                              <p className="text-xs text-red-500 font-medium">
                                 {errors.newPassword}
                              </p>
                           )}
                        </div>

                        {/* Confirm Password Field */}
                        <div className="space-y-1.5">
                           <label className="text-sm font-semibold text-zinc-700">
                              {t("modal.confirmPassword")}
                           </label>
                           <div className="relative">
                              <input
                                 type={showConfirm ? "text" : "password"}
                                 value={confirmPassword}
                                 onChange={(e) => setConfirmPassword(e.target.value)}
                                 placeholder={t("modal.placeholderConfirm")}
                                 className={`w-full rounded-lg border px-4 py-3 pr-10 text-sm outline-none transition-all placeholder:text-zinc-400 focus:ring-1 ${
                                    errors.confirmPassword
                                       ? "border-red-500 focus:border-red-500 focus:ring-red-100"
                                       : "border-zinc-200 focus:border-[#003580] focus:ring-blue-100"
                                 }`}
                              />
                              <button
                                 type="button"
                                 onClick={() => setShowConfirm(!showConfirm)}
                                 className="absolute right-3 top-3.5 text-zinc-400 hover:text-zinc-600"
                              >
                                 {showConfirm ? (
                                    <EyeOff className="h-4.5 w-4.5" />
                                 ) : (
                                    <Eye className="h-4.5 w-4.5" />
                                 )}
                              </button>
                           </div>
                           {errors.confirmPassword && (
                              <p className="text-xs text-red-500 font-medium">
                                 {errors.confirmPassword}
                              </p>
                           )}
                        </div>

                        {/* Form Submission */}
                        <div className="mt-8 flex gap-3 pt-2">
                           <button
                              type="button"
                              onClick={handleClose}
                              disabled={loading}
                              className="w-1/2 rounded-lg border border-zinc-200 py-3 text-sm font-semibold text-zinc-600 transition-all hover:bg-zinc-50 disabled:opacity-50"
                           >
                              {tDetails("cancel")}
                           </button>
                           <button
                              type="submit"
                              disabled={loading}
                              className="w-1/2 flex items-center justify-center gap-2 rounded-lg bg-[#003580] py-3 text-sm font-bold text-white transition-all hover:bg-[#002b66] disabled:opacity-50 active:scale-[0.98]"
                           >
                              {loading ? (
                                 <Loader2 className="h-4 w-4 animate-spin" />
                              ) : hasPassword ? (
                                 t("modal.submitChange")
                              ) : (
                                 t("modal.submitCreate")
                              )}
                           </button>
                        </div>
                     </form>
                  </div>
               </motion.div>
            </div>
         )}
      </AnimatePresence>
   );
}
