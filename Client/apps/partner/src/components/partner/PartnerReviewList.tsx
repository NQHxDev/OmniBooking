"use client";

import React, { useState, useEffect, useMemo, useCallback } from "react";
import { useTranslations } from "next-intl";
import { reviewService, ReviewResponse, PropertyResponse } from "@omnibooking/shared";
import {
   Search,
   Star,
   MessageSquare,
   ChevronLeft,
   ChevronRight,
   AlertCircle,
   Building2,
   CornerDownRight,
   Loader2,
   X,
   Send,
   Edit3,
   MessageCircleWarning,
} from "lucide-react";
import { toast } from "sonner";
import { format } from "date-fns";

interface PartnerReviewListProps {
   initialProperties: PropertyResponse[];
}

const ITEMS_PER_PAGE = 5;

export default function PartnerReviewList({ initialProperties }: PartnerReviewListProps) {
   const t = useTranslations("Partner.reviews");
   const [reviews, setReviews] = useState<ReviewResponse[]>([]);
   const [loading, setLoading] = useState(true);
   const [submittingReplyId, setSubmittingReplyId] = useState<string | null>(null);

   // Filter States
   const [searchTerm, setSearchTerm] = useState("");
   const [selectedPropertyId, setSelectedPropertyId] = useState<string>("all");
   const [selectedRating, setSelectedRating] = useState<string>("all");
   const [replyStatus, setReplyStatus] = useState<string>("all");
   const [currentPage, setCurrentPage] = useState(1);

   // Reply Form State
   const [replyingToId, setReplyingToId] = useState<string | null>(null);
   const [replyText, setReplyText] = useState("");

   // Load all reviews for partner properties
   const fetchReviews = useCallback(async () => {
      try {
         setLoading(true);
         // Fetch up to 100 reviews to support client-side filtering and real-time response
         const res = await reviewService.getPartnerReviews(0, 100);
         setReviews(res.items || []);
      } catch (err) {
         console.error("Error fetching partner reviews:", err);
         toast.error(t("errorFetch"));
      } finally {
         setLoading(false);
      }
   }, [t]);

   useEffect(() => {
      const timer = setTimeout(() => {
         fetchReviews();
      }, 0);
      return () => clearTimeout(timer);
   }, [fetchReviews]);

   // Memoized filtered reviews list
   const filteredReviews = useMemo(() => {
      return reviews.filter((review) => {
         // Property filter
         if (selectedPropertyId !== "all" && review.propertyId !== selectedPropertyId) {
            return false;
         }

         // Rating filter
         if (selectedRating !== "all" && review.rating !== parseInt(selectedRating)) {
            return false;
         }

         // Reply status filter
         if (replyStatus === "replied" && !review.reply) return false;
         if (replyStatus === "unreplied" && review.reply) return false;

         // Search term filter (guest name, property name, comment)
         if (searchTerm.trim() !== "") {
            const search = searchTerm.toLowerCase();
            const guestName = (review.userName || "").toLowerCase();
            const propertyName = (review.propertyName || "").toLowerCase();
            const comment = (review.comment || "").toLowerCase();
            return (
               guestName.includes(search) ||
               propertyName.includes(search) ||
               comment.includes(search)
            );
         }

         return true;
      });
   }, [reviews, selectedPropertyId, selectedRating, replyStatus, searchTerm]);

   // Reset to page 1 when filters change
   useEffect(() => {
      const timer = setTimeout(() => setCurrentPage(1), 0);
      return () => clearTimeout(timer);
   }, [selectedPropertyId, selectedRating, replyStatus, searchTerm]);

   // Pagination calculation
   const totalPages = Math.ceil(filteredReviews.length / ITEMS_PER_PAGE);
   const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
   const visibleReviews = useMemo(() => {
      return filteredReviews.slice(startIndex, startIndex + ITEMS_PER_PAGE);
   }, [filteredReviews, startIndex]);

   // Handle opening/editing reply form
   const handleStartReply = (reviewId: string, currentReply?: string) => {
      setReplyingToId(reviewId);
      setReplyText(currentReply || "");
   };

   const handleCancelReply = () => {
      setReplyingToId(null);
      setReplyText("");
   };

   // Submit reply to API
   const handleSubmitReply = async (reviewId: string) => {
      const trimmedReply = replyText.trim();
      if (!trimmedReply) {
         toast.error(t("errorReplyEmpty"));
         return;
      }

      try {
         setSubmittingReplyId(reviewId);
         const updatedReview = await reviewService.reply(reviewId, { reply: trimmedReply });

         // Update the review in the local state
         setReviews((prevReviews) =>
            prevReviews.map((r) => (r.id === reviewId ? updatedReview : r))
         );

         toast.success(t("successReply"));
         setReplyingToId(null);
         setReplyText("");
      } catch (err) {
         console.error("Error submitting reply:", err);
         toast.error(t("errorReplySubmit"));
      } finally {
         setSubmittingReplyId(null);
      }
   };

   const formatDate = (dateStr: string) => {
      try {
         return format(new Date(dateStr), "dd/MM/yyyy HH:mm");
      } catch {
         return dateStr;
      }
   };

   return (
      <div className="space-y-6 font-sans">
         {/* Stats Panel */}
         <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-xs flex items-center justify-between">
               <div>
                  <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">
                     {t("totalReviews")}
                  </p>
                  <h3 className="text-3xl font-extrabold text-zinc-900 mt-1">{reviews.length}</h3>
               </div>
               <div className="h-12 w-12 bg-blue-50 rounded-2xl flex items-center justify-center text-[#006ce4]">
                  <MessageSquare className="h-6 w-6" />
               </div>
            </div>

            <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-xs flex items-center justify-between">
               <div>
                  <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">
                     {t("averageRating")}
                  </p>
                  <div className="flex items-baseline gap-2 mt-1">
                     <h3 className="text-3xl font-extrabold text-zinc-900">
                        {reviews.length > 0
                           ? (
                                reviews.reduce((acc, r) => acc + r.rating, 0) / reviews.length
                             ).toFixed(1)
                           : "0.0"}
                     </h3>
                     <span className="text-zinc-400 text-sm font-semibold">/ 5.0</span>
                  </div>
               </div>
               <div className="h-12 w-12 bg-amber-50 rounded-2xl flex items-center justify-center text-amber-500">
                  <Star className="h-6 w-6 fill-amber-400 text-amber-500" />
               </div>
            </div>

            <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-xs flex items-center justify-between">
               <div>
                  <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">
                     {t("unreplied")}
                  </p>
                  <h3 className="text-3xl font-extrabold text-rose-600 mt-1">
                     {reviews.filter((r) => !r.reply).length}
                  </h3>
               </div>
               <div className="h-12 w-12 bg-rose-50 rounded-2xl flex items-center justify-center text-rose-600">
                  <MessageCircleWarning className="h-6 w-6" />
               </div>
            </div>
         </div>

         {/* Search and Filters */}
         <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-xs space-y-4">
            <div className="flex flex-col md:flex-row gap-4 items-stretch md:items-center">
               <div className="relative flex-1">
                  <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4.5 w-4.5 text-zinc-400" />
                  <input
                     type="text"
                     placeholder={t("searchPlaceholder")}
                     value={searchTerm}
                     onChange={(e) => setSearchTerm(e.target.value)}
                     className="w-full pl-11 pr-4 py-3 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all"
                  />
               </div>

               <div className="flex flex-wrap gap-3">
                  {/* Property Dropdown Filter */}
                  <select
                     value={selectedPropertyId}
                     onChange={(e) => setSelectedPropertyId(e.target.value)}
                     className="px-4 py-3 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:outline-none bg-white font-medium text-zinc-700 transition-colors"
                  >
                     <option value="all">{t("allProperties")}</option>
                     {initialProperties.map((p) => (
                        <option key={p.id} value={p.id}>
                           {p.name}
                        </option>
                     ))}
                  </select>

                  {/* Rating Dropdown Filter */}
                  <select
                     value={selectedRating}
                     onChange={(e) => setSelectedRating(e.target.value)}
                     className="px-4 py-3 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:outline-none bg-white font-medium text-zinc-700 transition-colors"
                  >
                     <option value="all">{t("allRatings")}</option>
                     <option value="5">{t("starsCount", { count: 5 })}</option>
                     <option value="4">{t("starsCount", { count: 4 })}</option>
                     <option value="3">{t("starsCount", { count: 3 })}</option>
                     <option value="2">{t("starsCount", { count: 2 })}</option>
                     <option value="1">{t("starsCount", { count: 1 })}</option>
                  </select>

                  {/* Status Dropdown Filter */}
                  <select
                     value={replyStatus}
                     onChange={(e) => setReplyStatus(e.target.value)}
                     className="px-4 py-3 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:outline-none bg-white font-medium text-zinc-700 transition-colors"
                  >
                     <option value="all">{t("replyStatus")}</option>
                     <option value="replied">{t("replied")}</option>
                     <option value="unreplied">{t("unreplied")}</option>
                  </select>
               </div>
            </div>

            <div className="flex items-center justify-between text-xs text-zinc-500 font-medium">
               <div>
                  {t.rich("showingMatchingReviews", {
                     count: filteredReviews.length,
                     span: (chunks) => <span className="font-bold text-zinc-800">{chunks}</span>,
                  })}
               </div>
               {(searchTerm ||
                  selectedPropertyId !== "all" ||
                  selectedRating !== "all" ||
                  replyStatus !== "all") && (
                  <button
                     onClick={() => {
                        setSearchTerm("");
                        setSelectedPropertyId("all");
                        setSelectedRating("all");
                        setReplyStatus("all");
                     }}
                     className="text-[#006ce4] hover:underline cursor-pointer font-bold"
                  >
                     {t("clearFilters")}
                  </button>
               )}
            </div>
         </div>

         {/* Reviews List */}
         {loading ? (
            <div className="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-zinc-200/80 shadow-xs">
               <Loader2 className="h-10 w-10 text-[#006ce4] animate-spin mb-4" />
               <p className="text-zinc-500 text-sm font-medium">{t("loadingReviews")}</p>
            </div>
         ) : filteredReviews.length === 0 ? (
            <div className="flex flex-col items-center justify-center rounded-3xl border border-zinc-200/80 bg-white p-16 text-center shadow-xs">
               <div className="flex h-16 w-16 items-center justify-center rounded-full bg-zinc-50 text-zinc-400 border border-zinc-200">
                  <AlertCircle className="h-8 w-8" />
               </div>
               <h3 className="mt-4 text-lg font-bold text-zinc-900">{t("noReviewsFound")}</h3>
               <p className="mt-1 max-w-sm text-xs text-zinc-500 font-medium leading-relaxed">
                  {t("noReviewsDesc")}
               </p>
            </div>
         ) : (
            <div className="space-y-6">
               {visibleReviews.map((review) => {
                  const isReplying = replyingToId === review.id;

                  return (
                     <div
                        key={review.id}
                        className="bg-white rounded-3xl border border-zinc-200/80 shadow-xs overflow-hidden transition-all hover:shadow-md p-6 space-y-5"
                     >
                        {/* Card Top: User Info & Property Name */}
                        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                           <div className="flex items-center gap-3">
                              <div className="h-11 w-11 rounded-full bg-blue-50 border border-blue-100 flex items-center justify-center text-[#006ce4] font-black text-sm uppercase shadow-xs overflow-hidden">
                                 {review.userAvatarUrl ? (
                                    <img
                                       src={review.userAvatarUrl}
                                       alt={review.userName}
                                       className="h-full w-full object-cover"
                                    />
                                 ) : review.userName ? (
                                    review.userName.substring(0, 2)
                                 ) : (
                                    t("guestInitialsDefault")
                                 )}
                              </div>
                              <div>
                                 <h4 className="font-extrabold text-zinc-900 text-sm">
                                    {review.userName || t("anonymousGuest")}
                                 </h4>
                                 <div className="flex items-center gap-1.5 text-xs text-zinc-500 font-medium mt-0.5">
                                    <span className="flex items-center gap-1 text-[#006ce4] font-bold">
                                       <Building2 className="h-3.5 w-3.5" />
                                       {review.propertyName}
                                    </span>
                                    <span>•</span>
                                    <span>{formatDate(review.createdAt)}</span>
                                 </div>
                              </div>
                           </div>

                           {/* Star Ratings Badge */}
                           <div className="flex items-center gap-2">
                              <div className="flex gap-0.5">
                                 {[1, 2, 3, 4, 5].map((star) => (
                                    <Star
                                       key={star}
                                       className={`h-4.5 w-4.5 ${
                                          star <= review.rating
                                             ? "fill-amber-400 text-amber-400"
                                             : "text-zinc-200 fill-none"
                                       }`}
                                    />
                                 ))}
                              </div>
                              <span className="inline-flex items-center justify-center h-6 w-10 text-xs font-black bg-blue-50 text-[#006ce4] rounded-lg">
                                 {review.rating * 2}/10
                              </span>
                           </div>
                        </div>

                        {/* Comment Content */}
                        {review.comment ? (
                           <div className="text-zinc-700 text-sm leading-relaxed font-medium bg-zinc-50/50 border border-zinc-100 rounded-2xl p-4.5 italic">
                              &quot;{review.comment}&quot;
                           </div>
                        ) : (
                           <div className="text-zinc-400 text-xs italic font-medium">
                              {t("ratingOnly")}
                           </div>
                        )}

                        {/* Reply Display & Forms */}
                        <div className="space-y-4">
                           {review.reply && !isReplying && (
                              <div className="bg-blue-50/40 border border-blue-100/60 rounded-2xl p-4.5 space-y-2 relative ml-4 sm:ml-8">
                                 <div className="absolute -left-4 top-4.5 text-blue-300">
                                    <CornerDownRight className="h-5 w-5" />
                                 </div>
                                 <div className="flex justify-between items-center">
                                    <span className="text-xs font-black uppercase tracking-wider text-blue-800 flex items-center gap-1">
                                       {t("yourReply")}
                                    </span>
                                    <div className="flex items-center gap-3">
                                       {review.replyUpdatedAt && (
                                          <span className="text-[10px] text-zinc-400 font-bold">
                                             {formatDate(review.replyUpdatedAt)}
                                          </span>
                                       )}
                                       <button
                                          onClick={() => handleStartReply(review.id, review.reply)}
                                          className="text-zinc-500 hover:text-[#006ce4] transition-colors p-1 hover:bg-white rounded-lg border border-transparent hover:border-zinc-200 cursor-pointer flex items-center gap-1 text-xs font-bold"
                                       >
                                          <Edit3 className="h-3.5 w-3.5" />
                                          {t("edit")}
                                       </button>
                                    </div>
                                 </div>
                                 <p className="text-zinc-700 text-sm leading-relaxed font-medium">
                                    {review.reply}
                                 </p>
                              </div>
                           )}

                           {!review.reply && !isReplying && (
                              <div className="flex justify-between items-center ml-4 sm:ml-8 pt-2">
                                 <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-rose-50 text-rose-600 border border-rose-200/50">
                                    <span className="h-1.5 w-1.5 rounded-full bg-rose-500 animate-pulse" />
                                    {t("unreplied")}
                                 </span>
                                 <button
                                    onClick={() => handleStartReply(review.id)}
                                    className="px-4.5 py-2 text-xs font-bold text-white bg-[#006ce4] hover:bg-[#0057b7] rounded-xl shadow-md shadow-blue-100 hover:shadow-blue-200 transition-all cursor-pointer flex items-center gap-1.5"
                                 >
                                    <MessageSquare className="h-3.5 w-3.5" />
                                    {t("replyToReview")}
                                 </button>
                              </div>
                           )}

                           {/* Interactive Reply Input State */}
                           {isReplying && (
                              <div className="bg-zinc-50 border border-zinc-200/80 rounded-2xl p-4.5 ml-4 sm:ml-8 space-y-4 shadow-inner">
                                 <div className="flex justify-between items-center">
                                    <span className="text-xs font-extrabold text-zinc-600 uppercase tracking-wider">
                                       {review.reply ? t("editReply") : t("writeReply")}
                                    </span>
                                    <span className="text-[10px] text-zinc-400 font-bold">
                                       {t("maxCharacters")}
                                    </span>
                                 </div>

                                 <textarea
                                    rows={4}
                                    placeholder={t("replyFieldPlaceholder")}
                                    value={replyText}
                                    onChange={(e) => setReplyText(e.target.value)}
                                    maxLength={1000}
                                    className="w-full bg-white border border-zinc-200 rounded-xl p-3.5 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all placeholder:text-zinc-400 font-medium"
                                 />

                                 <div className="flex justify-between items-center">
                                    <span className="text-xs text-zinc-400 font-semibold">
                                       {t.rich("charactersEntered", {
                                          count: replyText.length,
                                          span: (chunks) => (
                                             <span className="font-bold text-zinc-600">
                                                {chunks}
                                             </span>
                                          ),
                                       })}
                                    </span>
                                    <div className="flex gap-2">
                                       <button
                                          onClick={handleCancelReply}
                                          disabled={submittingReplyId === review.id}
                                          className="px-4 py-2 border border-zinc-200 rounded-xl text-xs font-bold text-zinc-500 bg-white hover:bg-zinc-50 transition-colors flex items-center gap-1 cursor-pointer disabled:opacity-50"
                                       >
                                          <X className="h-3.5 w-3.5" />
                                          {t("cancel")}
                                       </button>
                                       <button
                                          onClick={() => handleSubmitReply(review.id)}
                                          disabled={submittingReplyId === review.id}
                                          className="px-4 py-2 bg-[#006ce4] hover:bg-[#0057b7] text-white rounded-xl text-xs font-bold shadow-md shadow-blue-100 hover:shadow-blue-200 transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
                                       >
                                          {submittingReplyId === review.id ? (
                                             <Loader2 className="h-3.5 w-3.5 animate-spin" />
                                          ) : (
                                             <Send className="h-3.5 w-3.5" />
                                          )}
                                          {t("sendReply")}
                                       </button>
                                    </div>
                                 </div>
                              </div>
                           )}
                        </div>
                     </div>
                  );
               })}

               {/* Pagination bar */}
               {totalPages > 1 && (
                  <div className="flex items-center justify-between border border-zinc-200 bg-white rounded-2xl px-6 py-4.5 shadow-xs">
                     <div className="text-xs font-medium text-zinc-500">
                        {t.rich("paginationInfo", {
                           start: startIndex + 1,
                           end: Math.min(
                              startIndex + visibleReviews.length,
                              filteredReviews.length
                           ),
                           total: filteredReviews.length,
                           span: (chunks) => (
                              <span className="font-bold text-zinc-800">{chunks}</span>
                           ),
                        })}
                     </div>
                     <div className="flex items-center gap-1.5">
                        <button
                           onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                           disabled={currentPage === 1}
                           className="flex h-9 w-9 items-center justify-center rounded-xl border border-zinc-200 bg-white text-zinc-600 hover:border-[#006ce4] hover:text-[#006ce4] disabled:opacity-30 disabled:hover:border-zinc-200 disabled:hover:text-zinc-600 transition-all cursor-pointer"
                        >
                           <ChevronLeft className="h-4 w-4" />
                        </button>
                        <div className="flex gap-1">
                           {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                              <button
                                 key={page}
                                 onClick={() => setCurrentPage(page)}
                                 className={`h-9 w-9 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                                    currentPage === page
                                       ? "bg-[#006ce4] text-white shadow-md shadow-blue-100"
                                       : "bg-white border border-zinc-200 text-zinc-600 hover:border-zinc-300"
                                 }`}
                              >
                                 {page}
                              </button>
                           ))}
                        </div>
                        <button
                           onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                           disabled={currentPage === totalPages}
                           className="flex h-9 w-9 items-center justify-center rounded-xl border border-zinc-200 bg-white text-zinc-600 hover:border-[#006ce4] hover:text-[#006ce4] disabled:opacity-30 disabled:hover:border-zinc-200 disabled:hover:text-zinc-600 transition-all cursor-pointer"
                        >
                           <ChevronRight className="h-4 w-4" />
                        </button>
                     </div>
                  </div>
               )}
            </div>
         )}
      </div>
   );
}
