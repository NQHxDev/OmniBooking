"use client";

import { useState, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { useLocale } from "next-intl";
import { format, parseISO } from "date-fns";
import { vi, enUS } from "date-fns/locale";
import { CheckCircle, Home, Lock, ArrowRight, AlertCircle, Sparkles } from "lucide-react";
import { useAuthStore } from "@/store/useAuthStore";
import { useSettingStore } from "@/store/useSettingStore";
import { authService } from "@/lib/api/services/authService";
import { bookingService, BookingResponse } from "@/lib/api/services/bookingService";
import { paymentService } from "@/lib/api/services/paymentService";
import PriceDisplay from "@/components/PriceDisplay";
import Barcode from "@/components/Barcode";

export default function BookingSuccessPage() {
   const locale = useLocale();
   const router = useRouter();
   const searchParams = useSearchParams();
   const dateLocale = locale === "vi" ? vi : enUS;

   const rawBookingId = searchParams.get("bookingId") || searchParams.get("orderId");
   const bookingId =
      rawBookingId && rawBookingId.includes("_") ? rawBookingId.split("_")[0] : rawBookingId;
   const [bookingDetails, setBookingDetails] = useState<BookingResponse | null>(null);
   const [loading, setLoading] = useState(!!bookingId);
   const [fetchError, setFetchError] = useState<string | null>(null);
   const [retrying, setRetrying] = useState(false);

   const resultCode = searchParams.get("resultCode");
   const isPaymentSuccess = resultCode === null || resultCode === "0";

   const [prevBookingId, setPrevBookingId] = useState(bookingId);
   if (bookingId !== prevBookingId) {
      setPrevBookingId(bookingId);
      setLoading(true);
      setBookingDetails(null);
      setFetchError(null);
   }

   useEffect(() => {
      if (bookingId) {
         bookingService
            .getById(bookingId)
            .then((details) => {
               setBookingDetails(details);
            })
            .catch((err) => {
               console.error("Failed to fetch booking details:", err);
               setFetchError(
                  locale === "vi"
                     ? "Không thể tải thông tin đặt phòng."
                     : "Failed to load booking details."
               );
            })
            .finally(() => {
               setLoading(false);
            });
      }
   }, [bookingId, locale]);

   const handleRetryPayment = async () => {
      if (!bookingId) return;
      setRetrying(true);
      try {
         const payData = await paymentService.createMomoPayment(bookingId);
         if (payData && payData.payUrl) {
            window.location.href = payData.payUrl;
         } else {
            alert(
               locale === "vi"
                  ? "Không thể tạo liên kết thanh toán MoMo."
                  : "Failed to create MoMo payment link."
            );
         }
      } catch (err) {
         console.error("Retry payment error:", err);
         alert(
            locale === "vi"
               ? "Có lỗi xảy ra khi tạo liên kết thanh toán."
               : "An error occurred while creating the payment link."
         );
      } finally {
         setRetrying(false);
      }
   };

   const bookingCode = bookingDetails
      ? bookingDetails.bookingCode
      : searchParams.get("code") || "N/A";
   const guestName = bookingDetails ? bookingDetails.guestName : searchParams.get("name") || "";
   const propertyName = bookingDetails
      ? bookingDetails.propertyName
      : searchParams.get("property") || "";
   const roomTypeName = bookingDetails
      ? bookingDetails.roomTypeName
      : searchParams.get("room") || "";
   const checkin = bookingDetails ? bookingDetails.checkInDate : searchParams.get("checkin");
   const checkout = bookingDetails ? bookingDetails.checkOutDate : searchParams.get("checkout");
   const finalPrice = bookingDetails
      ? bookingDetails.finalPrice || 0
      : Number(searchParams.get("final")) || 0;
   const depositAmount = bookingDetails
      ? bookingDetails.depositAmount || 0
      : Number(searchParams.get("deposit")) || 0;
   const requiresDeposit = bookingDetails
      ? bookingDetails.requiresDeposit || false
      : searchParams.get("requiresDeposit") === "true";
   const payLaterAmount = finalPrice - depositAmount;
   const paymentMethod = bookingDetails
      ? bookingDetails.paymentMethod || "visa"
      : searchParams.get("paymentMethod") || "visa";
   const token = bookingDetails
      ? bookingDetails.activationToken || null
      : searchParams.get("token"); // Guest activation token

   const { setAuth } = useAuthStore();
   const { currency } = useSettingStore();

   // Form states
   const [password, setPassword] = useState("");
   const [confirmPassword, setConfirmPassword] = useState("");
   const [activating, setActivating] = useState(false);
   const [activationError, setActivationError] = useState<string | null>(null);
   const [activatedSuccessfully, setActivatedSuccessfully] = useState(false);

   const handleActivation = async (e: React.FormEvent) => {
      e.preventDefault();
      if (!token) return;

      if (password !== confirmPassword) {
         setActivationError(
            locale === "vi" ? "Mật khẩu xác nhận không khớp." : "Passwords do not match."
         );
         return;
      }

      if (password.length < 6) {
         setActivationError(
            locale === "vi"
               ? "Mật khẩu phải chứa ít nhất 6 ký tự."
               : "Password must be at least 6 characters."
         );
         return;
      }

      setActivating(true);
      setActivationError(null);

      try {
         const user = await authService.activateGuest(token, password);
         setAuth(user);
         setActivatedSuccessfully(true);
         setTimeout(() => {
            router.push(`/${locale}/profile`);
         }, 2000);
      } catch (err) {
         console.error(err);
         const errorWithResponse = err as { response?: { data?: { message?: string } } };
         setActivationError(
            errorWithResponse.response?.data?.message ||
               (locale === "vi"
                  ? "Kích hoạt tài khoản thất bại. Vui lòng thử lại."
                  : "Activation failed. Please try again.")
         );
         setActivating(false);
      }
   };

   if (loading) {
      return (
         <div className="min-h-screen bg-zinc-50/50 py-12 px-4 sm:px-6 lg:px-8 font-sans flex flex-col justify-center items-center">
            <div className="max-w-xl w-full space-y-8 flex flex-col items-center">
               <div className="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
               <p className="text-sm font-semibold text-zinc-500">
                  {locale === "vi"
                     ? "Đang tải thông tin đặt phòng..."
                     : "Loading booking details..."}
               </p>
            </div>
         </div>
      );
   }

   if (fetchError) {
      return (
         <div className="min-h-screen bg-zinc-50/50 py-12 px-4 sm:px-6 lg:px-8 font-sans flex flex-col justify-center items-center">
            <div className="max-w-md w-full bg-white rounded-3xl border border-zinc-200 shadow-sm p-8 text-center space-y-6">
               <div className="inline-flex p-3 bg-red-50 rounded-full border border-red-100 shadow-sm">
                  <AlertCircle className="h-10 w-10 text-red-600" />
               </div>
               <h1 className="text-xl font-bold text-zinc-950">
                  {locale === "vi" ? "Lỗi tải thông tin" : "Error Loading Details"}
               </h1>
               <p className="text-sm text-zinc-500 font-medium">{fetchError}</p>
               <button
                  onClick={() => router.push(`/${locale}`)}
                  className="w-full bg-zinc-900 hover:bg-zinc-800 text-white font-bold py-3 px-6 rounded-xl transition-all cursor-pointer shadow-md text-xs flex items-center justify-center gap-2"
               >
                  <Home className="h-4 w-4" />
                  <span>{locale === "vi" ? "Về trang chủ" : "Return Home"}</span>
               </button>
            </div>
         </div>
      );
   }

   return (
      <div className="min-h-screen bg-zinc-50/50 py-12 px-4 sm:px-6 lg:px-8 font-sans flex flex-col justify-center items-center">
         <div className="max-w-xl w-full space-y-8">
            {/* Header Success Status */}
            {isPaymentSuccess ? (
               <div className="text-center space-y-4 animate-in fade-in duration-300">
                  <div className="inline-flex p-3 bg-emerald-50 rounded-full border border-emerald-100 shadow-sm">
                     <CheckCircle className="h-14 w-14 text-emerald-600" />
                  </div>
                  <h1 className="text-3xl font-bold text-zinc-950 tracking-tight">
                     {locale === "vi" ? "Đặt phòng thành công!" : "Booking Confirmed!"}
                  </h1>
                  <p className="text-sm text-zinc-500 font-medium max-w-sm mx-auto">
                     {locale === "vi"
                        ? "Cảm ơn bạn đã lựa chọn OmniBooking. Chúng tôi đã gửi email xác nhận chi tiết tới địa chỉ email của bạn."
                        : "Thank you for booking with OmniBooking. We've sent a confirmation email to your address."}
                  </p>
               </div>
            ) : (
               <div className="text-center space-y-4 animate-in fade-in duration-300">
                  <div className="inline-flex p-3 bg-rose-50 rounded-full border border-rose-100 shadow-sm">
                     <AlertCircle className="h-14 w-14 text-rose-600" />
                  </div>
                  <h1 className="text-3xl font-bold text-zinc-950 tracking-tight">
                     {locale === "vi" ? "Thanh toán thất bại!" : "Payment Unsuccessful!"}
                  </h1>
                  <p className="text-sm text-zinc-500 font-medium max-w-sm mx-auto">
                     {locale === "vi"
                        ? "Giao dịch thanh toán qua MoMo đã bị hủy hoặc không thành công. Yêu cầu đặt phòng của bạn tạm thời chưa được xác nhận."
                        : "The MoMo payment transaction was cancelled or failed. Your booking has not been confirmed yet."}
                  </p>
               </div>
            )}

            {/* Booking Ticket Card (Boarding Pass Style) */}
            <div className="relative bg-white rounded-3xl border border-zinc-200 shadow-sm overflow-visible">
               {/* Left and Right punch-out circles (airline ticket style) */}
               <div className="absolute left-[-12px] top-[50%] -translate-y-1/2 w-6 h-6 rounded-full bg-zinc-50 border-r border-zinc-200 z-10 hidden sm:block"></div>
               <div className="absolute right-[-12px] top-[50%] -translate-y-1/2 w-6 h-6 rounded-full bg-zinc-50 border-l border-zinc-200 z-10 hidden sm:block"></div>

               {/* Top half: Hotel and Guest info */}
               <div className="p-6 sm:p-8 space-y-6">
                  <div className="flex justify-between items-center pb-4 border-b border-zinc-100">
                     <div className="flex items-center gap-2">
                        {isPaymentSuccess ? (
                           <div className="bg-emerald-50 text-emerald-700 font-bold text-[10px] px-2.5 py-1 rounded-full uppercase tracking-wider border border-emerald-100">
                              {locale === "vi" ? "Đã xác nhận" : "Confirmed"}
                           </div>
                        ) : (
                           <div className="bg-rose-50 text-rose-700 font-bold text-[10px] px-2.5 py-1 rounded-full uppercase tracking-wider border border-rose-100">
                              {locale === "vi" ? "Chưa thanh toán" : "Unpaid"}
                           </div>
                        )}
                     </div>
                     <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-widest">
                        OmniBooking Boarding Pass
                     </span>
                  </div>

                  <div className="space-y-4 text-xs font-semibold text-zinc-700">
                     <div className="grid grid-cols-2 gap-4">
                        <div>
                           <span className="text-zinc-400 block text-[9px] uppercase tracking-wider font-bold">
                              {locale === "vi" ? "Khách hàng" : "Passenger"}
                           </span>
                           <span className="text-zinc-900 font-bold text-sm block mt-0.5 truncate">
                              {guestName || "Guest User"}
                           </span>
                        </div>
                        <div>
                           <span className="text-zinc-400 block text-[9px] uppercase tracking-wider font-bold">
                              {locale === "vi" ? "Mã đặt phòng" : "Booking Code"}
                           </span>
                           <span className="text-blue-600 font-black text-sm block mt-0.5 tracking-wider">
                              {bookingCode}
                           </span>
                        </div>
                     </div>

                     <div className="grid grid-cols-2 gap-4 border-t border-zinc-100 pt-4">
                        <div>
                           <span className="text-zinc-400 block text-[9px] uppercase tracking-wider font-bold">
                              {locale === "vi" ? "Chỗ nghỉ / Khách sạn" : "Property / Hotel"}
                           </span>
                           <span className="text-zinc-900 font-bold text-sm block mt-0.5 truncate">
                              {propertyName}
                           </span>
                        </div>
                        <div>
                           <span className="text-zinc-400 block text-[9px] uppercase tracking-wider font-bold">
                              {locale === "vi" ? "Loại phòng" : "Room Type"}
                           </span>
                           <span className="text-zinc-900 font-bold block mt-0.5 truncate">
                              {roomTypeName}
                           </span>
                        </div>
                     </div>
                  </div>
               </div>

               {/* Dashed line dividing top and bottom */}
               <div className="relative flex items-center justify-between px-6 sm:px-8">
                  <div className="w-full border-t-2 border-dashed border-zinc-100"></div>
               </div>

               {/* Bottom half: Check-in / Check-out and Barcode */}
               <div className="p-6 sm:p-8 bg-zinc-50/20 rounded-b-3xl space-y-6">
                  <div className="grid grid-cols-2 gap-4 text-xs font-semibold text-zinc-700">
                     <div>
                        <span className="text-zinc-400 block text-[9px] uppercase tracking-wider font-bold">
                           {locale === "vi" ? "Ngày nhận phòng" : "Check-in Date"}
                        </span>
                        <span className="text-zinc-900 font-bold block mt-0.5">
                           {checkin
                              ? format(parseISO(checkin), "eee, dd MMM yyyy", {
                                   locale: dateLocale,
                                })
                              : "N/A"}
                        </span>
                        <span className="text-[10px] text-zinc-400 block mt-0.5 font-medium">
                           14:00 PM
                        </span>
                     </div>
                     <div>
                        <span className="text-zinc-400 block text-[9px] uppercase tracking-wider font-bold">
                           {locale === "vi" ? "Ngày trả phòng" : "Check-out Date"}
                        </span>
                        <span className="text-zinc-900 font-bold block mt-0.5">
                           {checkout
                              ? format(parseISO(checkout), "eee, dd MMM yyyy", {
                                   locale: dateLocale,
                                })
                              : "N/A"}
                        </span>
                        <span className="text-[10px] text-zinc-400 block mt-0.5 font-medium">
                           12:00 PM
                        </span>
                     </div>
                  </div>

                  {requiresDeposit ? (
                     <div className="border-t border-zinc-100 pt-4 space-y-3">
                        <div className="flex justify-between items-baseline text-xs font-semibold text-zinc-500">
                           <span>
                              {locale === "vi" ? "Tổng cộng giá trị đơn:" : "Total booking value:"}
                           </span>
                           <span>
                              <PriceDisplay
                                 amount={finalPrice}
                                 size="sm"
                                 className="text-zinc-700 font-bold"
                              />
                           </span>
                        </div>
                        <div className="flex justify-between items-baseline text-xs font-semibold text-blue-600">
                           <span>
                              {locale === "vi"
                                 ? `Đã đặt cọc trực tuyến qua ${
                                      paymentMethod.toLowerCase() === "momo"
                                         ? "Ví MoMo"
                                         : paymentMethod.toLowerCase() === "banking"
                                           ? "Chuyển khoản (giả lập)"
                                           : "Thẻ Visa/Master (qua MoMo)"
                                   }:`
                                 : `Deposit paid via ${
                                      paymentMethod.toLowerCase() === "momo"
                                         ? "MoMo"
                                         : paymentMethod.toLowerCase() === "banking"
                                           ? "Bank Transfer (simulated)"
                                           : "Visa/Mastercard (via MoMo)"
                                   }:`}
                           </span>
                           <span>
                              <PriceDisplay
                                 amount={depositAmount}
                                 size="sm"
                                 className="font-black text-blue-600"
                              />
                           </span>
                        </div>
                        <div className="flex justify-between items-baseline border-t border-zinc-100/50 pt-3">
                           <span className="text-xs font-bold text-zinc-900">
                              {locale === "vi"
                                 ? "Còn lại thanh toán tại chỗ nghỉ"
                                 : "Remaining to pay at property"}
                           </span>
                           <div className="text-right">
                              <PriceDisplay
                                 amount={payLaterAmount}
                                 size="lg"
                                 className="text-emerald-600 font-extrabold text-lg block"
                              />
                              {currency === "VND" && (
                                 <span className="text-[10px] text-zinc-400 font-bold block mt-0.5">
                                    (${payLaterAmount.toFixed(2)} USD)
                                 </span>
                              )}
                           </div>
                        </div>
                     </div>
                  ) : (
                     <div className="border-t border-zinc-100 pt-4 flex justify-between items-baseline">
                        <span className="text-xs font-bold text-zinc-900">
                           {locale === "vi"
                              ? "Tổng thanh toán tại chỗ nghỉ"
                              : "Total to pay at property"}
                        </span>
                        <div className="text-right">
                           <PriceDisplay
                              amount={finalPrice}
                              size="lg"
                              className="text-emerald-600 font-extrabold text-lg block"
                           />
                           {currency === "VND" && (
                              <span className="text-[10px] text-zinc-400 font-bold block mt-0.5">
                                 (${finalPrice.toFixed(2)} USD)
                              </span>
                           )}
                        </div>
                     </div>
                  )}

                  {/* Scannable Barcode */}
                  <div className="border-t border-zinc-100 pt-6 flex flex-col items-center justify-center space-y-2">
                     <div className="bg-white p-4 rounded-2xl border border-zinc-200/60 shadow-xs flex flex-col items-center max-w-[280px] w-full">
                        <Barcode
                           value={bookingCode}
                           height={45}
                           width={1.6}
                           fontSize={11}
                           displayValue={true}
                           background="#ffffff"
                        />
                     </div>
                     <span className="text-[10px] text-zinc-400 font-bold uppercase tracking-wider">
                        {locale === "vi"
                           ? "Quét mã này tại quầy lễ tân"
                           : "Scan barcode at reception"}
                     </span>
                  </div>
               </div>
            </div>

            {/* Lazy Sign-up Box */}
            {isPaymentSuccess && token && !activatedSuccessfully && (
               <div className="bg-blue-50/70 rounded-3xl border border-blue-100 p-6 sm:p-8 space-y-6 relative overflow-hidden shadow-xs">
                  <div className="absolute -top-6 -right-6 w-20 h-20 bg-blue-100 rounded-full opacity-50 blur-xl"></div>

                  <div className="space-y-2">
                     <div className="inline-flex items-center gap-1.5 bg-blue-100 text-blue-800 text-[10px] font-bold px-2.5 py-0.5 rounded-full">
                        <Sparkles className="h-3.5 w-3.5" />
                        <span>Genius Member Club</span>
                     </div>
                     <h3 className="text-lg font-extrabold text-zinc-900">
                        {locale === "vi"
                           ? "Kích hoạt tài khoản thành viên"
                           : "Activate Your Membership"}
                     </h3>
                     <p className="text-xs text-zinc-500 leading-relaxed font-medium">
                        {locale === "vi"
                           ? "Chúng tôi đã tạo tài khoản tạm thời liên kết với email của bạn. Chỉ cần đặt mật khẩu dưới đây để kích hoạt thành viên, mở khóa ưu đãi Genius giảm tới 20% cho lần sau!"
                           : "We created a temporary account for you. Set your password below to activate and unlock up to 20% off future bookings!"}
                     </p>
                  </div>

                  {activationError && (
                     <div className="bg-red-50 border border-red-200 rounded-xl p-3 flex items-center gap-2 text-red-700 text-xs font-semibold animate-in fade-in duration-200">
                        <AlertCircle className="h-4.5 w-4.5 text-red-500 shrink-0" />
                        <span>{activationError}</span>
                     </div>
                  )}

                  <form onSubmit={handleActivation} className="space-y-4">
                     <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                        <div className="space-y-1">
                           <label className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider block">
                              {locale === "vi" ? "Mật khẩu" : "Password"}
                           </label>
                           <div className="relative">
                              <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-400">
                                 <Lock className="h-4 w-4" />
                              </span>
                              <input
                                 type="password"
                                 required
                                 placeholder="******"
                                 value={password}
                                 onChange={(e) => setPassword(e.target.value)}
                                 className="w-full pl-8.5 pr-3 py-2.5 bg-white border border-zinc-200 rounded-xl focus:border-blue-500 outline-hidden text-xs font-bold"
                              />
                           </div>
                        </div>

                        <div className="space-y-1">
                           <label className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider block">
                              {locale === "vi" ? "Xác nhận mật khẩu" : "Confirm Password"}
                           </label>
                           <div className="relative">
                              <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-400">
                                 <Lock className="h-4 w-4" />
                              </span>
                              <input
                                 type="password"
                                 required
                                 placeholder="******"
                                 value={confirmPassword}
                                 onChange={(e) => setConfirmPassword(e.target.value)}
                                 className="w-full pl-8.5 pr-3 py-2.5 bg-white border border-zinc-200 rounded-xl focus:border-blue-500 outline-hidden text-xs font-bold"
                              />
                           </div>
                        </div>
                     </div>

                     <button
                        type="submit"
                        disabled={activating}
                        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-6 rounded-xl transition-all cursor-pointer shadow-md text-xs flex items-center justify-center gap-2 hover:shadow-lg disabled:opacity-50"
                     >
                        {activating ? (
                           <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                        ) : (
                           <>
                              <span>
                                 {locale === "vi"
                                    ? "Kích hoạt & Đăng nhập ngay"
                                    : "Activate & Log In Now"}
                              </span>
                              <ArrowRight className="h-3.5 w-3.5" />
                           </>
                        )}
                     </button>
                  </form>
               </div>
            )}

            {activatedSuccessfully && (
               <div className="bg-emerald-50 border border-emerald-200 rounded-3xl p-6 text-center space-y-3 animate-in fade-in duration-300">
                  <CheckCircle className="h-10 w-10 text-emerald-600 mx-auto" />
                  <h3 className="text-base font-bold text-emerald-950">
                     {locale === "vi"
                        ? "Kích hoạt tài khoản thành viên thành công!"
                        : "Membership Activated Successfully!"}
                  </h3>
                  <p className="text-xs text-emerald-800/80 leading-relaxed font-semibold">
                     {locale === "vi"
                        ? "Hệ thống đang tự động đăng nhập và đưa bạn tới trang hồ sơ cá nhân..."
                        : "Logging you in and redirecting to your profile dashboard..."}
                  </p>
               </div>
            )}

            {/* Actions */}
            <div className="text-center pt-4">
               {isPaymentSuccess ? (
                  <button
                     onClick={() => router.push(`/${locale}`)}
                     className="inline-flex items-center gap-2 text-zinc-500 hover:text-zinc-800 text-sm font-bold border border-zinc-200 bg-white rounded-xl px-6 py-3 cursor-pointer transition-colors shadow-2xs hover:shadow-xs"
                  >
                     <Home className="h-4.5 w-4.5" />
                     <span>{locale === "vi" ? "Về trang chủ" : "Return Home"}</span>
                  </button>
               ) : (
                  <div className="flex flex-col sm:flex-row gap-3 justify-center items-center">
                     <button
                        onClick={handleRetryPayment}
                        disabled={retrying}
                        className="inline-flex items-center justify-center gap-2 text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-sm font-bold rounded-xl px-6 py-3 cursor-pointer transition-all shadow-md hover:shadow-lg w-full sm:w-auto min-w-[160px]"
                     >
                        {retrying ? (
                           <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                        ) : (
                           <span>{locale === "vi" ? "Thử thanh toán lại" : "Retry Payment"}</span>
                        )}
                     </button>
                     <button
                        onClick={() => router.push(`/${locale}`)}
                        className="inline-flex items-center justify-center gap-2 text-zinc-500 hover:text-zinc-800 text-sm font-bold border border-zinc-200 bg-white rounded-xl px-6 py-3 cursor-pointer transition-colors shadow-2xs hover:shadow-xs w-full sm:w-auto"
                     >
                        <Home className="h-4.5 w-4.5" />
                        <span>{locale === "vi" ? "Về trang chủ" : "Return Home"}</span>
                     </button>
                  </div>
               )}
            </div>
         </div>
      </div>
   );
}
