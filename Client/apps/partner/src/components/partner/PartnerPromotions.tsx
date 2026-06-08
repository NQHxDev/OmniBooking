"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useTranslations } from "next-intl";
import {
   couponService,
   CouponResponse,
   CouponRequest,
   PropertyResponse,
} from "@omnibooking/shared";
import {
   Search,
   Plus,
   Ticket,
   Trash2,
   Edit,
   X,
   Loader2,
   AlertCircle,
   Building2,
   Check,
   Calendar,
   DollarSign,
   Percent,
} from "lucide-react";
import { toast } from "sonner";
import { format } from "date-fns";

interface PartnerPromotionsProps {
   initialProperties: PropertyResponse[];
}

export default function PartnerPromotions({ initialProperties }: PartnerPromotionsProps) {
   const t = useTranslations("Partner.promotions");
   const [selectedPropertyId, setSelectedPropertyId] = useState<string>(
      initialProperties.length > 0 ? initialProperties[0].id : "none"
   );
   const [coupons, setCoupons] = useState<CouponResponse[]>([]);
   const [loading, setLoading] = useState(false);
   const [searchTerm, setSearchTerm] = useState("");

   // Modal State
   const [isModalOpen, setIsModalOpen] = useState(false);
   const [editingCoupon, setEditingCoupon] = useState<CouponResponse | null>(null);
   const [isSubmitting, setIsSubmitting] = useState(false);

   // Form State
   const [code, setCode] = useState("");
   const [discountType, setDiscountType] = useState("FIXED_AMOUNT");
   const [discountValue, setDiscountValue] = useState("");
   const [minBookingAmount, setMinBookingAmount] = useState("");
   const [maxDiscountAmount, setMaxDiscountAmount] = useState("");
   const [validFrom, setValidFrom] = useState("");
   const [validUntil, setValidUntil] = useState("");
   const [usageLimit, setUsageLimit] = useState("");
   const [isActive, setIsActive] = useState(true);

   // Fetch coupons for property
   const fetchCoupons = useCallback(async () => {
      if (selectedPropertyId === "all" || selectedPropertyId === "none") {
         setCoupons([]);
         return;
      }
      try {
         setLoading(true);
         const res = await couponService.getByProperty(selectedPropertyId);
         setCoupons(res || []);
      } catch (err) {
         console.error("Error fetching coupons:", err);
         toast.error(t("errorFetch"));
      } finally {
         setLoading(false);
      }
   }, [selectedPropertyId, t]);

   useEffect(() => {
      const timer = setTimeout(() => {
         fetchCoupons();
      }, 0);
      return () => clearTimeout(timer);
   }, [fetchCoupons]);

   // Handle open create modal
   const handleOpenCreateModal = () => {
      setEditingCoupon(null);
      setCode("");
      setDiscountType("FIXED_AMOUNT");
      setDiscountValue("");
      setMinBookingAmount("");
      setMaxDiscountAmount("");

      // Defaults: start now, end in 30 days
      const now = new Date();
      const thirtyDaysLater = new Date();
      thirtyDaysLater.setDate(now.getDate() + 30);

      setValidFrom(format(now, "yyyy-MM-dd'T'HH:mm"));
      setValidUntil(format(thirtyDaysLater, "yyyy-MM-dd'T'HH:mm"));
      setUsageLimit("");
      setIsActive(true);
      setIsModalOpen(true);
   };

   // Handle open edit modal
   const handleOpenEditModal = (coupon: CouponResponse) => {
      setEditingCoupon(coupon);
      setCode(coupon.code);
      setDiscountType(coupon.discountType);
      setDiscountValue(coupon.discountValue.toString());
      setMinBookingAmount(coupon.minBookingAmount ? coupon.minBookingAmount.toString() : "");
      setMaxDiscountAmount(coupon.maxDiscountAmount ? coupon.maxDiscountAmount.toString() : "");

      const fromDate = new Date(coupon.validFrom);
      const untilDate = new Date(coupon.validUntil);

      setValidFrom(format(fromDate, "yyyy-MM-dd'T'HH:mm"));
      setValidUntil(format(untilDate, "yyyy-MM-dd'T'HH:mm"));
      setUsageLimit(coupon.usageLimit ? coupon.usageLimit.toString() : "");
      setIsActive(coupon.isActive);
      setIsModalOpen(true);
   };

   // Validate and submit
   const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      if (!code.trim()) {
         toast.error(t("errorCodeRequired"));
         return;
      }
      if (!discountValue || parseFloat(discountValue) <= 0) {
         toast.error(t("errorDiscountValueInvalid"));
         return;
      }
      if (!validFrom || !validUntil) {
         toast.error(t("errorDatesRequired"));
         return;
      }

      const fromInstant = new Date(validFrom).toISOString();
      const untilInstant = new Date(validUntil).toISOString();

      if (new Date(validUntil) <= new Date(validFrom)) {
         toast.error(t("errorEndDateBeforeStartDate"));
         return;
      }

      const req: CouponRequest = {
         code: code.trim().toUpperCase(),
         discountType,
         discountValue: parseFloat(discountValue),
         minBookingAmount: minBookingAmount ? parseFloat(minBookingAmount) : undefined,
         maxDiscountAmount: maxDiscountAmount ? parseFloat(maxDiscountAmount) : undefined,
         validFrom: fromInstant,
         validUntil: untilInstant,
         usageLimit: usageLimit ? parseInt(usageLimit) : undefined,
         isActive,
         propertyId: selectedPropertyId,
      };

      try {
         setIsSubmitting(true);
         if (editingCoupon) {
            const updated = await couponService.update(editingCoupon.id, req);
            setCoupons((prev) => prev.map((c) => (c.id === editingCoupon.id ? updated : c)));
            toast.success(t("successUpdate"));
         } else {
            const created = await couponService.create(req);
            setCoupons((prev) => [created, ...prev]);
            toast.success(t("successCreate"));
         }
         setIsModalOpen(false);
      } catch (err) {
         console.error("Error saving coupon:", err);
         toast.error(t("errorSave"));
      } finally {
         setIsSubmitting(false);
      }
   };

   // Delete Coupon
   const handleDelete = async (id: string) => {
      if (!confirm(t("confirmDelete"))) return;
      try {
         await couponService.delete(id);
         setCoupons((prev) => prev.filter((c) => c.id !== id));
         toast.success(t("successDelete"));
      } catch (err) {
         console.error("Error deleting coupon:", err);
         toast.error(t("errorDelete"));
      }
   };

   // Toggle coupon active state directly
   const handleToggleActive = async (coupon: CouponResponse) => {
      const req: CouponRequest = {
         code: coupon.code,
         discountType: coupon.discountType,
         discountValue: coupon.discountValue,
         minBookingAmount: coupon.minBookingAmount,
         maxDiscountAmount: coupon.maxDiscountAmount,
         validFrom: coupon.validFrom,
         validUntil: coupon.validUntil,
         usageLimit: coupon.usageLimit,
         isActive: !coupon.isActive,
         propertyId: selectedPropertyId,
      };
      try {
         const updated = await couponService.update(coupon.id, req);
         setCoupons((prev) => prev.map((c) => (c.id === coupon.id ? updated : c)));
         toast.success(updated.isActive ? t("successActivated") : t("successDeactivated"));
      } catch (err) {
         console.error("Error toggling active state:", err);
         toast.error(t("errorUpdate"));
      }
   };

   // Filters
   const filteredCoupons = useMemo(() => {
      return coupons.filter((c) => {
         if (searchTerm.trim() !== "") {
            return c.code.toLowerCase().includes(searchTerm.toLowerCase());
         }
         return true;
      });
   }, [coupons, searchTerm]);

   // Stats
   const activeCount = useMemo(() => coupons.filter((c) => c.isActive).length, [coupons]);
   const totalRedeemed = useMemo(() => coupons.reduce((acc, c) => acc + c.usedCount, 0), [coupons]);
   const totalReserved = useMemo(
      () => coupons.reduce((acc, c) => acc + c.reservedCount, 0),
      [coupons]
   );

   const formatDate = (dateStr: string) => {
      try {
         return format(new Date(dateStr), "dd/MM/yyyy HH:mm");
      } catch {
         return dateStr;
      }
   };

   return (
      <div className="space-y-6 font-sans">
         {/* Top selection bar */}
         <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-xs flex flex-col md:flex-row gap-4 items-stretch md:items-center justify-between">
            <div className="flex items-center gap-3">
               <Building2 className="h-5 w-5 text-zinc-400" />
               <select
                  value={selectedPropertyId}
                  onChange={(e) => setSelectedPropertyId(e.target.value)}
                  className="px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:outline-none bg-white font-bold text-zinc-700 transition-colors"
               >
                  {initialProperties.length === 0 ? (
                     <option value="none" disabled>
                        {t("noPropertiesAvailable") || "No properties available"}
                     </option>
                  ) : (
                     initialProperties.map((p) => (
                        <option key={p.id} value={p.id}>
                           {p.name}
                        </option>
                     ))
                  )}
               </select>
            </div>

            {selectedPropertyId !== "all" && selectedPropertyId !== "none" && (
               <button
                  onClick={handleOpenCreateModal}
                  className="px-5 py-3 text-sm font-bold text-white bg-[#006ce4] hover:bg-[#0057b7] rounded-2xl shadow-md shadow-blue-100 hover:shadow-blue-200 transition-all flex items-center gap-2 cursor-pointer"
               >
                  <Plus className="h-4 w-4" />
                  {t("createCoupon")}
               </button>
            )}
         </div>

         {selectedPropertyId === "all" || selectedPropertyId === "none" ? (
            <div className="bg-white rounded-3xl border border-zinc-200/80 p-12 text-center text-zinc-500 shadow-xs">
               {selectedPropertyId === "none"
                  ? t("noPropertiesDesc") ||
                    "You have no properties registered yet. Please register a property first."
                  : t("selectPropertyPrompt")}
            </div>
         ) : (
            <>
               {/* Stats Panel */}
               <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                  <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-xs flex items-center justify-between">
                     <div>
                        <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">
                           {t("totalCoupons")}
                        </p>
                        <h3 className="text-3xl font-extrabold text-zinc-900 mt-1">
                           {coupons.length}
                        </h3>
                     </div>
                     <div className="h-12 w-12 bg-blue-50 rounded-2xl flex items-center justify-center text-[#006ce4]">
                        <Ticket className="h-6 w-6" />
                     </div>
                  </div>

                  <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-xs flex items-center justify-between">
                     <div>
                        <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">
                           {t("activeCoupons")}
                        </p>
                        <h3 className="text-3xl font-extrabold text-emerald-600 mt-1">
                           {activeCount}
                        </h3>
                     </div>
                     <div className="h-12 w-12 bg-emerald-50 rounded-2xl flex items-center justify-center text-emerald-600">
                        <Check className="h-6 w-6" />
                     </div>
                  </div>

                  <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-xs flex items-center justify-between">
                     <div>
                        <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">
                           {t("totalUsed")}
                        </p>
                        <h3 className="text-3xl font-extrabold text-indigo-600 mt-1">
                           {totalRedeemed}
                        </h3>
                     </div>
                     <div className="h-12 w-12 bg-indigo-50 rounded-2xl flex items-center justify-center text-indigo-600">
                        <Calendar className="h-6 w-6" />
                     </div>
                  </div>

                  <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-xs flex items-center justify-between">
                     <div>
                        <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">
                           {t("totalReserved")}
                        </p>
                        <h3 className="text-3xl font-extrabold text-amber-600 mt-1">
                           {totalReserved}
                        </h3>
                     </div>
                     <div className="h-12 w-12 bg-amber-50 rounded-2xl flex items-center justify-center text-amber-500">
                        <Loader2 className="h-6 w-6 animate-pulse" />
                     </div>
                  </div>
               </div>

               {/* Table & List Card */}
               <div className="bg-white rounded-3xl border border-zinc-200/80 shadow-xs overflow-hidden">
                  <div className="p-6 border-b border-zinc-100 flex flex-col md:flex-row gap-4 items-stretch md:items-center justify-between">
                     <div className="relative flex-1 max-w-md">
                        <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4.5 w-4.5 text-zinc-400" />
                        <input
                           type="text"
                           placeholder={t("searchPlaceholder")}
                           value={searchTerm}
                           onChange={(e) => setSearchTerm(e.target.value)}
                           className="w-full pl-11 pr-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all placeholder:text-zinc-400 font-medium"
                        />
                     </div>
                  </div>

                  {loading ? (
                     <div className="flex flex-col items-center justify-center py-20">
                        <Loader2 className="h-10 w-10 text-[#006ce4] animate-spin mb-4" />
                        <p className="text-zinc-500 text-sm font-medium">{t("loadingCoupons")}</p>
                     </div>
                  ) : filteredCoupons.length === 0 ? (
                     <div className="flex flex-col items-center justify-center py-16 text-center">
                        <div className="h-16 w-16 bg-zinc-50 rounded-full flex items-center justify-center text-zinc-400 border border-zinc-200 mb-4">
                           <AlertCircle className="h-8 w-8" />
                        </div>
                        <h3 className="text-lg font-bold text-zinc-900">{t("noCouponsFound")}</h3>
                        <p className="max-w-sm text-xs text-zinc-500 mt-1 font-medium">
                           {t("noCouponsDesc")}
                        </p>
                     </div>
                  ) : (
                     <div className="overflow-x-auto">
                        <table className="w-full text-left border-collapse">
                           <thead>
                              <tr className="bg-zinc-50/75 border-b border-zinc-200 text-xs font-bold uppercase tracking-wider text-zinc-500">
                                 <th className="px-6 py-4">{t("couponCode")}</th>
                                 <th className="px-6 py-4">{t("discountType")}</th>
                                 <th className="px-6 py-4">{t("discountValue")}</th>
                                 <th className="px-6 py-4">{t("minAmount")}</th>
                                 <th className="px-6 py-4">{t("validity")}</th>
                                 <th className="px-6 py-4 text-center">{t("usageLimit")}</th>
                                 <th className="px-6 py-4 text-center">{t("usedCount")}</th>
                                 <th className="px-6 py-4 text-center">{t("status")}</th>
                                 <th className="px-6 py-4 text-right">{t("actions")}</th>
                              </tr>
                           </thead>
                           <tbody className="divide-y divide-zinc-100 text-sm font-medium text-zinc-700">
                              {filteredCoupons.map((coupon) => (
                                 <tr
                                    key={coupon.id}
                                    className="hover:bg-zinc-50/50 transition-colors"
                                 >
                                    <td className="px-6 py-4 font-black text-zinc-950">
                                       <span className="px-3 py-1.5 rounded-lg bg-blue-50 border border-blue-100 text-[#006ce4] text-xs font-mono uppercase tracking-wider">
                                          {coupon.code}
                                       </span>
                                    </td>
                                    <td className="px-6 py-4">
                                       {coupon.discountType === "PERCENTAGE" ? (
                                          <span className="flex items-center gap-1.5 text-indigo-600">
                                             <Percent className="h-4 w-4" />
                                             {t("percentage")}
                                          </span>
                                       ) : (
                                          <span className="flex items-center gap-1.5 text-[#003580]">
                                             <DollarSign className="h-4 w-4" />
                                             {t("fixedAmount")}
                                          </span>
                                       )}
                                    </td>
                                    <td className="px-6 py-4 font-bold text-zinc-900">
                                       {coupon.discountType === "PERCENTAGE"
                                          ? `${coupon.discountValue}%`
                                          : new Intl.NumberFormat("vi-VN", {
                                               style: "currency",
                                               currency: "VND",
                                            }).format(coupon.discountValue)}
                                    </td>
                                    <td className="px-6 py-4">
                                       {coupon.minBookingAmount
                                          ? new Intl.NumberFormat("vi-VN", {
                                               style: "currency",
                                               currency: "VND",
                                            }).format(coupon.minBookingAmount)
                                          : t("noMinAmount")}
                                    </td>
                                    <td className="px-6 py-4 text-xs font-semibold text-zinc-500">
                                       <div className="flex flex-col">
                                          <span>
                                             {t("from")}: {formatDate(coupon.validFrom)}
                                          </span>
                                          <span className="mt-0.5">
                                             {t("to")}: {formatDate(coupon.validUntil)}
                                          </span>
                                       </div>
                                    </td>
                                    <td className="px-6 py-4 text-center">
                                       {coupon.usageLimit !== null &&
                                       coupon.usageLimit !== undefined
                                          ? coupon.usageLimit
                                          : "∞"}
                                    </td>
                                    <td className="px-6 py-4 text-center">
                                       <div className="flex flex-col items-center">
                                          <span className="text-zinc-900 font-bold">
                                             {coupon.usedCount}
                                          </span>
                                          {coupon.reservedCount > 0 && (
                                             <span className="text-[10px] text-amber-600 bg-amber-50 border border-amber-100 px-1.5 py-0.5 rounded-full mt-0.5 font-bold">
                                                +{coupon.reservedCount} {t("reservedSmall")}
                                             </span>
                                          )}
                                       </div>
                                    </td>
                                    <td className="px-6 py-4 text-center">
                                       <button
                                          onClick={() => handleToggleActive(coupon)}
                                          className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                                             coupon.isActive ? "bg-emerald-500" : "bg-zinc-200"
                                          }`}
                                       >
                                          <span
                                             className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-sm ring-0 transition duration-200 ease-in-out ${
                                                coupon.isActive ? "translate-x-5" : "translate-x-0"
                                             }`}
                                          />
                                       </button>
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                       <div className="flex justify-end gap-2">
                                          <button
                                             onClick={() => handleOpenEditModal(coupon)}
                                             className="p-2 text-zinc-500 hover:text-blue-600 bg-zinc-50 hover:bg-blue-50 border border-zinc-100 hover:border-blue-100 rounded-xl transition-all cursor-pointer"
                                             title={t("edit")}
                                          >
                                             <Edit className="h-4 w-4" />
                                          </button>
                                          <button
                                             onClick={() => handleDelete(coupon.id)}
                                             className="p-2 text-zinc-500 hover:text-rose-600 bg-zinc-50 hover:bg-rose-50 border border-zinc-100 hover:border-rose-100 rounded-xl transition-all cursor-pointer"
                                             title={t("delete")}
                                          >
                                             <Trash2 className="h-4 w-4" />
                                          </button>
                                       </div>
                                    </td>
                                 </tr>
                              ))}
                           </tbody>
                        </table>
                     </div>
                  )}
               </div>
            </>
         )}

         {/* Create/Edit Modal */}
         {isModalOpen && (
            <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-xs">
               <div className="bg-white rounded-3xl max-w-lg w-full overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-200">
                  <div className="flex items-center justify-between p-6 border-b border-zinc-100">
                     <h2 className="text-xl font-bold text-zinc-900">
                        {editingCoupon ? t("editCouponTitle") : t("createCouponTitle")}
                     </h2>
                     <button
                        onClick={() => setIsModalOpen(false)}
                        className="p-2 hover:bg-zinc-50 border border-transparent hover:border-zinc-200 rounded-xl transition-all cursor-pointer text-zinc-400 hover:text-zinc-600"
                     >
                        <X className="h-5 w-5" />
                     </button>
                  </div>

                  <form onSubmit={handleSubmit} className="p-6 space-y-4">
                     <div>
                        <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                           {t("couponCode")} *
                        </label>
                        <input
                           type="text"
                           placeholder="E.g. SUMMER20"
                           value={code}
                           onChange={(e) => setCode(e.target.value.toUpperCase())}
                           disabled={!!editingCoupon}
                           className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all placeholder:text-zinc-400 font-bold uppercase tracking-wider disabled:bg-zinc-50 disabled:text-zinc-500"
                        />
                     </div>

                     <div className="grid grid-cols-2 gap-4">
                        <div>
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {t("discountType")} *
                           </label>
                           <select
                              value={discountType}
                              onChange={(e) => setDiscountType(e.target.value)}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:outline-none bg-white font-medium text-zinc-700 transition-colors"
                           >
                              <option value="FIXED_AMOUNT">{t("fixedAmount")}</option>
                              <option value="PERCENTAGE">{t("percentage")}</option>
                           </select>
                        </div>

                        <div>
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {t("discountValue")} *
                           </label>
                           <input
                              type="number"
                              min="0.01"
                              step="any"
                              placeholder={
                                 discountType === "PERCENTAGE" ? "E.g. 15" : "E.g. 200000"
                              }
                              value={discountValue}
                              onChange={(e) => setDiscountValue(e.target.value)}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all placeholder:text-zinc-400 font-medium"
                           />
                        </div>
                     </div>

                     <div className="grid grid-cols-2 gap-4">
                        <div>
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {t("minAmount")}
                           </label>
                           <input
                              type="number"
                              min="0"
                              placeholder="E.g. 1000000"
                              value={minBookingAmount}
                              onChange={(e) => setMinBookingAmount(e.target.value)}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all placeholder:text-zinc-400 font-medium"
                           />
                        </div>

                        <div>
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {discountType === "PERCENTAGE"
                                 ? `${t("maxDiscount")} (${t("optional")})`
                                 : t("notApplicable")}
                           </label>
                           <input
                              type="number"
                              min="0"
                              placeholder="E.g. 500000"
                              value={maxDiscountAmount}
                              onChange={(e) => setMaxDiscountAmount(e.target.value)}
                              disabled={discountType !== "PERCENTAGE"}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all placeholder:text-zinc-400 font-medium disabled:bg-zinc-50 disabled:placeholder:text-zinc-300"
                           />
                        </div>
                     </div>

                     <div className="grid grid-cols-2 gap-4">
                        <div>
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {t("validFrom")} *
                           </label>
                           <input
                              type="datetime-local"
                              value={validFrom}
                              onChange={(e) => setValidFrom(e.target.value)}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all font-medium text-zinc-700"
                           />
                        </div>

                        <div>
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {t("validUntil")} *
                           </label>
                           <input
                              type="datetime-local"
                              value={validUntil}
                              onChange={(e) => setValidUntil(e.target.value)}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all font-medium text-zinc-700"
                           />
                        </div>
                     </div>

                     <div className="grid grid-cols-2 gap-4">
                        <div>
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {t("usageLimit")}
                           </label>
                           <input
                              type="number"
                              min="1"
                              placeholder="Leave blank for infinite"
                              value={usageLimit}
                              onChange={(e) => setUsageLimit(e.target.value)}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all placeholder:text-zinc-400 font-medium"
                           />
                        </div>

                        <div className="flex items-center pt-6 pl-2">
                           <input
                              type="checkbox"
                              id="modal-isActive"
                              checked={isActive}
                              onChange={(e) => setIsActive(e.target.checked)}
                              className="h-4.5 w-4.5 rounded-md border-zinc-300 text-[#006ce4] focus:ring-[#006ce4]"
                           />
                           <label
                              htmlFor="modal-isActive"
                              className="ml-2 text-sm font-bold text-zinc-700 select-none cursor-pointer"
                           >
                              {t("isActiveLabel")}
                           </label>
                        </div>
                     </div>

                     <div className="flex justify-end gap-3 pt-4 border-t border-zinc-100">
                        <button
                           type="button"
                           onClick={() => setIsModalOpen(false)}
                           disabled={isSubmitting}
                           className="px-5 py-2.5 border border-zinc-200 rounded-2xl text-sm font-bold text-zinc-500 hover:bg-zinc-50 transition-colors cursor-pointer"
                        >
                           {t("cancel")}
                        </button>
                        <button
                           type="submit"
                           disabled={isSubmitting}
                           className="px-5 py-2.5 bg-[#006ce4] hover:bg-[#0057b7] text-white rounded-2xl text-sm font-bold shadow-md shadow-blue-100 hover:shadow-blue-200 transition-all flex items-center gap-2 cursor-pointer disabled:opacity-50"
                        >
                           {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : t("save")}
                        </button>
                     </div>
                  </form>
               </div>
            </div>
         )}
      </div>
   );
}
