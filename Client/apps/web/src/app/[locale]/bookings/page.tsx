"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/useAuthStore";
import {
   bookingService,
   reviewService,
   BookingResponse,
   ReviewResponse,
} from "@omnibooking/shared";
import { toast } from "sonner";
import { Loader2, Calendar, Building, Star, X, MessageSquare, AlertCircle } from "lucide-react";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { motion, AnimatePresence } from "framer-motion";
import { useTranslations } from "next-intl";

export default function BookingsPage() {
   const tErrors = useTranslations("Errors");
   const tBookings = useTranslations("Bookings");
   const router = useRouter();
   const { isLoggedIn } = useAuthStore();
   const [mounted, setMounted] = useState(false);
   const [bookings, setBookings] = useState<BookingResponse[]>([]);
   const [reviewedBookingIds, setReviewedBookingIds] = useState<Set<string>>(new Set());
   const [loading, setLoading] = useState(true);

   // Modal review state
   const [activeBooking, setActiveBooking] = useState<BookingResponse | null>(null);
   const [rating, setRating] = useState<number>(0);
   const [comment, setComment] = useState<string>("");
   const [hoveredRating, setHoveredRating] = useState<number>(0);
   const [submitLoading, setSubmitLoading] = useState(false);
   const [commentError, setCommentError] = useState<string | null>(null);

   const logError = useCallback((msg: string, err: unknown) => {
      console.error(msg, err);
   }, []);

   useEffect(() => {
      const timer = setTimeout(() => setMounted(true), 0);
      if (isLoggedIn === false) {
         router.push("/auth/login");
      }
      return () => clearTimeout(timer);
   }, [isLoggedIn, router]);

   const loadData = useCallback(async () => {
      try {
         setLoading(true);
         const myBookings = await bookingService.getMyBookings();
         // Sort bookings by check-in date descending
         const sortedBookings = [...myBookings].sort(
            (a, b) => new Date(b.checkInDate).getTime() - new Date(a.checkInDate).getTime()
         );
         setBookings(sortedBookings);

         // Load user reviews to identify which bookings have already been reviewed
         try {
            const myReviews = await reviewService.getMyReviews(0, 50);
            const reviewedIds = new Set(myReviews.items.map((r: ReviewResponse) => r.bookingId));
            setReviewedBookingIds(reviewedIds);
         } catch (e) {
            logError("Failed to fetch user reviews", e);
         }
      } catch (err) {
         toast.error(tBookings("failedLoadBookings"));
         logError("Error loading bookings page data", err);
      } finally {
         setLoading(false);
      }
   }, [tBookings, logError]);

   useEffect(() => {
      if (mounted && isLoggedIn) {
         const timer = setTimeout(() => {
            loadData();
         }, 0);
         return () => clearTimeout(timer);
      }
   }, [mounted, isLoggedIn, loadData]);

   const handleOpenReview = (booking: BookingResponse) => {
      setActiveBooking(booking);
      setRating(0);
      setComment("");
      setCommentError(null);
   };

   const handleCloseReview = () => {
      setActiveBooking(null);
   };

   const handleCommentChange = (val: string) => {
      setComment(val);
      if (val.trim().length > 0 && val.length < 10) {
         setCommentError(tBookings("commentMinLength"));
      } else if (val.length > 1000) {
         setCommentError(tBookings("commentMaxLength"));
      } else {
         setCommentError(null);
      }
   };

   const submitReview = async (e: React.FormEvent) => {
      e.preventDefault();
      if (!activeBooking) return;

      if (rating === 0) {
         toast.error(tBookings("selectRating"));
         return;
      }

      if (comment.trim().length > 0 && comment.length < 10) {
         setCommentError(tBookings("commentMinLength"));
         return;
      }

      try {
         setSubmitLoading(true);
         await reviewService.create({
            bookingId: activeBooking.id,
            rating,
            comment: comment.trim() || undefined,
         });

         toast.success(tBookings("reviewSubmitSuccess"));
         handleCloseReview();
      } catch (err) {
         const error = err as { response?: { data?: { errorCode?: string } }; errorCode?: string };
         const code = error.response?.data?.errorCode || error.errorCode;
         let message = tErrors("reviewSubmitFailed");
         if (code === "REV_011" || code === "AUTH_011") {
            message = tErrors("REV_011");
         } else if (code === "REV_004") {
            message = tErrors("REV_004");
         }
         toast.error(message);
         logError("Submit review error", err);
      } finally {
         setSubmitLoading(false);
      }
   };

   if (!mounted) return null;

   const formatStatus = (status: string) => {
      switch (status) {
         case "CONFIRMED":
            return {
               label: tBookings("statusConfirmed"),
               class: "bg-green-100 text-green-800 border-green-200",
            };
         case "PENDING":
            return {
               label: tBookings("statusPending"),
               class: "bg-yellow-100 text-yellow-800 border-yellow-200",
            };
         case "STAYED":
            return {
               label: tBookings("statusCompleted"),
               class: "bg-blue-100 text-blue-800 border-blue-200",
            };
         case "CANCELLED":
            return {
               label: tBookings("statusCancelled"),
               class: "bg-red-100 text-red-800 border-red-200",
            };
         default:
            return { label: status, class: "bg-zinc-100 text-zinc-800 border-zinc-200" };
      }
   };

   return (
      <div className="min-h-screen bg-[#f5f5f5] flex flex-col">
         <Navbar />

         <main className="flex-1 mx-auto max-w-5xl w-full px-4 sm:px-6 lg:px-8 py-10">
            <div className="mb-8">
               <h1 className="text-3xl font-black text-zinc-900 tracking-tight">
                  {tBookings("pageTitle")}
               </h1>
               <p className="text-zinc-500 mt-2">{tBookings("pageSubtitle")}</p>
            </div>

            {loading ? (
               <div className="flex flex-col items-center justify-center py-20">
                  <Loader2 className="h-10 w-10 text-[#003580] animate-spin mb-4" />
                  <p className="text-zinc-500 font-medium">{tBookings("loadingTrips")}</p>
               </div>
            ) : bookings.length === 0 ? (
               <div className="bg-white rounded-2xl p-12 text-center border border-zinc-200 shadow-sm flex flex-col items-center justify-center">
                  <div className="h-16 w-16 bg-blue-50 rounded-full flex items-center justify-center mb-6">
                     <Calendar className="h-8 w-8 text-[#003580]" />
                  </div>
                  <h3 className="text-xl font-bold text-zinc-900">
                     {tBookings("noBookingsTitle")}
                  </h3>
                  <p className="text-zinc-500 mt-2 max-w-md">{tBookings("noBookingsDesc")}</p>
                  <button
                     onClick={() => router.push("/")}
                     className="mt-6 px-6 py-3 bg-[#003580] hover:bg-[#002b66] text-white font-bold rounded-lg transition-all shadow-md active:scale-95 cursor-pointer"
                  >
                     {tBookings("bookNow")}
                  </button>
               </div>
            ) : (
               <div className="space-y-6">
                  {bookings.map((booking) => {
                     const statusInfo = formatStatus(booking.status);
                     const isStayed = booking.status === "STAYED";
                     const isCheckoutPassed = new Date(booking.checkOutDate) <= new Date();
                     const hasReviewed = reviewedBookingIds.has(booking.id);
                     const canReview = isStayed && isCheckoutPassed && !hasReviewed;

                     return (
                        <div
                           key={booking.id}
                           className="bg-white rounded-2xl border border-zinc-200 overflow-hidden shadow-xs hover:shadow-md transition-all flex flex-col md:flex-row"
                        >
                           {/* Left Accent Color bar */}
                           <div className="w-full md:w-2 bg-[#003580] shrink-0" />

                           {/* Main Booking Body */}
                           <div className="p-6 flex-1 flex flex-col md:flex-row md:items-center justify-between gap-6">
                              <div className="space-y-3">
                                 <div className="flex flex-wrap items-center gap-2">
                                    <span className="text-xs font-bold text-zinc-400">
                                       CODE: {booking.bookingCode}
                                    </span>
                                    <span
                                       className={`px-2.5 py-0.5 rounded-full text-xs font-bold border ${statusInfo.class}`}
                                    >
                                       {statusInfo.label}
                                    </span>
                                 </div>

                                 <h2 className="text-xl font-bold text-zinc-900 flex items-center gap-2">
                                    <Building className="h-5 w-5 text-zinc-500 shrink-0" />
                                    {booking.propertyName}
                                 </h2>

                                 <p className="text-sm font-semibold text-zinc-700">
                                    {booking.roomTypeName} ({booking.numRooms}{" "}
                                    {tBookings("roomsUnit")})
                                 </p>

                                 <div className="flex flex-wrap items-center gap-4 text-xs text-zinc-500 pt-1">
                                    <div className="flex items-center gap-1">
                                       <Calendar className="h-4 w-4 text-zinc-400" />
                                       <span>
                                          {booking.checkInDate} &rarr; {booking.checkOutDate}
                                       </span>
                                    </div>
                                 </div>
                              </div>

                              {/* Price and Review Action Column */}
                              <div className="flex flex-col md:items-end justify-between gap-4 shrink-0 border-t md:border-t-0 pt-4 md:pt-0 border-zinc-100">
                                 <div className="md:text-right">
                                    <p className="text-xs text-zinc-400 font-semibold">
                                       {tBookings("totalPrice")}
                                    </p>
                                    <p className="text-lg font-black text-[#003580] mt-0.5">
                                       {booking.finalPrice.toLocaleString()}{" "}
                                       {booking.currency || "VND"}
                                    </p>
                                 </div>

                                 {canReview ? (
                                    <button
                                       onClick={() => handleOpenReview(booking)}
                                       className="px-5 py-2.5 bg-[#003580] hover:bg-[#002b66] text-white text-sm font-bold rounded-lg transition-all active:scale-[0.98] cursor-pointer flex items-center justify-center gap-2 shadow-xs"
                                    >
                                       <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                                       {tBookings("writeReview")}
                                    </button>
                                 ) : hasReviewed ? (
                                    <div className="flex items-center gap-1.5 px-3 py-2 bg-green-50 rounded-lg text-green-700 text-xs font-bold border border-green-200">
                                       <MessageSquare className="h-4 w-4" />
                                       {tBookings("reviewed")}
                                    </div>
                                 ) : null}
                              </div>
                           </div>
                        </div>
                     );
                  })}
               </div>
            )}
         </main>

         {/* Review Dialog/Modal */}
         <AnimatePresence>
            {activeBooking && (
               <div className="fixed inset-0 z-100 flex items-center justify-center p-4">
                  {/* Backdrop */}
                  <motion.div
                     initial={{ opacity: 0 }}
                     animate={{ opacity: 1 }}
                     exit={{ opacity: 0 }}
                     onClick={handleCloseReview}
                     className="absolute inset-0 bg-zinc-900/60 backdrop-blur-sm"
                  />

                  {/* Modal Container */}
                  <motion.div
                     initial={{ opacity: 0, scale: 0.95, y: 20 }}
                     animate={{ opacity: 1, scale: 1, y: 0 }}
                     exit={{ opacity: 0, scale: 0.95, y: 20 }}
                     className="relative w-full max-w-lg overflow-hidden rounded-2xl bg-white shadow-2xl border border-zinc-200"
                  >
                     <div className="h-1.5 w-full bg-[#003580]" />

                     <button
                        onClick={handleCloseReview}
                        className="absolute right-4 top-5 p-2 text-zinc-400 hover:text-zinc-600 transition-colors"
                     >
                        <X className="h-5 w-5" />
                     </button>

                     <div className="p-6 sm:p-8">
                        <div className="mb-6">
                           <span className="text-xs font-bold text-zinc-400">
                              {tBookings("modalTitle")}
                           </span>
                           <h2 className="text-2xl font-black text-zinc-900 tracking-tight mt-1">
                              {activeBooking.propertyName}
                           </h2>
                           <p className="text-xs text-zinc-500 mt-1">
                              {activeBooking.roomTypeName} &bull; {activeBooking.checkInDate} &rarr;{" "}
                              {activeBooking.checkOutDate}
                           </p>
                        </div>

                        <form onSubmit={submitReview} className="space-y-6">
                           {/* Rating Star Selector */}
                           <div className="space-y-2">
                              <label className="text-sm font-bold text-zinc-800">
                                 {tBookings("ratingQuestion")}
                              </label>

                              <div className="flex items-center gap-2 py-2">
                                 {[1, 2, 3, 4, 5].map((star) => (
                                    <button
                                       key={star}
                                       type="button"
                                       onClick={() => setRating(star)}
                                       onMouseEnter={() => setHoveredRating(star)}
                                       onMouseLeave={() => setHoveredRating(0)}
                                       className="p-1 hover:scale-115 transition-all cursor-pointer"
                                    >
                                       <Star
                                          className={`h-9 w-9 transition-colors ${
                                             star <= (hoveredRating || rating)
                                                ? "fill-yellow-400 text-yellow-400"
                                                : "text-zinc-300 fill-none"
                                          }`}
                                       />
                                    </button>
                                 ))}
                                 {rating > 0 && (
                                    <span className="text-sm font-bold text-zinc-700 ml-2">
                                       {rating}/5
                                    </span>
                                 )}
                              </div>
                           </div>

                           {/* Comment Text Area */}
                           <div className="space-y-2">
                              <div className="flex justify-between items-center">
                                 <label className="text-sm font-bold text-zinc-800">
                                    {tBookings("writeCommentLabel")}
                                 </label>
                                 <span className="text-[10px] text-zinc-400 font-bold uppercase">
                                    {tBookings("optional")}
                                 </span>
                              </div>

                              <textarea
                                 rows={4}
                                 value={comment}
                                 onChange={(e) => handleCommentChange(e.target.value)}
                                 placeholder={tBookings("commentPlaceholder")}
                                 className={`w-full text-sm rounded-lg border p-4 outline-none resize-none transition-all placeholder:text-zinc-400 focus:ring-1 ${
                                    commentError
                                       ? "border-red-500 focus:border-red-500 focus:ring-red-100"
                                       : "border-zinc-200 focus:border-[#003580] focus:ring-blue-100"
                                 }`}
                              />

                              {commentError ? (
                                 <p className="text-xs text-red-500 font-medium flex items-center gap-1">
                                    <AlertCircle className="h-3.5 w-3.5" />
                                    {commentError}
                                 </p>
                              ) : comment.trim().length > 0 ? (
                                 <p className="text-right text-[10px] font-bold text-zinc-400">
                                    {comment.length}/1000
                                 </p>
                              ) : null}
                           </div>

                           {/* Notice */}
                           <div className="p-3 bg-blue-50 border border-blue-100 rounded-lg text-[11px] text-zinc-600 leading-relaxed">
                              {tBookings("modalNotice")}
                           </div>

                           {/* Actions */}
                           <div className="flex gap-3 pt-2">
                              <button
                                 type="button"
                                 onClick={handleCloseReview}
                                 disabled={submitLoading}
                                 className="w-1/2 rounded-lg border border-zinc-200 py-3 text-sm font-semibold text-zinc-600 transition-all hover:bg-zinc-50 disabled:opacity-50 cursor-pointer"
                              >
                                 {tBookings("cancel")}
                              </button>
                              <button
                                 type="submit"
                                 disabled={submitLoading || !!commentError}
                                 className="w-1/2 flex items-center justify-center gap-2 rounded-lg bg-[#003580] py-3 text-sm font-bold text-white transition-all hover:bg-[#002b66] disabled:opacity-50 active:scale-[0.98] cursor-pointer"
                              >
                                 {submitLoading ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                 ) : (
                                    tBookings("submitReview")
                                 )}
                              </button>
                           </div>
                        </form>
                     </div>
                  </motion.div>
               </div>
            )}
         </AnimatePresence>

         <Footer />
      </div>
   );
}
