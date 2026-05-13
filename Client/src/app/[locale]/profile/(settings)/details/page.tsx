"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import countries from "i18n-iso-countries";
import Select from "react-select";
import {
   User,
   Loader2,
   Mail,
   Phone,
   Globe,
   MapPin,
   Calendar,
   UserCircle,
   Camera,
} from "lucide-react";
import Image from "next/image";
import { useTranslations, useLocale } from "next-intl";

// Register country languages
import enLocale from "i18n-iso-countries/langs/en.json";
import viLocale from "i18n-iso-countries/langs/vi.json";
countries.registerLocale(enLocale);
countries.registerLocale(viLocale);

import { authService } from "@/lib/api/services/authService";
import { profileService, UserProfile as UserProfileType } from "@/lib/api/services/profileService";
import { toast } from "sonner";

export default function PersonalDetailsPage() {
   const [isResending, setIsResending] = useState(false);
   const [profileData, setProfileData] = useState<UserProfileType | null>(null);
   const [isLoading, setIsLoading] = useState(true);
   const [cooldown, setCooldown] = useState(0);
   const locale = useLocale();

   const options = useMemo(() => {
      const obj = countries.getNames(locale, { select: "official" });
      return Object.entries(obj).map(([key, value]) => ({
         label: value,
         value: key,
      }));
   }, [locale]);

   const [editingField, setEditingField] = useState<keyof UserProfileType | null>(null);
   const [editValue, setEditValue] = useState<string>("");
   const [isSaving, setIsSaving] = useState(false);

   const tDetails = useTranslations("Profile.details");

   const fetchProfile = useCallback(async () => {
      try {
         const data = await profileService.getMyProfile();
         setProfileData(data);
      } catch (error) {
         console.error("Failed to fetch profile:", error);
         toast.error("Failed to load profile information");
      } finally {
         setIsLoading(false);
      }
   }, []);

   useEffect(() => {
      const timer = setTimeout(() => fetchProfile(), 0);
      return () => clearTimeout(timer);
   }, [fetchProfile]);

   useEffect(() => {
      if (cooldown > 0) {
         const timer = setInterval(() => {
            setCooldown((prev) => prev - 1);
         }, 1000);
         return () => clearInterval(timer);
      }
   }, [cooldown]);

   const handleResendVerification = async () => {
      if (isResending || cooldown > 0) return;
      setIsResending(true);
      try {
         await authService.resendVerification();
         toast.success(tDetails("verificationSent"));
         setCooldown(30);
      } catch (err: unknown) {
         const error = err as { response?: { data?: { errorCode?: string } } };
         if (error?.response?.data?.errorCode === "AUTH_011") {
            toast.error(tDetails("rateLimitError"));
            setCooldown(30);
         } else {
            toast.error("Failed to send verification email");
         }
      } finally {
         setIsResending(false);
      }
   };

   const handleUpdateField = async () => {
      if (!editingField || isSaving) return;
      setIsSaving(true);
      try {
         const updated = await profileService.updateMyProfile({ [editingField]: editValue });
         setProfileData(updated);
         toast.success("Cập nhật thành công");
         setEditingField(null);
      } catch (error) {
         console.error(`Update ${editingField} failed:`, error);
         toast.error("Cập nhật thất bại");
      } finally {
         setIsSaving(false);
      }
   };

   const startEditing = (field: keyof UserProfileType, value: string | null) => {
      setEditingField(field);
      if (field === "gender" && !value) {
         setEditValue("MALE");
      } else {
         setEditValue(value || "");
      }
   };

   if (isLoading) {
      return (
         <div className="flex h-64 items-center justify-center">
            <Loader2 className="h-8 w-8 animate-spin text-[#006ce4]" />
         </div>
      );
   }

   return (
      <div className="animate-in fade-in slide-in-from-bottom-2 duration-500">
         <div className="flex justify-between items-start mb-8">
            <div>
               <h1 className="text-3xl font-bold text-zinc-900 tracking-tight">
                  {tDetails("title")}
               </h1>
               <p className="text-[15px] text-zinc-500 mt-2">{tDetails("subtitle")}</p>
            </div>
            <div className="relative group">
               <div className="h-24 w-24 rounded-full border-4 border-white bg-zinc-100 shadow-xl overflow-hidden relative">
                  {profileData?.avatarUrl ? (
                     <Image
                        src={profileData.avatarUrl}
                        alt="Avatar"
                        fill
                        className="object-cover transition-transform group-hover:scale-110"
                        unoptimized
                     />
                  ) : (
                     <div className="h-full w-full flex items-center justify-center bg-linear-to-br from-[#006ce4] to-[#003580] text-white text-3xl font-bold">
                        {profileData?.displayName?.charAt(0) || profileData?.email?.charAt(0)}
                     </div>
                  )}
                  <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center cursor-pointer">
                     <Camera className="h-7 w-7 text-white" />
                  </div>
               </div>
            </div>
         </div>

         <div className="bg-white rounded-2xl border border-zinc-100 shadow-sm overflow-hidden">
            <InfoRow
               icon={<UserCircle className="h-5 w-5 text-zinc-400" />}
               label={tDetails("displayName")}
               value={profileData?.displayName || ""}
               isEditing={editingField === "displayName"}
               onSave={handleUpdateField}
               onCancel={() => setEditingField(null)}
               onStartEdit={() => startEditing("displayName", profileData?.displayName || "")}
               isSaving={isSaving}
               saveLabel={tDetails("save")}
               cancelLabel={tDetails("cancel")}
               editLabel={tDetails("edit")}
               renderEditField={() => (
                  <input
                     type="text"
                     value={editValue}
                     onChange={(e) => setEditValue(e.target.value)}
                     className="w-full px-3 py-2 rounded-lg border border-zinc-200 focus:border-[#006ce4] focus:outline-none text-[14px]"
                     autoFocus
                  />
               )}
            />
            <InfoRow
               icon={<Mail className="h-5 w-5 text-zinc-400" />}
               label={tDetails("email")}
               value={
                  <div className="flex flex-col gap-1">
                     <div className="flex items-center gap-2">
                        <span className="text-zinc-900">{profileData?.email}</span>
                        {profileData?.verified ? (
                           <span className="rounded bg-green-100 px-2 py-0.5 text-[10px] font-bold text-green-700 uppercase">
                              {tDetails("verified")}
                           </span>
                        ) : (
                           <button
                              onClick={handleResendVerification}
                              disabled={isResending || cooldown > 0}
                              className="flex items-center gap-1 rounded bg-orange-500 px-2 py-0.5 text-[10px] font-bold text-white uppercase hover:bg-orange-600 transition-colors disabled:opacity-70 disabled:cursor-not-allowed cursor-pointer"
                           >
                              {isResending ? (
                                 <Loader2 className="h-3 w-3 animate-spin" />
                              ) : cooldown > 0 ? (
                                 <span>{cooldown}s</span>
                              ) : null}
                              {cooldown > 0 ? tDetails("resendIn") : tDetails("verifyNow")}
                           </button>
                        )}
                     </div>
                     <p className="text-[12px] text-zinc-500 mt-0.5">{tDetails("emailDesc")}</p>
                  </div>
               }
            />
            <InfoRow
               icon={<Phone className="h-5 w-5 text-zinc-400" />}
               label={tDetails("phoneNumber")}
               value={
                  <div className="flex flex-col gap-1">
                     <span
                        className={
                           profileData?.phoneNumber ? "text-zinc-900" : "text-zinc-400 italic"
                        }
                     >
                        {profileData?.phoneNumber || tDetails("notProvided")}
                     </span>
                     <p className="text-[12px] text-zinc-500 mt-0.5">{tDetails("phoneDesc")}</p>
                  </div>
               }
               isEditing={editingField === "phoneNumber"}
               onSave={handleUpdateField}
               onCancel={() => setEditingField(null)}
               onStartEdit={() => startEditing("phoneNumber", profileData?.phoneNumber || "")}
               isSaving={isSaving}
               saveLabel={tDetails("save")}
               cancelLabel={tDetails("cancel")}
               editLabel={tDetails("edit")}
               renderEditField={() => (
                  <input
                     type="text"
                     value={editValue}
                     onChange={(e) => setEditValue(e.target.value)}
                     className="w-full px-3 py-2 rounded-lg border border-zinc-200 focus:border-[#006ce4] focus:outline-none text-[14px]"
                     autoFocus
                  />
               )}
            />
            <InfoRow
               icon={<Calendar className="h-5 w-5 text-zinc-400" />}
               label={tDetails("dateOfBirth")}
               value={profileData?.dateOfBirth || tDetails("enterDob")}
               isEditing={editingField === "dateOfBirth"}
               onSave={handleUpdateField}
               onCancel={() => setEditingField(null)}
               onStartEdit={() => startEditing("dateOfBirth", profileData?.dateOfBirth || "")}
               isSaving={isSaving}
               saveLabel={tDetails("save")}
               cancelLabel={tDetails("cancel")}
               editLabel={tDetails("edit")}
               renderEditField={() => (
                  <input
                     type="date"
                     value={editValue}
                     onChange={(e) => setEditValue(e.target.value)}
                     className="w-full px-3 py-2 rounded-lg border border-zinc-200 focus:border-[#006ce4] focus:outline-none text-[14px]"
                     autoFocus
                  />
               )}
            />
            <InfoRow
               icon={<Globe className="h-5 w-5 text-zinc-400" />}
               label={tDetails("nationality")}
               value={
                  profileData?.nationality
                     ? countries.getName(profileData.nationality, locale) || profileData.nationality
                     : tDetails("selectNationality")
               }
               isEditing={editingField === "nationality"}
               onSave={handleUpdateField}
               onCancel={() => setEditingField(null)}
               onStartEdit={() => startEditing("nationality", profileData?.nationality || "")}
               isSaving={isSaving}
               saveLabel={tDetails("save")}
               cancelLabel={tDetails("cancel")}
               editLabel={tDetails("edit")}
               renderEditField={() => (
                  <Select
                     options={options}
                     value={options?.find((o) => o.value === editValue)}
                     onChange={(val: { value: string } | null) => setEditValue(val?.value || "")}
                     className="text-[14px]"
                     classNamePrefix="select"
                     placeholder="Chọn quốc gia..."
                     autoFocus
                  />
               )}
            />
            <InfoRow
               icon={<User className="h-5 w-5 text-zinc-400" />}
               label={tDetails("gender")}
               value={
                  profileData?.gender
                     ? tDetails(`genderOptions.${profileData.gender}`)
                     : tDetails("selectGender")
               }
               isEditing={editingField === "gender"}
               onSave={handleUpdateField}
               onCancel={() => setEditingField(null)}
               onStartEdit={() => startEditing("gender", profileData?.gender || "")}
               isSaving={isSaving}
               saveLabel={tDetails("save")}
               cancelLabel={tDetails("cancel")}
               editLabel={tDetails("edit")}
               renderEditField={() => (
                  <select
                     value={editValue}
                     onChange={(e) => setEditValue(e.target.value)}
                     className="w-full px-3 py-2 rounded-lg border border-zinc-200 focus:border-[#006ce4] focus:outline-none text-[14px]"
                     autoFocus
                  >
                     {[
                        { label: tDetails("genderOptions.MALE"), value: "MALE" },
                        { label: tDetails("genderOptions.FEMALE"), value: "FEMALE" },
                        { label: tDetails("genderOptions.OTHER"), value: "OTHER" },
                     ].map((opt) => (
                        <option key={opt.value} value={opt.value}>
                           {opt.label}
                        </option>
                     ))}
                  </select>
               )}
            />
            <InfoRow
               icon={<MapPin className="h-5 w-5 text-zinc-400" />}
               label={tDetails("address")}
               value={profileData?.address || tDetails("enterAddress")}
               isEditing={editingField === "address"}
               onSave={handleUpdateField}
               onCancel={() => setEditingField(null)}
               onStartEdit={() => startEditing("address", profileData?.address || "")}
               isSaving={isSaving}
               saveLabel={tDetails("save")}
               cancelLabel={tDetails("cancel")}
               editLabel={tDetails("edit")}
               noBorder
               renderEditField={() => (
                  <input
                     type="text"
                     value={editValue}
                     onChange={(e) => setEditValue(e.target.value)}
                     className="w-full px-3 py-2 rounded-lg border border-zinc-200 focus:border-[#006ce4] focus:outline-none text-[14px]"
                     autoFocus
                  />
               )}
            />
         </div>
      </div>
   );
}

