"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useTranslations } from "next-intl";
import {
   priceRuleService,
   PriceRuleResponse,
   PriceRuleRequest,
   PropertyResponse,
   RoomTypeResponse,
} from "@omnibooking/shared";
import { propertyService } from "@/lib/api/propertyService";
import {
   Search,
   Plus,
   Trash2,
   Edit,
   X,
   Loader2,
   AlertCircle,
   Building2,
   Check,
   Calendar,
   TrendingUp,
   Users,
   Info,
} from "lucide-react";
import { toast } from "sonner";
import { format } from "date-fns";

interface PartnerPricingRulesProps {
   initialProperties: PropertyResponse[];
}

export default function PartnerPricingRules({ initialProperties }: PartnerPricingRulesProps) {
   const t = useTranslations("Partner.pricingRules");
   const [selectedPropertyId, setSelectedPropertyId] = useState<string>(
      initialProperties.length > 0 ? initialProperties[0].id : "none"
   );
   const [rules, setRules] = useState<PriceRuleResponse[]>([]);
   const [roomTypes, setRoomTypes] = useState<RoomTypeResponse[]>([]);
   const [loading, setLoading] = useState(false);
   const [searchTerm, setSearchTerm] = useState("");

   // Modal State
   const [isModalOpen, setIsModalOpen] = useState(false);
   const [editingRule, setEditingRule] = useState<PriceRuleResponse | null>(null);
   const [isSubmitting, setIsSubmitting] = useState(false);

   // Form State
   const [name, setName] = useState("");
   const [ruleType, setRuleType] = useState("SEASONAL");
   const [selectedRoomTypeId, setSelectedRoomTypeId] = useState("");
   const [adjustmentType, setAdjustmentType] = useState("PERCENTAGE");
   const [adjustmentValue, setAdjustmentValue] = useState("");
   const [startDate, setStartDate] = useState("");
   const [endDate, setEndDate] = useState("");
   const [occupancyThreshold, setOccupancyThreshold] = useState("");
   const [priority, setPriority] = useState("0");
   const [isActive, setIsActive] = useState(true);

   // Fetch rules & room types
   const fetchData = useCallback(async () => {
      if (selectedPropertyId === "all" || selectedPropertyId === "none") {
         setRules([]);
         setRoomTypes([]);
         return;
      }
      try {
         setLoading(true);
         const [rulesRes, detailRes] = await Promise.all([
            priceRuleService.getByProperty(selectedPropertyId),
            propertyService.getPropertyDetail(selectedPropertyId),
         ]);
         setRules(rulesRes || []);
         setRoomTypes(detailRes?.roomTypes || []);
      } catch (err) {
         console.error("Error fetching rules data:", err);
         toast.error(t("errorFetch"));
      } finally {
         setLoading(false);
      }
   }, [selectedPropertyId, t]);

   useEffect(() => {
      const timer = setTimeout(() => {
         fetchData();
      }, 0);
      return () => clearTimeout(timer);
   }, [fetchData]);

   // Handle open create modal
   const handleOpenCreateModal = () => {
      setEditingRule(null);
      setName("");
      setRuleType("SEASONAL");
      setSelectedRoomTypeId("");
      setAdjustmentType("PERCENTAGE");
      setAdjustmentValue("");

      const now = new Date();
      const thirtyDaysLater = new Date();
      thirtyDaysLater.setDate(now.getDate() + 30);

      setStartDate(format(now, "yyyy-MM-dd"));
      setEndDate(format(thirtyDaysLater, "yyyy-MM-dd"));
      setOccupancyThreshold("");
      setPriority("0");
      setIsActive(true);
      setIsModalOpen(true);
   };

   // Handle open edit modal
   const handleOpenEditModal = (rule: PriceRuleResponse) => {
      setEditingRule(rule);
      setName(rule.name);
      setRuleType(rule.ruleType);
      setSelectedRoomTypeId(rule.roomTypeId || "");
      setAdjustmentType(rule.adjustmentType);
      setAdjustmentValue(rule.adjustmentValue.toString());

      setStartDate(rule.startDate ? rule.startDate : "");
      setEndDate(rule.endDate ? rule.endDate : "");
      setOccupancyThreshold(rule.occupancyThreshold ? rule.occupancyThreshold.toString() : "");
      setPriority(rule.priority.toString());
      setIsActive(rule.isActive);
      setIsModalOpen(true);
   };

   // Validate and submit
   const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      if (!name.trim()) {
         toast.error(t("errorNameRequired"));
         return;
      }
      if (!adjustmentValue || parseFloat(adjustmentValue) === 0) {
         toast.error(t("errorAdjustmentValueInvalid"));
         return;
      }

      let startD: string | undefined = undefined;
      let endD: string | undefined = undefined;
      let threshold: number | undefined = undefined;

      if (ruleType === "SEASONAL") {
         if (!startDate || !endDate) {
            toast.error(t("errorDatesRequired"));
            return;
         }
         if (new Date(endDate) < new Date(startDate)) {
            toast.error(t("errorEndDateBeforeStartDate"));
            return;
         }
         startD = startDate;
         endD = endDate;
      }

      if (ruleType === "OCCUPANCY") {
         if (!occupancyThreshold || parseInt(occupancyThreshold) <= 0) {
            toast.error(t("errorOccupancyThresholdInvalid"));
            return;
         }
         threshold = parseInt(occupancyThreshold);
      }

      const req: PriceRuleRequest = {
         propertyId: selectedPropertyId,
         roomTypeId: selectedRoomTypeId || undefined,
         name: name.trim(),
         ruleType,
         startDate: startD,
         endDate: endD,
         adjustmentType,
         adjustmentValue: parseFloat(adjustmentValue),
         occupancyThreshold: threshold,
         priority: parseInt(priority) || 0,
         isActive,
      };

      try {
         setIsSubmitting(true);
         if (editingRule) {
            const updated = await priceRuleService.update(editingRule.id, req);
            setRules((prev) => prev.map((r) => (r.id === editingRule.id ? updated : r)));
            toast.success(t("successUpdate"));
         } else {
            const created = await priceRuleService.create(req);
            setRules((prev) => [created, ...prev]);
            toast.success(t("successCreate"));
         }
         setIsModalOpen(false);
      } catch (err) {
         console.error("Error saving rule:", err);
         toast.error(t("errorSave"));
      } finally {
         setIsSubmitting(false);
      }
   };

   // Delete Rule
   const handleDelete = async (id: string) => {
      if (!confirm(t("confirmDelete"))) return;
      try {
         await priceRuleService.delete(id);
         setRules((prev) => prev.filter((r) => r.id !== id));
         toast.success(t("successDelete"));
      } catch (err) {
         console.error("Error deleting rule:", err);
         toast.error(t("errorDelete"));
      }
   };

   // Toggle rule active state directly
   const handleToggleActive = async (rule: PriceRuleResponse) => {
      const req: PriceRuleRequest = {
         propertyId: selectedPropertyId,
         roomTypeId: rule.roomTypeId || undefined,
         name: rule.name,
         ruleType: rule.ruleType,
         startDate: rule.startDate,
         endDate: rule.endDate,
         adjustmentType: rule.adjustmentType,
         adjustmentValue: rule.adjustmentValue,
         occupancyThreshold: rule.occupancyThreshold || undefined,
         priority: rule.priority,
         isActive: !rule.isActive,
      };
      try {
         const updated = await priceRuleService.update(rule.id, req);
         setRules((prev) => prev.map((r) => (r.id === rule.id ? updated : r)));
         toast.success(updated.isActive ? t("successActivated") : t("successDeactivated"));
      } catch (err) {
         console.error("Error toggling active state:", err);
         toast.error(t("errorUpdate"));
      }
   };

   // Filters
   const filteredRules = useMemo(() => {
      return rules.filter((r) => {
         if (searchTerm.trim() !== "") {
            return r.name.toLowerCase().includes(searchTerm.toLowerCase());
         }
         return true;
      });
   }, [rules, searchTerm]);

   // Stats
   const seasonalCount = useMemo(
      () => rules.filter((r) => r.ruleType === "SEASONAL").length,
      [rules]
   );
   const weekendCount = useMemo(
      () => rules.filter((r) => r.ruleType === "WEEKEND").length,
      [rules]
   );
   const occupancyCount = useMemo(
      () => rules.filter((r) => r.ruleType === "OCCUPANCY").length,
      [rules]
   );
   const activeCount = useMemo(() => rules.filter((r) => r.isActive).length, [rules]);

   const getRoomTypeName = (roomTypeId?: string) => {
      if (!roomTypeId) return t("allRoomTypes");
      const matched = roomTypes.find((rt) => rt.id === roomTypeId);
      return matched ? matched.name : t("unknownRoomType");
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
                  {t("createRule")}
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
                           {t("seasonalRules")}
                        </p>
                        <h3 className="text-3xl font-extrabold text-zinc-900 mt-1">
                           {seasonalCount}
                        </h3>
                     </div>
                     <div className="h-12 w-12 bg-amber-50 rounded-2xl flex items-center justify-center text-amber-600">
                        <Calendar className="h-6 w-6" />
                     </div>
                  </div>

                  <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-xs flex items-center justify-between">
                     <div>
                        <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">
                           {t("weekendRules")}
                        </p>
                        <h3 className="text-3xl font-extrabold text-indigo-600 mt-1">
                           {weekendCount}
                        </h3>
                     </div>
                     <div className="h-12 w-12 bg-indigo-50 rounded-2xl flex items-center justify-center text-indigo-600">
                        <TrendingUp className="h-6 w-6" />
                     </div>
                  </div>

                  <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-xs flex items-center justify-between">
                     <div>
                        <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">
                           {t("occupancyRules")}
                        </p>
                        <h3 className="text-3xl font-extrabold text-rose-600 mt-1">
                           {occupancyCount}
                        </h3>
                     </div>
                     <div className="h-12 w-12 bg-rose-50 rounded-2xl flex items-center justify-center text-rose-600">
                        <Users className="h-6 w-6" />
                     </div>
                  </div>

                  <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-xs flex items-center justify-between">
                     <div>
                        <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider">
                           {t("activeRules")}
                        </p>
                        <h3 className="text-3xl font-extrabold text-emerald-600 mt-1">
                           {activeCount}
                        </h3>
                     </div>
                     <div className="h-12 w-12 bg-emerald-50 rounded-2xl flex items-center justify-center text-emerald-600">
                        <Check className="h-6 w-6" />
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
                        <p className="text-zinc-500 text-sm font-medium">{t("loadingRules")}</p>
                     </div>
                  ) : filteredRules.length === 0 ? (
                     <div className="flex flex-col items-center justify-center py-16 text-center">
                        <div className="h-16 w-16 bg-zinc-50 rounded-full flex items-center justify-center text-zinc-400 border border-zinc-200 mb-4">
                           <AlertCircle className="h-8 w-8" />
                        </div>
                        <h3 className="text-lg font-bold text-zinc-900">{t("noRulesFound")}</h3>
                        <p className="max-w-sm text-xs text-zinc-500 mt-1 font-medium">
                           {t("noRulesDesc")}
                        </p>
                     </div>
                  ) : (
                     <div className="overflow-x-auto">
                        <table className="w-full text-left border-collapse">
                           <thead>
                              <tr className="bg-zinc-50/75 border-b border-zinc-200 text-xs font-bold uppercase tracking-wider text-zinc-500">
                                 <th className="px-6 py-4">{t("ruleName")}</th>
                                 <th className="px-6 py-4">{t("ruleType")}</th>
                                 <th className="px-6 py-4">{t("roomType")}</th>
                                 <th className="px-6 py-4 text-center">{t("priority")}</th>
                                 <th className="px-6 py-4">{t("adjustment")}</th>
                                 <th className="px-6 py-4">{t("ruleDetails")}</th>
                                 <th className="px-6 py-4 text-center">{t("status")}</th>
                                 <th className="px-6 py-4 text-right">{t("actions")}</th>
                              </tr>
                           </thead>
                           <tbody className="divide-y divide-zinc-100 text-sm font-medium text-zinc-700">
                              {filteredRules.map((rule) => (
                                 <tr
                                    key={rule.id}
                                    className="hover:bg-zinc-50/50 transition-colors"
                                 >
                                    <td className="px-6 py-4 font-bold text-zinc-900">
                                       {rule.name}
                                    </td>
                                    <td className="px-6 py-4">
                                       {rule.ruleType === "SEASONAL" && (
                                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200/50">
                                             <Calendar className="h-3 w-3" />
                                             {t("seasonal")}
                                          </span>
                                       )}
                                       {rule.ruleType === "WEEKEND" && (
                                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200/50">
                                             <TrendingUp className="h-3 w-3" />
                                             {t("weekend")}
                                          </span>
                                       )}
                                       {rule.ruleType === "OCCUPANCY" && (
                                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200/50">
                                             <Users className="h-3 w-3" />
                                             {t("occupancy")}
                                          </span>
                                       )}
                                    </td>
                                    <td className="px-6 py-4 text-xs font-bold text-zinc-600">
                                       {getRoomTypeName(rule.roomTypeId)}
                                    </td>
                                    <td className="px-6 py-4 text-center font-bold text-zinc-900">
                                       {rule.priority}
                                    </td>
                                    <td className="px-6 py-4">
                                       <span
                                          className={`font-black ${rule.adjustmentValue >= 0 ? "text-emerald-600" : "text-rose-600"}`}
                                       >
                                          {rule.adjustmentValue >= 0 ? "+" : ""}
                                          {rule.adjustmentType === "PERCENTAGE"
                                             ? `${rule.adjustmentValue}%`
                                             : new Intl.NumberFormat("vi-VN", {
                                                  style: "currency",
                                                  currency: "VND",
                                               }).format(rule.adjustmentValue)}
                                       </span>
                                    </td>
                                    <td className="px-6 py-4 text-xs text-zinc-500 font-semibold">
                                       {rule.ruleType === "SEASONAL" &&
                                          rule.startDate &&
                                          rule.endDate && (
                                             <span>
                                                {format(new Date(rule.startDate), "dd/MM/yyyy")} -{" "}
                                                {format(new Date(rule.endDate), "dd/MM/yyyy")}
                                             </span>
                                          )}
                                       {rule.ruleType === "WEEKEND" && (
                                          <span>T6, T7, CN hàng tuần</span>
                                       )}
                                       {rule.ruleType === "OCCUPANCY" &&
                                          rule.occupancyThreshold && (
                                             <span className="flex items-center gap-1">
                                                <Users className="h-3 w-3" />
                                                &gt;= {rule.occupancyThreshold} khách
                                             </span>
                                          )}
                                    </td>
                                    <td className="px-6 py-4 text-center">
                                       <button
                                          onClick={() => handleToggleActive(rule)}
                                          className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                                             rule.isActive ? "bg-emerald-500" : "bg-zinc-200"
                                          }`}
                                       >
                                          <span
                                             className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-sm ring-0 transition duration-200 ease-in-out ${
                                                rule.isActive ? "translate-x-5" : "translate-x-0"
                                             }`}
                                          />
                                       </button>
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                       <div className="flex justify-end gap-2">
                                          <button
                                             onClick={() => handleOpenEditModal(rule)}
                                             className="p-2 text-zinc-500 hover:text-blue-600 bg-zinc-50 hover:bg-blue-50 border border-zinc-100 hover:border-blue-100 rounded-xl transition-all cursor-pointer"
                                             title={t("edit")}
                                          >
                                             <Edit className="h-4 w-4" />
                                          </button>
                                          <button
                                             onClick={() => handleDelete(rule.id)}
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
                        {editingRule ? t("editRuleTitle") : t("createRuleTitle")}
                     </h2>
                     <button
                        onClick={() => setIsModalOpen(false)}
                        className="p-2 hover:bg-zinc-50 border border-transparent hover:border-zinc-200 rounded-xl transition-all cursor-pointer text-zinc-400 hover:text-zinc-600"
                     >
                        <X className="h-5 w-5" />
                     </button>
                  </div>

                  <form
                     onSubmit={handleSubmit}
                     className="p-6 space-y-4 max-h-[75vh] overflow-y-auto"
                  >
                     <div>
                        <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                           {t("ruleName")} *
                        </label>
                        <input
                           type="text"
                           placeholder="E.g. Peak Season Surcharge"
                           value={name}
                           onChange={(e) => setName(e.target.value)}
                           className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all placeholder:text-zinc-400 font-medium"
                        />
                     </div>

                     <div className="grid grid-cols-2 gap-4">
                        <div>
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {t("ruleType")} *
                           </label>
                           <select
                              value={ruleType}
                              onChange={(e) => setRuleType(e.target.value)}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:outline-none bg-white font-medium text-zinc-700 transition-colors"
                           >
                              <option value="SEASONAL">{t("seasonal")}</option>
                              <option value="WEEKEND">{t("weekend")}</option>
                              <option value="OCCUPANCY">{t("occupancy")}</option>
                           </select>
                        </div>

                        <div>
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {t("roomTypeScope")}
                           </label>
                           <select
                              value={selectedRoomTypeId}
                              onChange={(e) => setSelectedRoomTypeId(e.target.value)}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:outline-none bg-white font-medium text-zinc-700 transition-colors"
                           >
                              <option value="">{t("allRoomTypes")}</option>
                              {roomTypes.map((rt) => (
                                 <option key={rt.id} value={rt.id}>
                                    {rt.name}
                                 </option>
                              ))}
                           </select>
                        </div>
                     </div>

                     <div className="grid grid-cols-2 gap-4">
                        <div>
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {t("adjustmentType")} *
                           </label>
                           <select
                              value={adjustmentType}
                              onChange={(e) => setAdjustmentType(e.target.value)}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:outline-none bg-white font-medium text-zinc-700 transition-colors"
                           >
                              <option value="PERCENTAGE">{t("percentage")}</option>
                              <option value="FIXED_AMOUNT">{t("fixedAmount")}</option>
                           </select>
                        </div>

                        <div>
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {t("adjustmentValue")} *
                           </label>
                           <input
                              type="number"
                              step="any"
                              placeholder={
                                 adjustmentType === "PERCENTAGE"
                                    ? "E.g. +10 or -15"
                                    : "E.g. +200000 or -150000"
                              }
                              value={adjustmentValue}
                              onChange={(e) => setAdjustmentValue(e.target.value)}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all placeholder:text-zinc-400 font-medium font-mono"
                           />
                           <p className="text-[10px] text-zinc-400 mt-1 flex items-center gap-1 font-medium">
                              <Info className="h-3 w-3 shrink-0" />
                              {t("adjustmentValueHint")}
                           </p>
                        </div>
                     </div>

                     {/* Seasonal Rule Dates */}
                     {ruleType === "SEASONAL" && (
                        <div className="grid grid-cols-2 gap-4 border border-zinc-100 rounded-2xl p-4.5 bg-zinc-50/50">
                           <div>
                              <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                                 {t("startDate")} *
                              </label>
                              <input
                                 type="date"
                                 value={startDate}
                                 onChange={(e) => setStartDate(e.target.value)}
                                 className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:outline-none bg-white font-medium text-zinc-700"
                              />
                           </div>

                           <div>
                              <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                                 {t("endDate")} *
                              </label>
                              <input
                                 type="date"
                                 value={endDate}
                                 onChange={(e) => setEndDate(e.target.value)}
                                 className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:outline-none bg-white font-medium text-zinc-700"
                              />
                           </div>
                        </div>
                     )}

                     {/* Occupancy Rule Threshold */}
                     {ruleType === "OCCUPANCY" && (
                        <div className="border border-zinc-100 rounded-2xl p-4.5 bg-zinc-50/50">
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {t("occupancyThreshold")} *
                           </label>
                           <input
                              type="number"
                              min="1"
                              placeholder="E.g. 3 (applied when guests count >= 3)"
                              value={occupancyThreshold}
                              onChange={(e) => setOccupancyThreshold(e.target.value)}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all placeholder:text-zinc-400 font-medium"
                           />
                        </div>
                     )}

                     <div className="grid grid-cols-2 gap-4">
                        <div>
                           <label className="block text-xs font-bold uppercase tracking-wider text-zinc-500 mb-1.5">
                              {t("priority")} (0 = {t("lowest")})
                           </label>
                           <input
                              type="number"
                              min="0"
                              value={priority}
                              onChange={(e) => setPriority(e.target.value)}
                              className="w-full px-4 py-2.5 rounded-2xl border border-zinc-200 text-sm focus:border-[#006ce4] focus:ring-1 focus:ring-[#006ce4] focus:outline-none transition-all font-medium"
                           />
                        </div>

                        <div className="flex items-center pt-6 pl-2">
                           <input
                              type="checkbox"
                              id="modal-isActive-rule"
                              checked={isActive}
                              onChange={(e) => setIsActive(e.target.checked)}
                              className="h-4.5 w-4.5 rounded-md border-zinc-300 text-[#006ce4] focus:ring-[#006ce4]"
                           />
                           <label
                              htmlFor="modal-isActive-rule"
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
