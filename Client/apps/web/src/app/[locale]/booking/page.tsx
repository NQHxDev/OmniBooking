"use client";

import { useState, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { useLocale } from "next-intl";
import { differenceInDays, parseISO, format } from "date-fns";
import { vi, enUS } from "date-fns/locale";
import {
   User,
   Mail,
   Phone,
   MessageSquare,
   CreditCard,
   ArrowRight,
   AlertCircle,
   ChevronLeft,
   Sparkles,
   CheckCircle,
   X,
} from "lucide-react";
import { useAuthStore } from "@/store/useAuthStore";
import { useSettingStore } from "@/store/useSettingStore";
import { propertyService, PropertyDetailResponse } from "@/services/propertyService";
import { authService } from "@/lib/api/services/authService";
import { bookingService } from "@/lib/api/services/bookingService";
import PriceDisplay from "@/components/PriceDisplay";

export default function BookingPage() {
   const locale = useLocale();
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
   const [guestPhone, setGuestPhone] = useState("");
   const [specialRequests, setSpecialRequests] = useState("");
   const [appliedCoupon] = useState<{ id: string } | null>(null);

   // UI States
   const [property, setProperty] = useState<PropertyDetailResponse | null>(null);
   const [loading, setLoading] = useState(true);
   const [submitting, setSubmitting] = useState(false);
   const [error, setError] = useState<string | null>(null);
   const [showLoginPrompt, setShowLoginPrompt] = useState(false);

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
            setError(
               locale === "vi"
                  ? "Không thể tải thông tin khách sạn."
                  : "Failed to load property details."
            );
            setLoading(false);
         });
   }, [propertyId, locale]);

   if (loading) {
      return (
         <div className="min-h-screen flex items-center justify-center bg-zinc-50">
            <div className="flex flex-col items-center gap-4">
               <div className="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
               <p className="text-zinc-500 font-medium">
                  {locale === "vi"
                     ? "Đang tải thông tin đặt phòng..."
                     : "Loading booking details..."}
               </p>
            </div>
         </div>
      );
   }

   if (error || !property || !roomTypeId || !checkin || !checkout) {
      return (
         <div className="min-h-screen flex items-center justify-center bg-zinc-50">
            <div className="bg-white p-8 rounded-2xl border border-zinc-200 shadow-sm max-w-md text-center space-y-4">
               <AlertCircle className="h-12 w-12 text-red-500 mx-auto" />
               <h2 className="text-xl font-bold text-zinc-900">
                  {locale === "vi" ? "Thông tin không hợp lệ" : "Invalid Information"}
               </h2>
               <p className="text-zinc-500 text-sm">
                  {error ||
                     (locale === "vi"
                        ? "Thiếu thông tin đặt phòng cần thiết."
                        : "Missing required booking details.")}
               </p>
               <button
                  onClick={() => router.back()}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-xl font-bold text-sm transition-colors cursor-pointer"
               >
                  {locale === "vi" ? "Quay lại" : "Go Back"}
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
               <h2 className="text-xl font-bold text-zinc-900">
                  {locale === "vi" ? "Loại phòng không tồn tại" : "Room Type Not Found"}
               </h2>
               <button
                  onClick={() => router.back()}
                  className="bg-blue-600 text-white px-6 py-2 rounded-xl"
               >
                  {locale === "vi" ? "Quay lại" : "Go Back"}
               </button>
            </div>
         </div>
      );
   }

   const nights = Math.max(1, differenceInDays(parseISO(checkout), parseISO(checkin)));
   const basePrice = roomType.basePrice * nights * roomsCount;
   const finalPrice = basePrice; // In real flow, handle coupon discount on backend response

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

   const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      setSubmitting(true);
      setError(null);

      try {
         const response = await bookingService.create({
            roomTypeId,
            checkInDate: checkin,
            checkOutDate: checkout,
            numRooms: roomsCount,
            guestName,
            guestEmail,
            guestPhone: guestPhone || undefined,
            specialRequests: specialRequests || undefined,
            couponId: appliedCoupon?.id || undefined,
            currency,
         });

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
            (response.activationToken ? `&token=${response.activationToken}` : "");

         router.push(successUrl);
      } catch (err) {
         console.error(err);
         const errorWithResponse = err as { response?: { data?: { message?: string } } };
         setError(
            errorWithResponse.response?.data?.message ||
               (locale === "vi"
                  ? "Đặt phòng thất bại. Vui lòng thử lại."
                  : "Booking failed. Please try again.")
         );
         setSubmitting(false);
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
                     <h3 className="text-xl font-bold text-zinc-900">
                        {locale === "vi" ? "Nhận Ưu Đãi Genius!" : "Get Genius Discounts!"}
                     </h3>
                     <p className="text-sm text-zinc-500 leading-relaxed">
                        {locale === "vi"
                           ? "Email này đã có tài khoản OmniBooking. Hãy đăng nhập để tự động điền thông tin và áp dụng các ưu đãi thành viên đặc biệt!"
                           : "This email is already registered. Log in now to autofill details and enjoy Genius member discounts!"}
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
                           {locale === "vi" ? "Đăng nhập ngay" : "Log In Now"}
                        </button>
                        <button
                           onClick={() => setShowLoginPrompt(false)}
                           className="text-zinc-500 hover:text-zinc-700 font-bold py-2.5 text-xs transition-colors cursor-pointer"
                        >
                           {locale === "vi" ? "Tiếp tục đặt phòng ẩn danh" : "Continue as Guest"}
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
                  {locale === "vi" ? "Yêu cầu đặt phòng của bạn" : "Confirm and Pay"}
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
                        <h2 className="text-lg font-bold text-zinc-900">
                           {locale === "vi" ? "Thông tin liên hệ" : "Contact details"}
                        </h2>
                     </div>

                     <form onSubmit={handleSubmit} id="booking-form" className="space-y-5">
                        <div className="space-y-1.5">
                           <label className="text-xs font-bold text-zinc-500 uppercase tracking-wider">
                              {locale === "vi" ? "Họ và tên" : "Full Name"} *
                           </label>
                           <div className="relative">
                              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-zinc-400">
                                 <User className="h-4.5 w-4.5" />
                              </span>
                              <input
                                 type="text"
                                 required
                                 placeholder={locale === "vi" ? "Nhập họ tên đầy đủ" : "John Doe"}
                                 value={guestName}
                                 onChange={(e) => setGuestName(e.target.value)}
                                 className="w-full pl-10 pr-4 py-3 border border-zinc-200 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-hidden transition-all text-sm font-medium"
                              />
                           </div>
                        </div>

                        <div className="space-y-1.5">
                           <label className="text-xs font-bold text-zinc-500 uppercase tracking-wider">
                              {locale === "vi" ? "Địa chỉ Email" : "Email Address"} *
                           </label>
                           <div className="relative">
                              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-zinc-400">
                                 <Mail className="h-4.5 w-4.5" />
                              </span>
                              <input
                                 type="email"
                                 required
                                 placeholder="name@example.com"
                                 value={guestEmail}
                                 onChange={(e) => setGuestEmail(e.target.value)}
                                 onBlur={handleEmailBlur}
                                 className="w-full pl-10 pr-4 py-3 border border-zinc-200 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-hidden transition-all text-sm font-medium"
                              />
                           </div>
                           <p className="text-[10px] text-zinc-400 font-medium">
                              {locale === "vi"
                                 ? "OmniBooking sẽ gửi xác nhận đặt phòng và link kích hoạt tài khoản thành viên tới email này."
                                 : "OmniBooking will send your booking confirmation and guest activation details here."}
                           </p>
                        </div>

                        <div className="space-y-1.5">
                           <label className="text-xs font-bold text-zinc-500 uppercase tracking-wider">
                              {locale === "vi" ? "Số điện thoại" : "Phone Number"}
                           </label>
                           <div className="relative">
                              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-zinc-400">
                                 <Phone className="h-4.5 w-4.5" />
                              </span>
                              <input
                                 type="tel"
                                 placeholder="e.g. +84 123 456 789"
                                 value={guestPhone}
                                 onChange={(e) => setGuestPhone(e.target.value)}
                                 className="w-full pl-10 pr-4 py-3 border border-zinc-200 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-hidden transition-all text-sm font-medium"
                              />
                           </div>
                        </div>

                        <div className="space-y-1.5">
                           <label className="text-xs font-bold text-zinc-500 uppercase tracking-wider">
                              {locale === "vi" ? "Yêu cầu đặc biệt" : "Special Requests"}
                           </label>
                           <div className="relative">
                              <span className="absolute top-3.5 left-3.5 text-zinc-400">
                                 <MessageSquare className="h-4.5 w-4.5" />
                              </span>
                              <textarea
                                 rows={3}
                                 placeholder={
                                    locale === "vi"
                                       ? "Nhập yêu cầu đặc biệt của bạn (nếu có)"
                                       : "Smoking room, late check-in, etc."
                                 }
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
                        <h2 className="text-lg font-bold text-zinc-900">
                           {locale === "vi" ? "Hình thức thanh toán" : "Payment option"}
                        </h2>
                     </div>
                     <div className="p-4 bg-emerald-50/50 border border-emerald-100 rounded-2xl flex items-start gap-3">
                        <CheckCircle className="h-5 w-5 text-emerald-600 shrink-0 mt-0.5" />
                        <div>
                           <h4 className="text-sm font-bold text-emerald-800">
                              {locale === "vi"
                                 ? "Thanh toán trực tiếp tại chỗ nghỉ"
                                 : "Pay at the property"}
                           </h4>
                           <p className="text-xs text-emerald-700/85 leading-relaxed mt-0.5">
                              {locale === "vi"
                                 ? "Nhận phòng thành công mới cần trả tiền! OmniBooking không yêu cầu bạn trả trước bất kỳ khoản phí nào."
                                 : "No upfront fees required! Simply pay at the hotel desk during check-in."}
                           </p>
                        </div>
                     </div>
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
                              <span className="text-zinc-500">
                                 {locale === "vi" ? "Loại phòng" : "Room type"}
                              </span>
                              <span className="text-zinc-900 font-bold">{roomType.name}</span>
                           </div>
                           <div className="flex items-center justify-between">
                              <span className="text-zinc-500">
                                 {locale === "vi" ? "Số lượng phòng" : "Rooms"}
                              </span>
                              <span className="text-zinc-900 font-bold">{roomsCount}</span>
                           </div>
                           <div className="flex items-center justify-between">
                              <span className="text-zinc-500">
                                 {locale === "vi" ? "Số đêm lưu trú" : "Nights"}
                              </span>
                              <span className="text-zinc-900 font-bold">
                                 {nights} {locale === "vi" ? "đêm" : "nights"}
                              </span>
                           </div>
                        </div>

                        <div className="bg-zinc-50 rounded-2xl p-4 flex gap-4 text-center items-center justify-between text-xs font-semibold">
                           <div className="flex-1">
                              <span className="text-zinc-400 uppercase tracking-wider block text-[9px]">
                                 {locale === "vi" ? "Nhận phòng" : "Check-in"}
                              </span>
                              <span className="text-zinc-900 font-bold text-sm block mt-1">
                                 {format(parseISO(checkin), "eee, dd MMM", { locale: dateLocale })}
                              </span>
                           </div>
                           <div className="w-px h-8 bg-zinc-200"></div>
                           <div className="flex-1">
                              <span className="text-zinc-400 uppercase tracking-wider block text-[9px]">
                                 {locale === "vi" ? "Trả phòng" : "Check-out"}
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
                     <h3 className="text-sm font-bold text-zinc-900">
                        {locale === "vi" ? "Chi tiết giá phòng" : "Pricing details"}
                     </h3>

                     <div className="space-y-2 text-xs font-semibold text-zinc-700">
                        <div className="flex justify-between">
                           <span className="text-zinc-500">
                              {locale === "vi" ? "Giá gốc" : "Original price"} ({roomsCount}{" "}
                              {locale === "vi" ? "phòng" : "rooms"} x {nights}{" "}
                              {locale === "vi" ? "đêm" : "nights"})
                           </span>
                           <span className="text-zinc-800">
                              <PriceDisplay amount={basePrice} size="sm" />
                           </span>
                        </div>
                        {appliedCoupon && (
                           <div className="flex justify-between text-emerald-600 font-bold">
                              <span>{locale === "vi" ? "Mã giảm giá" : "Coupon Discount"}</span>
                              <span>
                                 - <PriceDisplay amount={basePrice - finalPrice} size="sm" />
                              </span>
                           </div>
                        )}
                        <div className="flex justify-between text-zinc-500 font-medium">
                           <span>{locale === "vi" ? "Thuế & phí dịch vụ" : "Taxes & fees"}</span>
                           <span className="text-emerald-600 font-bold">
                              {locale === "vi" ? "Đã bao gồm" : "Included"}
                           </span>
                        </div>
                     </div>

                     <div className="border-t border-zinc-100 pt-4 flex justify-between items-baseline">
                        <span className="text-sm font-bold text-zinc-900">
                           {locale === "vi" ? "Tổng cộng" : "Total Price"}
                        </span>
                        <div className="text-right">
                           <PriceDisplay
                              amount={finalPrice}
                              size="lg"
                              className="text-[#006ce4] font-extrabold text-2xl"
                           />
                           <span className="text-[10px] text-zinc-400 block font-medium mt-0.5">
                              {locale === "vi" ? "Không mất phí đặt trước" : "Zero booking fees"}
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
                                 <span>
                                    {locale === "vi" ? "Hoàn tất đặt phòng" : "Complete Booking"}
                                 </span>
                                 <ArrowRight className="h-4.5 w-4.5" />
                              </>
                           )}
                        </button>
                     </div>
                  </div>
               </div>
            </div>
         </div>
      </div>
   );
}
