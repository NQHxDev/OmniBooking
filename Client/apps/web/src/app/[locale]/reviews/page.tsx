"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/useAuthStore";
import { reviewService, ReviewResponse } from "@omnibooking/shared";
import { toast } from "sonner";
import { Loader2, Star, Trash2, MessageSquare, Sparkles } from "lucide-react";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { useTranslations } from "next-intl";

export default function ReviewsPage() {
   const t = useTranslations("Reviews");
   const router = useRouter();
   const { isLoggedIn } = useAuthStore();
   const [mounted, setMounted] = useState(false);
   const [reviews, setReviews] = useState<ReviewResponse[]>([]);
   const [loading, setLoading] = useState(true);
   const [deleteLoading, setDeleteLoading] = useState<string | null>(null);

   useEffect(() => {
      const timer = setTimeout(() => setMounted(true), 0);
      if (isLoggedIn === false) {
         router.push("/auth/login");
      }
      return () => clearTimeout(timer);
   }, [isLoggedIn, router]);

   const loadReviews = useCallback(async () => {
      try {
         setLoading(true);
         const res = await reviewService.getMyReviews(0, 50);
         setReviews(res.items || []);
      } catch (err) {
         toast.error(t("failedLoad"));
         console.error("Error loading reviews", err);
      } finally {
         setLoading(false);
      }
   }, [t]);

   useEffect(() => {
      if (mounted && isLoggedIn) {
         const timer = setTimeout(() => {
            loadReviews();
         }, 0);
         return () => clearTimeout(timer);
      }
   }, [mounted, isLoggedIn, loadReviews]);

   const handleDeleteReview = async (id: string) => {
      if (!confirm(t("deleteConfirm"))) {
         return;
      }
      try {
         setDeleteLoading(id);
         await reviewService.delete(id, "Deleted by Guest");
         toast.success(t("deleteSuccess"));
         loadReviews();
      } catch (err) {
         toast.error(t("deleteFailed"));
         console.error(err);
      } finally {
         setDeleteLoading(null);
      }
   };

   if (!mounted) return null;

   return (
      <div className="min-h-screen bg-[#f5f5f5] flex flex-col">
         <Navbar />

         <main className="flex-1 mx-auto max-w-4xl w-full px-4 sm:px-6 lg:px-8 py-10">
            <div className="mb-8">
               <h1 className="text-3xl font-black text-zinc-900 tracking-tight">
                  {t("pageTitle")}
               </h1>
               <p className="text-zinc-500 mt-2">{t("pageSubtitle")}</p>
            </div>

            {loading ? (
               <div className="flex flex-col items-center justify-center py-20">
                  <Loader2 className="h-10 w-10 text-[#003580] animate-spin mb-4" />
                  <p className="text-zinc-500 font-medium">{t("loading")}</p>
               </div>
            ) : reviews.length === 0 ? (
               <div className="bg-white rounded-2xl p-12 text-center border border-zinc-200 shadow-sm flex flex-col items-center justify-center">
                  <div className="h-16 w-16 bg-blue-50 rounded-full flex items-center justify-center mb-6">
                     <MessageSquare className="h-8 w-8 text-[#003580]" />
                  </div>
                  <h3 className="text-xl font-bold text-zinc-900">{t("noReviewsTitle")}</h3>
                  <p className="text-zinc-500 mt-2 max-w-md">{t("noReviewsDesc")}</p>
                  <button
                     onClick={() => router.push("/bookings")}
                     className="mt-6 px-6 py-3 bg-[#003580] hover:bg-[#002b66] text-white font-bold rounded-lg transition-all shadow-md active:scale-95 cursor-pointer"
                  >
                     {t("viewBookings")}
                  </button>
               </div>
            ) : (
               <div className="space-y-6">
                  {reviews.map((review) => (
                     <div
                        key={review.id}
                        className="bg-white rounded-2xl border border-zinc-200 overflow-hidden shadow-xs hover:shadow-md transition-all p-6 space-y-4"
                     >
                        {/* Header: Property Name, Rating, Action */}
                        <div className="flex justify-between items-start gap-4">
                           <div className="space-y-1">
                              <h2 className="text-lg font-bold text-zinc-900">
                                 {review.propertyName}
                              </h2>
                              <div className="flex items-center gap-1">
                                 {[1, 2, 3, 4, 5].map((star) => (
                                    <Star
                                       key={star}
                                       className={`h-4.5 w-4.5 ${
                                          star <= review.rating
                                             ? "fill-yellow-400 text-yellow-400"
                                             : "text-zinc-200 fill-none"
                                       }`}
                                    />
                                 ))}
                                 <span className="text-xs text-zinc-400 font-bold ml-1">
                                    {new Date(review.createdAt).toLocaleDateString()}
                                 </span>
                              </div>
                           </div>

                           <button
                              onClick={() => handleDeleteReview(review.id)}
                              disabled={deleteLoading === review.id}
                              className="p-2 text-zinc-400 hover:text-red-600 transition-colors border border-zinc-100 hover:border-red-100 rounded-lg bg-zinc-50 hover:bg-red-50 cursor-pointer disabled:opacity-50"
                              title={t("deleteButton")}
                           >
                              {deleteLoading === review.id ? (
                                 <Loader2 className="h-4.5 w-4.5 animate-spin" />
                              ) : (
                                 <Trash2 className="h-4.5 w-4.5" />
                              )}
                           </button>
                        </div>

                        {/* Comment Content */}
                        {review.comment ? (
                           <p className="text-sm text-zinc-700 leading-relaxed bg-zinc-50 rounded-xl p-4 border border-zinc-100 italic">
                              &quot;{review.comment}&quot;
                           </p>
                        ) : (
                           <p className="text-xs text-zinc-400 font-medium italic">
                              {t("ratingOnly")}
                           </p>
                        )}

                        {/* Partner Response */}
                        {review.reply && (
                           <div className="mt-4 border-l-4 border-blue-500 pl-4 py-1 space-y-2 bg-blue-50/50 rounded-r-xl p-4 border">
                              <div className="flex items-center justify-between">
                                 <h4 className="text-xs font-black text-blue-800 tracking-wider uppercase flex items-center gap-1.5">
                                    <Sparkles className="h-3.5 w-3.5 fill-blue-500 text-blue-500" />
                                    {t("propertyResponse")}
                                 </h4>
                                 {review.replyUpdatedAt && (
                                    <span className="text-[10px] text-zinc-400 font-bold">
                                       {new Date(review.replyUpdatedAt).toLocaleDateString()}
                                    </span>
                                 )}
                              </div>
                              <p className="text-sm text-zinc-700 leading-relaxed font-medium">
                                 {review.reply}
                              </p>
                           </div>
                        )}
                     </div>
                  ))}
               </div>
            )}
         </main>

         <Footer />
      </div>
   );
}
