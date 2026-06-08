"use client";

import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api/apiClient";
import { useSettingStore } from "@/store/useSettingStore";
import { useLocale } from "next-intl";
import { Skeleton } from "./ui/skeleton";

interface PriceDisplayProps {
   amount: number; // Base amount in USD
   className?: string;
   size?: "sm" | "md" | "lg" | "xl" | "custom";
}

export default function PriceDisplay({ amount, className = "", size = "md" }: PriceDisplayProps) {
   const { currency: targetCurrency } = useSettingStore();
   const locale = useLocale();

   // Fetch rates from backend
   const { data: rates, isLoading } = useQuery({
      queryKey: ["currency-rates"],
      queryFn: async () => {
         const response = await apiClient.get<unknown, Record<string, number>>("/currencies/rates");
         return response;
      },
      staleTime: 1000 * 60 * 60, // 1 hour
   });

   if (isLoading) {
      return <Skeleton className="h-6 w-20 inline-block" />;
   }

   const rate = rates?.[targetCurrency] || 1;
   let convertedAmount = amount * rate;
   if (targetCurrency === "VND") {
      convertedAmount = Math.round(convertedAmount / 1000) * 1000;
   }

   const sizeClasses = {
      sm: "text-sm",
      md: "text-base font-bold",
      lg: "text-xl font-bold",
      xl: "text-2xl font-extrabold",
      custom: "",
   };

   if (targetCurrency === "VND") {
      const numberFormatter = new Intl.NumberFormat("vi-VN", {
         style: "decimal",
         minimumFractionDigits: 0,
         maximumFractionDigits: 0,
      });
      return (
         <span className={`${sizeClasses[size]} ${className} transition-all duration-300`}>
            VND {numberFormatter.format(convertedAmount)}
         </span>
      );
   }

   const localeMap: Record<string, string> = {
      vi: "vi-VN",
      en: "en-US",
   };
   const formatter = new Intl.NumberFormat(localeMap[locale] || locale, {
      style: "currency",
      currency: targetCurrency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
   });

   return (
      <span className={`${sizeClasses[size]} ${className} transition-all duration-300`}>
         {formatter.format(convertedAmount)}
      </span>
   );
}
