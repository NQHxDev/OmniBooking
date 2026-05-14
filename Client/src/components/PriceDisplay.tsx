"use client";

import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api/apiClient";
import { useSettingStore } from "@/store/useSettingStore";
import { useLocale } from "next-intl";
import { Skeleton } from "./ui/skeleton";

interface PriceDisplayProps {
   amount: number; // Base amount in USD
   className?: string;
   size?: "sm" | "md" | "lg" | "xl";
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
   const convertedAmount = amount * rate;

   const formatter = new Intl.NumberFormat(locale === "vi" ? "vi-VN" : "en-US", {
      style: "currency",
      currency: targetCurrency,
      minimumFractionDigits: targetCurrency === "VND" ? 0 : 2,
      maximumFractionDigits: targetCurrency === "VND" ? 0 : 2,
   });

   const sizeClasses = {
      sm: "text-sm",
      md: "text-base font-bold",
      lg: "text-xl font-bold",
      xl: "text-2xl font-extrabold",
   };

   return (
      <span className={`${sizeClasses[size]} ${className} transition-all duration-300`}>
         {formatter.format(convertedAmount)}
      </span>
   );
}
