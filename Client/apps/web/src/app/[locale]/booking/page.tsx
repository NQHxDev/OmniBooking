"use client";

import { useState, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { differenceInDays, parseISO, format } from "date-fns";
import { vi, enUS } from "date-fns/locale";
import {
   User,
   Mail,
   MessageSquare,
   CreditCard,
   ArrowRight,
   AlertCircle,
   ChevronLeft,
   Sparkles,
   CheckCircle,
   X,
   Wallet,
   Landmark,
} from "lucide-react";
import { useAuthStore } from "@/store/useAuthStore";
import { useSettingStore } from "@/store/useSettingStore";
import { propertyService, PropertyDetailResponse } from "@/services/propertyService";
import { authService } from "@/lib/api/services/authService";
import { bookingService } from "@/lib/api/services/bookingService";
import { paymentService } from "@/lib/api/services/paymentService";
import PriceDisplay from "@/components/PriceDisplay";
import Select from "react-select";
import type { CountryCode } from "libphonenumber-js";
import { profileService } from "@/lib/api/services/profileService";

const countriesList: {
   code: CountryCode;
   callingCode: string;
   label: string;
   flag: string;
}[] = [
   { code: "VN", callingCode: "+84", label: "Vietnam (+84)", flag: "🇻🇳" },
   { code: "US", callingCode: "+1", label: "United States (+1)", flag: "🇺🇸" },
   { code: "GB", callingCode: "+44", label: "United Kingdom (+44)", flag: "🇬🇧" },
   { code: "SG", callingCode: "+65", label: "Singapore (+65)", flag: "🇸🇬" },
   { code: "TH", callingCode: "+66", label: "Thailand (+66)", flag: "🇹🇭" },
   { code: "MY", callingCode: "+60", label: "Malaysia (+60)", flag: "🇲🇾" },
   { code: "ID", callingCode: "+62", label: "Indonesia (+62)", flag: "🇮🇩" },
   { code: "JP", callingCode: "+81", label: "Japan (+81)", flag: "🇯🇵" },
   { code: "KR", callingCode: "+82", label: "South Korea (+82)", flag: "🇰🇷" },
   { code: "FR", callingCode: "+33", label: "France (+33)", flag: "🇫🇷" },
   { code: "DE", callingCode: "+49", label: "Germany (+49)", flag: "🇩🇪" },
   { code: "AU", callingCode: "+61", label: "Australia (+61)", flag: "🇦🇺" },
   { code: "CN", callingCode: "+86", label: "China (+86)", flag: "🇨🇳" },
   { code: "IN", callingCode: "+91", label: "India (+91)", flag: "🇮🇳" },
];

export default function BookingPage() {
   const locale = useLocale();
   const t = useTranslations("Booking");
   const router = useRouter();
   const searchParams = useSearchParams();
   const dateLocale = locale === "vi" ? vi : enUS;

   const propertyId = searchParams.get("propertyId");
   const roomTypeId = searchParams.get("roomTypeId");
   const checkin = searchParams.get("checkin");
   const checkout = searchParams.get("checkout");
   const roomsCount = Number(searchParams.get("rooms")) || 1;

   const { user: authUser, isLoggedIn } = useAuthStore();
   const { currency } = useSettingStore();

   // Form states
   const [guestName, setGuestName] = useState("");
   const [guestEmail, setGuestEmail] = useState("");
   const [selectedCountry, setSelectedCountry] = useState(countriesList[0]);
   const [guestPhone, setGuestPhone] = useState("");
   const [specialRequests, setSpecialRequests] = useState("");
   const [appliedCoupon] = useState<{ id: string } | null>(null);
   const [paymentMethod, setPaymentMethod] = useState<"visa" | "momo" | "banking">("visa");
   const [errors, setErrors] = useState<{
      guestName?: string;
      guestEmail?: string;
      guestPhone?: string;
   }>({});

   // UI States
   const [property, setProperty] = useState<PropertyDetailResponse | null>(null);
   const [loading, setLoading] = useState(true);
   const [submitting, setSubmitting] = useState(false);
   const [error, setError] = useState<string | null>(null);
   const [showLoginPrompt, setShowLoginPrompt] = useState(false);
   const [showPaymentModal, setShowPaymentModal] = useState(false);
   const [paying, setPaying] = useState(false);

   // Track last seen auth state to detect updates
   const [prevAuth, setPrevAuth] = useState({
      isLoggedIn,
      userEmail: authUser?.email,
      userFullName: authUser?.fullName,
   });

   // Adjust state during render when auth state changes
   if (
      prevAuth.isLoggedIn !== isLoggedIn ||
      prevAuth.userEmail !== authUser?.email ||
      prevAuth.userFullName !== authUser?.fullName
   ) {
      setPrevAuth({
         isLoggedIn,
         userEmail: authUser?.email,
         userFullName: authUser?.fullName,
      });

      if (isLoggedIn && authUser) {
         setGuestName(authUser.fullName || "");
         setGuestEmail(authUser.email || "");
      }
   }

   // Fetch user profile to pre-fill phone number if logged in
   useEffect(() => {
      if (isLoggedIn) {
         profileService
            .getMyProfile()
            .then((data) => {
               if (data && data.phoneNumber) {
                  const matchedCountry = countriesList.find((c) =>
                     data.phoneNumber?.startsWith(c.callingCode)
                  );
                  if (matchedCountry) {
                     setSelectedCountry(matchedCountry);
                     setGuestPhone(data.phoneNumber.slice(matchedCountry.callingCode.length));
                  } else {
                     setGuestPhone(data.phoneNumber);
                  }
               }
            })
            .catch((err) => {
               console.error("Failed to fetch user profile:", err);
            });
      }
   }, [isLoggedIn]);

   // Fetch property details
   useEffect(() => {
      if (!propertyId) return;
      propertyService
         .getPropertyDetail(propertyId)
         .then((data) => {
            setProperty(data);
            setLoading(false);
         })
         .catch((err) => {
            console.error(err);
            setError(t("failedToLoadPropertyDetails"));
            setLoading(false);
         });
   }, [propertyId, locale]);

   if (loading) {
      return (
         <div className="min-h-screen flex items-center justify-center bg-zinc-50">
            <div className="flex flex-col items-center gap-4">
               <div className="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
               <p className="text-zinc-500 font-medium">{t("loadingBookingDetails")}</p>
            </div>
         </div>
      );
   }

   if (error || !property || !roomTypeId || !checkin || !checkout) {
      return (
         <div className="min-h-screen flex items-center justify-center bg-zinc-50">
            <div className="bg-white p-8 rounded-2xl border border-zinc-200 shadow-sm max-w-md text-center space-y-4">
               <AlertCircle className="h-12 w-12 text-red-500 mx-auto" />
               <h2 className="text-xl font-bold text-zinc-900">{t("invalidInformation")}</h2>
               <p className="text-zinc-500 text-sm">
                  {error || t("missingRequiredBookingDetails")}
               </p>
               <button
                  onClick={() => router.back()}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-xl font-bold text-sm transition-colors cursor-pointer"
               >
                  {t("goBack")}
               </button>
            </div>
         </div>
      );
   }

   const roomType = property.roomTypes?.find((r) => r.id === roomTypeId);
   if (!roomType) {
      return (
         <div className="min-h-screen flex items-center justify-center bg-zinc-50">
            <div className="bg-white p-8 rounded-2xl border border-zinc-200 text-center max-w-md space-y-4">
               <AlertCircle className="h-12 w-12 text-red-500 mx-auto" />
               <h2 className="text-xl font-bold text-zinc-900">{t("roomTypeNotFound")}</h2>
               <button
                  onClick={() => router.back()}
                  className="bg-blue-600 text-white px-6 py-2 rounded-xl"
               >
                  {t("goBack")}
               </button>
            </div>
         </div>
      );
   }

   const nights = Math.max(1, differenceInDays(parseISO(checkout), parseISO(checkin)));
   const basePrice = roomType.basePrice * nights * roomsCount;
   const finalPrice = basePrice; // In real flow, handle coupon discount on backend response

   const checkinDate = parseISO(checkin);
   const todayDate = new Date();
   todayDate.setHours(0, 0, 0, 0);
   const daysUntilCheckin = differenceInDays(checkinDate, todayDate);
   const requiresDeposit = !isLoggedIn || daysUntilCheckin >= 5;

   const firstNightPrice = roomType.basePrice * roomsCount;
   const fifteenPercent = finalPrice * 0.15;
   const depositAmount = requiresDeposit ? Math.min(fifteenPercent, firstNightPrice) : 0;
   const payLaterAmount = finalPrice - depositAmount;

   const handleEmailBlur = async () => {
      if (isLoggedIn) return; // Skip if logged in
      if (!guestEmail || !guestEmail.includes("@")) return;

      try {
         const hasAccount = await authService.checkEmail(guestEmail);
         if (hasAccount) {
            setShowLoginPrompt(true);
         }
      } catch (err) {
         console.error("Email check failed", err);
      }
   };

   const performBooking = async () => {
      setSubmitting(true);
      setError(null);

      const cleanDigits = guestPhone.replace(/\D/g, "");
      const digitsToValidate = cleanDigits.startsWith("0") ? cleanDigits.slice(1) : cleanDigits;
      const fullE164 = guestPhone ? selectedCountry.callingCode + digitsToValidate : undefined;

      try {
         const response = await bookingService.create({
            roomTypeId,
            checkInDate: checkin,
            checkOutDate: checkout,
            numRooms: roomsCount,
            guestName,
            guestEmail,
            guestPhone: fullE164,
            specialRequests: specialRequests || undefined,
            couponId: appliedCoupon?.id || undefined,
            currency,
            paymentMethod: paymentMethod.toUpperCase(),
         });

         if (requiresDeposit && (paymentMethod === "momo" || paymentMethod === "visa")) {
            const payData = await paymentService.createMomoPayment(response.id);
            if (payData && payData.payUrl) {
               window.location.href = payData.payUrl;
               return;
            } else {
               throw new Error(t("failedToCreateMomoPaymentLink"));
            }
         }

         // Navigate to success page with details in query params
         const successUrl =
            `/${locale}/booking/success` +
            `?bookingId=${response.id}` +
            `&code=${response.bookingCode}` +
            `&name=${encodeURIComponent(response.guestName)}` +
            `&email=${encodeURIComponent(response.guestEmail)}` +
            `&property=${encodeURIComponent(response.propertyName)}` +
            `&room=${encodeURIComponent(response.roomTypeName)}` +
            `&checkin=${response.checkInDate}` +
            `&checkout=${response.checkOutDate}` +
            `&rooms=${response.numRooms}` +
            `&total=${response.totalPrice}` +
            `&final=${response.finalPrice}` +
            `&deposit=${response.depositAmount || 0}` +
            `&requiresDeposit=${response.requiresDeposit || false}` +
            `&paymentMethod=${paymentMethod}` +
            (response.activationToken ? `&token=${response.activationToken}` : "");

         router.push(successUrl);
      } catch (err) {
         console.error(err);
         const errorWithResponse = err as { response?: { data?: { message?: string } } };
         setError(errorWithResponse.response?.data?.message || t("bookingFailedPleaseTryAgain"));
         setSubmitting(false);
      }
   };

   const handleSimulatedPayment = async () => {
      setPaying(true);
      await new Promise((resolve) => setTimeout(resolve, 1500));
      setPaying(false);
      setShowPaymentModal(false);
      await performBooking();
   };

   const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();

      const newErrors: { guestName?: string; guestEmail?: string; guestPhone?: string } = {};
      if (!guestName.trim()) {
         newErrors.guestName = t("pleaseEnterYourFullName");
      }

      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!guestEmail.trim()) {
         newErrors.guestEmail = t("pleaseEnterYourEmailAddress");
      } else if (!emailRegex.test(guestEmail.trim())) {
         newErrors.guestEmail = t("invalidEmailAddress");
      }

      if (!guestPhone.trim()) {
         newErrors.guestPhone = t("pleaseEnterYourContactPhoneNum");
      } else {
         const { isValidPhoneNumber } = await import("libphonenumber-js");
         const cleanDigits = guestPhone.replace(/\D/g, "");
         const digitsToValidate = cleanDigits.startsWith("0") ? cleanDigits.slice(1) : cleanDigits;
         const fullE164 = selectedCountry.callingCode + digitsToValidate;
         if (!isValidPhoneNumber(fullE164, selectedCountry.code)) {
            newErrors.guestPhone = t("invalidPhoneNumberForTheSelect");
         }
      }

      if (Object.keys(newErrors).length > 0) {
         setErrors(newErrors);
         return;
      }

      setErrors({});

      if (requiresDeposit && paymentMethod === "banking") {
         setShowPaymentModal(true);
      } else {
         await performBooking();
      }
   };

   return (
      <div className="min-h-screen bg-zinc-50/50 py-10 px-4 sm:px-6 lg:px-8 font-sans">
         {/* Suggest Login Dialog */}
         {showLoginPrompt && (
            <div className="fixed inset-0 bg-black/40 backdrop-blur-xs flex items-center justify-center z-50 p-4">
               <div className="bg-white rounded-3xl border border-zinc-200/80 shadow-2xl p-6 sm:p-8 max-w-md w-full relative animate-in fade-in zoom-in-95 duration-200">
                  <button
                     onClick={() => setShowLoginPrompt(false)}
                     className="absolute top-4 right-4 text-zinc-400 hover:text-zinc-600"
                  >
                     <X className="h-5 w-5" />
                  </button>
                  <div className="flex flex-col items-center text-center space-y-4">
                     <div className="p-3 bg-blue-50 rounded-full">
                        <Sparkles className="h-8 w-8 text-blue-600" />
                     </div>
                     <h3 className="text-xl font-bold text-zinc-900">{t("getGeniusDiscounts")}</h3>
                     <p className="text-sm text-zinc-500 leading-relaxed">
                        {t("thisEmailIsAlreadyRegisteredLo")}
                     </p>
                     <div className="flex flex-col gap-2 w-full pt-4">
                        <button
                           onClick={() =>
                              router.push(
                                 `/${locale}/auth/login?callbackUrl=${encodeURIComponent(window.location.href)}`
                              )
                           }
                           className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-6 rounded-xl transition-all cursor-pointer shadow-md"
                        >
                           {t("logInNow")}
                        </button>
                        <button
                           onClick={() => setShowLoginPrompt(false)}
                           className="text-zinc-500 hover:text-zinc-700 font-bold py-2.5 text-xs transition-colors cursor-pointer"
                        >
                           {t("continueAsGuest")}
                        </button>
                     </div>
                  </div>
               </div>
            </div>
         )}

         <div className="max-w-6xl mx-auto space-y-6">
            {/* Header */}
            <div className="flex items-center gap-3">
               <button
                  onClick={() => router.back()}
                  className="p-2 hover:bg-zinc-200/50 rounded-full transition-colors cursor-pointer"
               >
                  <ChevronLeft className="h-6 w-6 text-zinc-700" />
               </button>
               <h1 className="text-2xl font-bold text-zinc-900 tracking-tight">
                  {t("confirmAndPay")}
               </h1>
            </div>

            {error && (
               <div className="bg-red-50 border border-red-200 rounded-2xl p-4 flex items-center gap-3 text-red-700 text-sm font-medium animate-in fade-in duration-200">
                  <AlertCircle className="h-5 w-5 shrink-0 text-red-500" />
                  <span>{error}</span>
               </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
               {/* Left Column: Form */}
               <div className="lg:col-span-7 space-y-6">
                  {/* Contact Info Card */}
                  <div className="bg-white rounded-3xl p-6 sm:p-8 border border-zinc-200 shadow-xs space-y-6">
                     <div className="flex items-center gap-3 pb-4 border-b border-zinc-100">
                        <User className="h-5 w-5 text-blue-600" />
                        <h2 className="text-lg font-bold text-zinc-900">{t("contactDetails")}</h2>
                     </div>

                     <form
                        onSubmit={handleSubmit}
                        id="booking-form"
                        className="space-y-5"
                        noValidate
                     >
                        <div className="space-y-1.5">
                           <label className="text-xs font-bold text-zinc-500 uppercase tracking-wider">
                              {t("fullName")} *
                           </label>
                           <div className="relative">
                              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-zinc-400">
                                 <User className="h-4.5 w-4.5" />
                              </span>
                              <input
                                 type="text"
                                 id="guestName"
                                 name="name"
                                 autoComplete="name"
                                 placeholder={t("johnDoe")}
                                 value={guestName}
                                 onChange={(e) => {
                                    setGuestName(e.target.value);
                                    if (errors.guestName) {
                                       setErrors((prev) => ({ ...prev, guestName: undefined }));
                                    }
                                 }}
                                 className={`w-full pl-10 pr-4 py-3 border rounded-xl outline-hidden transition-all text-sm font-medium ${
                                    errors.guestName
                                       ? "border-red-500 focus:border-red-500 focus:ring-1 focus:ring-red-500"
                                       : "border-zinc-200 focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
                                 }`}
                              />
                           </div>
                           {errors.guestName && (
                              <p className="text-xs text-red-500 font-semibold mt-1 flex items-center gap-1 animate-in fade-in duration-200">
                                 <AlertCircle className="h-3.5 w-3.5 shrink-0" />
                                 <span>{errors.guestName}</span>
                              </p>
                           )}
                        </div>

                        <div className="space-y-1.5">
                           <label className="text-xs font-bold text-zinc-500 uppercase tracking-wider">
                              {t("emailAddress")} *
                           </label>
                           <div className="relative">
                              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-zinc-400">
                                 <Mail className="h-4.5 w-4.5" />
                              </span>
                              <input
                                 type="email"
                                 placeholder="name@example.com"
                                 value={guestEmail}
                                 onChange={(e) => {
                                    setGuestEmail(e.target.value);
                                    if (errors.guestEmail) {
                                       setErrors((prev) => ({ ...prev, guestEmail: undefined }));
                                    }
                                 }}
                                 onBlur={handleEmailBlur}
                                 className={`w-full pl-10 pr-4 py-3 border rounded-xl outline-hidden transition-all text-sm font-medium ${
                                    errors.guestEmail
                                       ? "border-red-500 focus:border-red-500 focus:ring-1 focus:ring-red-500"
                                       : "border-zinc-200 focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
                                 }`}
                              />
                           </div>
                           {errors.guestEmail && (
                              <p className="text-xs text-red-500 font-semibold mt-1 flex items-center gap-1 animate-in fade-in duration-200">
                                 <AlertCircle className="h-3.5 w-3.5 shrink-0" />
                                 <span>{errors.guestEmail}</span>
                              </p>
                           )}
                           <p className="text-[10px] text-zinc-400 font-medium">
                              {t("omnibookingWillSendYourBooking")}
                           </p>
                        </div>

                        <div className="space-y-1.5 animate-in fade-in duration-200">
                           <label className="text-xs font-bold text-zinc-500 uppercase tracking-wider block">
                              {t("contactPhoneNumber")} *
                           </label>
                           <div className="flex gap-2 items-center w-full">
                              <div className="w-[155px] shrink-0">
                                 <Select
                                    inputId="countryCode"
                                    name="countryCode"
                                    options={countriesList}
                                    value={selectedCountry}
                                    onChange={(val) => {
                                       if (val) setSelectedCountry(val);
                                    }}
                                    getOptionLabel={(option) =>
                                       `${option.flag} ${option.code} (${option.callingCode})`
                                    }
                                    getOptionValue={(option) => option.code}
                                    className="text-sm font-semibold text-zinc-700"
                                    classNamePrefix="select"
                                    isSearchable={true}
                                    menuPortalTarget={
                                       typeof document !== "undefined" ? document.body : null
                                    }
                                    styles={{
                                       menuPortal: (base) => ({ ...base, zIndex: 9999 }),
                                       control: (base) => ({
                                          ...base,
                                          borderRadius: "0.75rem",
                                          paddingTop: "4.5px",
                                          paddingBottom: "4.5px",
                                          borderColor: "#e4e4e7",
                                          fontSize: "0.875rem",
                                          fontFamily: "inherit",
                                          fontWeight: "500",
                                       }),
                                    }}
                                 />
                              </div>
                              <input
                                 type="tel"
                                 id="guestPhone"
                                 name="tel"
                                 autoComplete="tel"
                                 placeholder={t("enterPhoneNumber")}
                                 value={guestPhone}
                                 onChange={(e) => {
                                    const val = e.target.value.replace(/\D/g, "");

                                    // Automatically detect if a user pasted/autofilled a number containing country code
                                    const matchedCountry = countriesList.find((c) => {
                                       const cleanCallingCode = c.callingCode.replace(/\D/g, "");
                                       return (
                                          val.startsWith(cleanCallingCode) &&
                                          val.length > cleanCallingCode.length
                                       );
                                    });

                                    if (matchedCountry) {
                                       const cleanCallingCode = matchedCountry.callingCode.replace(
                                          /\D/g,
                                          ""
                                       );
                                       setSelectedCountry(matchedCountry);
                                       setGuestPhone(val.slice(cleanCallingCode.length));
                                    } else {
                                       setGuestPhone(val);
                                    }

                                    if (errors.guestPhone) {
                                       setErrors((prev) => ({ ...prev, guestPhone: undefined }));
                                    }
                                 }}
                                 className={`flex-1 px-4 py-3 border rounded-xl outline-hidden transition-all text-sm font-medium ${
                                    errors.guestPhone
                                       ? "border-red-500 focus:border-red-500 focus:ring-1 focus:ring-red-500"
                                       : "border-zinc-200 focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
                                 }`}
                              />
                           </div>
                           {errors.guestPhone && (
                              <p className="text-xs text-red-500 font-semibold mt-1 flex items-center gap-1 animate-in fade-in duration-200">
                                 <AlertCircle className="h-3.5 w-3.5 shrink-0" />
                                 <span>{errors.guestPhone}</span>
                              </p>
                           )}
                        </div>

                        <div className="space-y-1.5">
                           <label className="text-xs font-bold text-zinc-500 uppercase tracking-wider">
                              {t("specialRequests")}
                           </label>
                           <div className="relative">
                              <span className="absolute top-3.5 left-3.5 text-zinc-400">
                                 <MessageSquare className="h-4.5 w-4.5" />
                              </span>
                              <textarea
                                 rows={3}
                                 placeholder={t("smokingRoomLateCheckInEtc")}
                                 value={specialRequests}
                                 onChange={(e) => setSpecialRequests(e.target.value)}
                                 className="w-full pl-10 pr-4 py-3 border border-zinc-200 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-hidden transition-all text-sm font-medium resize-none"
                              />
                           </div>
                        </div>
                     </form>
                  </div>

                  {/* Payment Info Card */}
                  <div className="bg-white rounded-3xl p-6 sm:p-8 border border-zinc-200 shadow-xs space-y-6">
                     <div className="flex items-center gap-3 pb-4 border-b border-zinc-100">
                        <CreditCard className="h-5 w-5 text-blue-600" />
                        <h2 className="text-lg font-bold text-zinc-900">{t("paymentOption")}</h2>
                     </div>
                     {requiresDeposit ? (
                        <div className="space-y-4">
                           <div className="p-4 bg-blue-50 border border-blue-100 rounded-2xl flex items-start gap-3">
                              <CreditCard className="h-5 w-5 text-blue-600 shrink-0 mt-0.5" />
                              <div>
                                 <h4 className="text-sm font-bold text-blue-800">
                                    {t("15DepositRequired")}
                                 </h4>
                                 <div className="text-xs text-blue-700/85 leading-relaxed mt-0.5">
                                    {t("simulatedDepositIntro")}
                                    <span className="font-bold">
                                       <PriceDisplay amount={depositAmount} size="sm" />
                                    </span>
                                    {t("simulatedDepositOutro")}
                                 </div>
                              </div>
                           </div>
                           <div className="p-4 bg-emerald-50/50 border border-emerald-100 rounded-2xl flex items-start gap-3">
                              <CheckCircle className="h-5 w-5 text-emerald-600 shrink-0 mt-0.5" />
                              <div>
                                 <h4 className="text-sm font-bold text-emerald-800">
                                    {t("payRemainderAtTheProperty")}
                                 </h4>
                                 <div className="text-xs text-emerald-700/85 leading-relaxed mt-0.5">
                                    {t("remainingAmountIntro")}
                                    <span className="font-bold">
                                       <PriceDisplay amount={payLaterAmount} size="sm" />
                                    </span>
                                    {t("remainingAmountOutro")}
                                 </div>
                              </div>
                           </div>
                           <div className="pt-4 border-t border-zinc-100">
                              <h3 className="text-xs font-bold text-zinc-500 uppercase tracking-wider mb-3">
                                 {t("selectPaymentMethodForDeposit")}
                              </h3>
                              <div className="grid grid-cols-3 gap-3">
                                 <button
                                    type="button"
                                    onClick={() => setPaymentMethod("visa")}
                                    className={`p-4 border rounded-2xl flex flex-col items-center gap-2 cursor-pointer transition-all ${
                                       paymentMethod === "visa"
                                          ? "border-blue-600 bg-blue-50/50 text-blue-600 ring-2 ring-blue-600/20"
                                          : "border-zinc-200 hover:border-zinc-300 text-zinc-600 hover:bg-zinc-50"
                                    }`}
                                 >
                                    <CreditCard className="h-6 w-6" />
                                    <span className="text-xs font-bold">Visa / Master</span>
                                 </button>

                                 <button
                                    type="button"
                                    onClick={() => setPaymentMethod("momo")}
                                    className={`p-4 border rounded-2xl flex flex-col items-center gap-2 cursor-pointer transition-all ${
                                       paymentMethod === "momo"
                                          ? "border-pink-600 bg-pink-50/30 text-pink-600 ring-2 ring-pink-600/20"
                                          : "border-zinc-200 hover:border-zinc-300 text-zinc-600 hover:bg-zinc-50"
                                    }`}
                                 >
                                    <Wallet className="h-6 w-6" />
                                    <span className="text-xs font-bold">Ví MoMo</span>
                                 </button>

                                 <button
                                    type="button"
                                    onClick={() => setPaymentMethod("banking")}
                                    className={`p-4 border rounded-2xl flex flex-col items-center gap-2 cursor-pointer transition-all ${
                                       paymentMethod === "banking"
                                          ? "border-emerald-600 bg-emerald-50/30 text-emerald-600 ring-2 ring-emerald-600/20"
                                          : "border-zinc-200 hover:border-zinc-300 text-zinc-600 hover:bg-zinc-50"
                                    }`}
                                 >
                                    <Landmark className="h-6 w-6" />
                                    <span className="text-xs font-bold">Banking</span>
                                 </button>
                              </div>
                           </div>
                        </div>
                     ) : (
                        <div className="p-4 bg-emerald-50/50 border border-emerald-100 rounded-2xl flex items-start gap-3">
                           <CheckCircle className="h-5 w-5 text-emerald-600 shrink-0 mt-0.5" />
                           <div>
                              <h4 className="text-sm font-bold text-emerald-800">
                                 {t("payAtTheProperty")}
                              </h4>
                              <p className="text-xs text-emerald-700/85 leading-relaxed mt-0.5">
                                 {t("noUpfrontFeesRequiredSimplyPay")}
                              </p>
                           </div>
                        </div>
                     )}
                  </div>
               </div>

               {/* Right Column: Order Summary */}
               <div className="lg:col-span-5 space-y-6">
                  {/* Property Card */}
                  <div className="bg-white rounded-3xl border border-zinc-200 shadow-xs overflow-hidden">
                     {property.imageUrl && (
                        <div className="h-44 relative bg-zinc-100">
                           <img
                              src={property.imageUrl}
                              alt={property.name}
                              className="w-full h-full object-cover"
                           />
                        </div>
                     )}
                     <div className="p-6 space-y-4">
                        <div>
                           <span className="text-[10px] bg-blue-100 text-blue-800 font-bold px-2 py-0.5 rounded uppercase">
                              {property.propertyType}
                           </span>
                           <h3 className="text-lg font-extrabold text-zinc-950 mt-1.5 leading-tight">
                              {property.name}
                           </h3>
                           <p className="text-xs text-zinc-500 mt-1 font-medium">
                              {property.address}, {property.city}, {property.country}
                           </p>
                        </div>

                        <div className="border-t border-zinc-100 pt-4 space-y-2 text-xs font-semibold text-zinc-700">
                           <div className="flex items-center justify-between">
                              <span className="text-zinc-500">{t("roomType")}</span>
                              <span className="text-zinc-900 font-bold">{roomType.name}</span>
                           </div>
                           <div className="flex items-center justify-between">
                              <span className="text-zinc-500">{t("rooms")}</span>
                              <span className="text-zinc-900 font-bold">{roomsCount}</span>
                           </div>
                           <div className="flex items-center justify-between">
                              <span className="text-zinc-500">{t("nights")}</span>
                              <span className="text-zinc-900 font-bold">
                                 {nights} {t("nights2")}
                              </span>
                           </div>
                        </div>

                        <div className="bg-zinc-50 rounded-2xl p-4 flex gap-4 text-center items-center justify-between text-xs font-semibold">
                           <div className="flex-1">
                              <span className="text-zinc-400 uppercase tracking-wider block text-[9px]">
                                 {t("checkIn")}
                              </span>
                              <span className="text-zinc-900 font-bold text-sm block mt-1">
                                 {format(parseISO(checkin), "eee, dd MMM", { locale: dateLocale })}
                              </span>
                           </div>
                           <div className="w-px h-8 bg-zinc-200"></div>
                           <div className="flex-1">
                              <span className="text-zinc-400 uppercase tracking-wider block text-[9px]">
                                 {t("checkOut")}
                              </span>
                              <span className="text-zinc-900 font-bold text-sm block mt-1">
                                 {format(parseISO(checkout), "eee, dd MMM", { locale: dateLocale })}
                              </span>
                           </div>
                        </div>
                     </div>
                  </div>

                  {/* Price Calculations */}
                  <div className="bg-white rounded-3xl p-6 border border-zinc-200 shadow-xs space-y-4">
                     <h3 className="text-sm font-bold text-zinc-900">{t("pricingDetails")}</h3>

                     <div className="space-y-2 text-xs font-semibold text-zinc-700">
                        <div className="flex justify-between">
                           <span className="text-zinc-500">
                              {t("originalPrice")} ({roomsCount} {t("rooms2")} x {nights}{" "}
                              {t("nights2")})
                           </span>
                           <span className="text-zinc-800">
                              <PriceDisplay amount={basePrice} size="sm" />
                           </span>
                        </div>
                        {appliedCoupon && (
                           <div className="flex justify-between text-emerald-600 font-bold">
                              <span>{t("couponDiscount")}</span>
                              <span>
                                 - <PriceDisplay amount={basePrice - finalPrice} size="sm" />
                              </span>
                           </div>
                        )}
                        <div className="flex justify-between text-zinc-500 font-medium">
                           <span>{t("taxesFees")}</span>
                           <span className="text-emerald-600 font-bold">{t("included")}</span>
                        </div>
                     </div>

                     {requiresDeposit && (
                        <div className="border-t border-zinc-100 pt-4 space-y-2 text-xs font-semibold text-zinc-700">
                           <div className="flex justify-between text-blue-600 font-bold">
                              <span>{t("depositRequired15")}</span>
                              <span>
                                 <PriceDisplay amount={depositAmount} size="sm" />
                              </span>
                           </div>
                           <div className="flex justify-between text-zinc-500 font-medium">
                              <span>{t("payAtTheProperty2")}</span>
                              <span>
                                 <PriceDisplay amount={payLaterAmount} size="sm" />
                              </span>
                           </div>
                        </div>
                     )}

                     <div className="border-t border-zinc-100 pt-4 flex justify-between items-baseline">
                        <span className="text-sm font-bold text-zinc-900">{t("totalPrice")}</span>
                        <div className="text-right">
                           <PriceDisplay
                              amount={finalPrice}
                              size="lg"
                              className="text-[#006ce4] font-extrabold text-2xl"
                           />
                           <span className="text-[10px] text-zinc-400 block font-medium mt-0.5">
                              {t("zeroBookingFees")}
                           </span>
                        </div>
                     </div>

                     <div className="pt-2">
                        <button
                           type="submit"
                           form="booking-form"
                           disabled={submitting}
                           className="w-full bg-[#006ce4] hover:bg-[#0057b7] text-white py-4 rounded-2xl font-bold text-sm transition-all active:scale-[0.98] cursor-pointer flex items-center justify-center gap-2 shadow-md hover:shadow-lg disabled:opacity-50"
                        >
                           {submitting ? (
                              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                           ) : (
                              <>
                                 <span>{t("completeBooking")}</span>
                                 <ArrowRight className="h-4.5 w-4.5" />
                              </>
                           )}
                        </button>
                     </div>
                  </div>
               </div>
            </div>
         </div>

         {/* Simulated Payment Modal */}
         {showPaymentModal && (
            <div className="fixed inset-0 bg-black/40 backdrop-blur-xs flex items-center justify-center z-50 p-4">
               <div className="bg-white rounded-3xl border border-zinc-200/80 shadow-2xl p-6 sm:p-8 max-w-md w-full relative animate-in fade-in zoom-in-95 duration-200">
                  {paying && (
                     <div className="absolute inset-0 bg-white/90 backdrop-blur-xs rounded-3xl flex flex-col items-center justify-center space-y-4 z-20">
                        <div className="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
                        <p className="text-sm font-bold text-zinc-900 animate-pulse">
                           {t("processingSimulatedPayment")}
                        </p>
                     </div>
                  )}
                  <button
                     onClick={() => setShowPaymentModal(false)}
                     disabled={paying}
                     className="absolute top-4 right-4 text-zinc-400 hover:text-zinc-600"
                  >
                     <X className="h-5 w-5" />
                  </button>
                  <div className="flex flex-col space-y-5">
                     <div className="flex items-center gap-3 pb-3 border-b border-zinc-100">
                        <div className="p-2 bg-blue-50 rounded-lg">
                           <CreditCard className="h-5 w-5 text-blue-600" />
                        </div>
                        <div>
                           <h3 className="text-lg font-bold text-zinc-950">
                              {t("simulatedDeposit")}
                           </h3>
                           <p className="text-[10px] text-zinc-400 font-bold uppercase tracking-wider">
                              OmniBooking Payment Sandbox
                           </p>
                        </div>
                     </div>

                     <p className="text-xs text-zinc-500 leading-relaxed">
                        {t("theSystemIsRunningInPaymentSan")}
                     </p>

                     {paymentMethod === "visa" && (
                        <div className="space-y-3 border border-zinc-200/60 rounded-2xl p-4 bg-zinc-50/50">
                           <div className="space-y-1">
                              <label className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider block">
                                 {t("cardNumber")}
                              </label>
                              <input
                                 type="text"
                                 readOnly
                                 value="4111 2222 3333 4444"
                                 className="w-full px-3 py-2 bg-white border border-zinc-200 rounded-xl text-xs font-semibold text-zinc-700 outline-hidden"
                              />
                           </div>
                           <div className="grid grid-cols-2 gap-3">
                              <div className="space-y-1">
                                 <label className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider block">
                                    {t("expiryDate")}
                                 </label>
                                 <input
                                    type="text"
                                    readOnly
                                    value="12/28"
                                    className="w-full px-3 py-2 bg-white border border-zinc-200 rounded-xl text-xs font-semibold text-zinc-700 outline-hidden"
                                 />
                              </div>
                              <div className="space-y-1">
                                 <label className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider block">
                                    CVV
                                 </label>
                                 <input
                                    type="password"
                                    readOnly
                                    value="123"
                                    className="w-full px-3 py-2 bg-white border border-zinc-200 rounded-xl text-xs font-semibold text-zinc-700 outline-hidden"
                                 />
                              </div>
                           </div>
                           <p className="text-[9px] text-zinc-400 font-medium">
                              * {t("demoCardInfoIsPreFilled")}
                           </p>
                        </div>
                     )}

                     {paymentMethod === "momo" && (
                        <div className="flex flex-col items-center justify-center border border-pink-100 rounded-2xl p-5 bg-pink-50/10 space-y-3">
                           <div className="w-36 h-36 bg-white border-2 border-pink-500 p-2 rounded-2xl flex items-center justify-center relative shadow-xs">
                              <svg className="w-full h-full text-zinc-800" viewBox="0 0 100 100">
                                 <rect width="100" height="100" fill="none" />
                                 <rect x="5" y="5" width="25" height="25" fill="currentColor" />
                                 <rect x="9" y="9" width="17" height="17" fill="white" />
                                 <rect x="13" y="13" width="9" height="9" fill="currentColor" />
                                 <rect x="70" y="5" width="25" height="25" fill="currentColor" />
                                 <rect x="74" y="9" width="17" height="17" fill="white" />
                                 <rect x="78" y="13" width="9" height="9" fill="currentColor" />
                                 <rect x="5" y="70" width="25" height="25" fill="currentColor" />
                                 <rect x="9" y="74" width="17" height="17" fill="white" />
                                 <rect x="13" y="78" width="9" height="9" fill="currentColor" />
                                 <rect x="40" y="5" width="10" height="10" fill="currentColor" />
                                 <rect x="55" y="15" width="10" height="5" fill="currentColor" />
                                 <rect x="45" y="30" width="15" height="10" fill="currentColor" />
                                 <rect x="5" y="45" width="10" height="15" fill="currentColor" />
                                 <rect x="20" y="50" width="15" height="10" fill="currentColor" />
                                 <rect x="80" y="45" width="15" height="15" fill="currentColor" />
                                 <rect x="45" y="55" width="10" height="20" fill="currentColor" />
                                 <rect x="75" y="75" width="20" height="10" fill="currentColor" />
                                 <rect x="60" y="80" width="10" height="15" fill="currentColor" />
                                 <rect x="40" y="85" width="15" height="10" fill="currentColor" />
                              </svg>
                              <div className="absolute inset-0 m-auto w-10 h-10 bg-pink-600 rounded-xl flex items-center justify-center text-white text-[10px] font-black shadow-md border-2 border-white">
                                 MoMo
                              </div>
                           </div>
                           <div className="text-center">
                              <p className="text-xs font-bold text-pink-700">
                                 {t("scanSimulatedQrToPay")}
                              </p>
                              <p className="text-[10px] text-zinc-400 mt-1">
                                 {t("openMomoAppAndScanToPay")}
                              </p>
                           </div>
                        </div>
                     )}

                     {paymentMethod === "banking" && (
                        <div className="flex flex-col items-center border border-emerald-100 rounded-2xl p-4 bg-emerald-50/10 space-y-3">
                           <div className="w-32 h-32 bg-white border-2 border-emerald-500 p-2 rounded-2xl flex items-center justify-center relative shadow-xs">
                              <svg className="w-full h-full text-zinc-800" viewBox="0 0 100 100">
                                 <rect width="100" height="100" fill="none" />
                                 <rect x="5" y="5" width="25" height="25" fill="currentColor" />
                                 <rect x="9" y="9" width="17" height="17" fill="white" />
                                 <rect x="13" y="13" width="9" height="9" fill="currentColor" />
                                 <rect x="70" y="5" width="25" height="25" fill="currentColor" />
                                 <rect x="74" y="9" width="17" height="17" fill="white" />
                                 <rect x="78" y="13" width="9" height="9" fill="currentColor" />
                                 <rect x="5" y="70" width="25" height="25" fill="currentColor" />
                                 <rect x="9" y="74" width="17" height="17" fill="white" />
                                 <rect x="13" y="78" width="9" height="9" fill="currentColor" />
                                 <rect x="40" y="5" width="10" height="10" fill="currentColor" />
                                 <rect x="55" y="15" width="10" height="5" fill="currentColor" />
                                 <rect x="45" y="30" width="15" height="10" fill="currentColor" />
                                 <rect x="5" y="45" width="10" height="15" fill="currentColor" />
                                 <rect x="20" y="50" width="15" height="10" fill="currentColor" />
                                 <rect x="80" y="45" width="15" height="15" fill="currentColor" />
                                 <rect x="45" y="55" width="10" height="20" fill="currentColor" />
                                 <rect x="75" y="75" width="20" height="10" fill="currentColor" />
                                 <rect x="60" y="80" width="10" height="15" fill="currentColor" />
                                 <rect x="40" y="85" width="15" height="10" fill="currentColor" />
                              </svg>
                              <div className="absolute inset-0 m-auto w-8 h-8 bg-emerald-600 rounded-full flex items-center justify-center text-white text-[9px] font-black shadow-md border-2 border-white">
                                 VietQR
                              </div>
                           </div>
                           <div className="w-full space-y-1.5 text-xs font-semibold text-zinc-700">
                              <div className="flex justify-between border-b border-zinc-100 pb-1">
                                 <span className="text-zinc-400">{t("bank")}</span>
                                 <span className="text-zinc-800 font-bold">Techcombank</span>
                              </div>
                              <div className="flex justify-between border-b border-zinc-100 pb-1">
                                 <span className="text-zinc-400">{t("accountNo")}</span>
                                 <span className="text-zinc-800 font-bold">1902 8888 8888</span>
                              </div>
                              <div className="flex justify-between border-b border-zinc-100 pb-1">
                                 <span className="text-zinc-400">{t("accountName")}</span>
                                 <span className="text-zinc-800 font-bold">
                                    OMNIBOOKING SANDBOX
                                 </span>
                              </div>
                              <div className="flex justify-between">
                                 <span className="text-zinc-400">{t("depositAmount")}</span>
                                 <span className="text-emerald-600 font-extrabold">
                                    <PriceDisplay amount={depositAmount} size="sm" />
                                 </span>
                              </div>
                           </div>
                        </div>
                     )}

                     <div className="bg-zinc-50 rounded-2xl p-4 space-y-2.5 text-xs font-semibold text-zinc-700 border border-zinc-100">
                        <div className="flex justify-between">
                           <span className="text-zinc-500">{t("bookingTotal")}</span>
                           <span className="text-zinc-900">
                              <PriceDisplay amount={finalPrice} />
                           </span>
                        </div>
                        <div className="flex justify-between text-blue-600 font-bold">
                           <span>{t("depositNow15")}</span>
                           <span>
                              <PriceDisplay amount={depositAmount} />
                           </span>
                        </div>
                        <div className="h-px bg-zinc-200/80 my-1"></div>
                        <div className="flex justify-between text-emerald-600">
                           <span>{t("payAtPropertyOnCheckIn")}</span>
                           <span>
                              <PriceDisplay amount={payLaterAmount} />
                           </span>
                        </div>
                     </div>

                     <div className="space-y-3 pt-2">
                        <button
                           onClick={handleSimulatedPayment}
                           className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3.5 rounded-xl transition-all cursor-pointer shadow-md hover:shadow-lg flex items-center justify-center gap-2"
                        >
                           <span>{t("confirmSimulatedPayment")}</span>
                        </button>
                        <button
                           onClick={() => setShowPaymentModal(false)}
                           className="w-full text-zinc-400 hover:text-zinc-600 font-bold py-2 text-xs transition-colors cursor-pointer"
                        >
                           {t("cancel")}
                        </button>
                     </div>
                  </div>
               </div>
            </div>
         )}
      </div>
   );
}