interface InfoRowProps {
   icon: React.ReactNode;
   label: string;
   value: React.ReactNode;
   isEditing?: boolean;
   isSaving?: boolean;
   onSave?: () => void;
   onCancel?: () => void;
   onStartEdit?: () => void;
   saveLabel?: string;
   cancelLabel?: string;
   editLabel?: string;
   noBorder?: boolean;
   renderEditField?: () => React.ReactNode;
}

function InfoRow({
   icon,
   label,
   value,
   isEditing,
   isSaving,
   onSave,
   onCancel,
   onStartEdit,
   saveLabel = "Save",
   cancelLabel = "Cancel",
   editLabel = "Edit",
   noBorder = false,
   renderEditField,
}: InfoRowProps) {
   return (
      <div
         className={`grid grid-cols-12 py-7 gap-4 transition-all hover:bg-zinc-50/80 px-6 ${!noBorder ? "border-b border-zinc-100" : ""}`}
      >
         <div className="col-span-4 flex items-center gap-3">
            {icon}
            <div className="text-[14px] font-semibold text-zinc-900">{label}</div>
         </div>

         <div className="col-span-6 flex items-center">
            {isEditing && renderEditField ? (
               <div className="w-full">{renderEditField()}</div>
            ) : (
               <div className="text-[14px] text-zinc-600 font-medium">{value}</div>
            )}
         </div>

         <div className="col-span-2 flex items-center justify-end">
            {isEditing ? (
               <div className="flex gap-2">
                  <button
                     onClick={onSave}
                     disabled={isSaving}
                     className="text-[13px] font-bold text-[#006ce4] hover:underline disabled:opacity-50"
                  >
                     {isSaving ? <Loader2 className="h-4 w-4 animate-spin" /> : saveLabel}
                  </button>
                  <button
                     onClick={onCancel}
                     className="text-[13px] font-bold text-zinc-400 hover:text-zinc-600 hover:underline"
                  >
                     {cancelLabel}
                  </button>
               </div>
            ) : onStartEdit ? (
               <button
                  onClick={onStartEdit}
                  className="text-[13px] font-bold text-[#006ce4] hover:text-[#003580] transition-colors cursor-pointer"
               >
                  {editLabel}
               </button>
            ) : null}
         </div>
      </div>
   );
}
