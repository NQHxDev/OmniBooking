"use client";

import { useState, useEffect, useCallback } from "react";
import { motion } from "framer-motion";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { X, Minimize2, CheckCircle2, AlertTriangle, ImageIcon, Loader2 } from "lucide-react";
import { UploadJob } from "@/types/media";

interface UploadProgressWidgetProps {
   job: UploadJob;
   onDismiss: () => void;
   bottomOffset?: number;
   index?: number;
}

export default function UploadProgressWidget({
   job,
   onDismiss,
   bottomOffset = 0,
   index = 0,
}: UploadProgressWidgetProps) {
   const t = useTranslations("Partner.uploadProgress");
   const queryClient = useQueryClient();
   const router = useRouter();
   const [minimized, setMinimized] = useState(false);
   const [elapsedSeconds, setElapsedSeconds] = useState(0);
   const [displayedProcessed, setDisplayedProcessed] = useState(0);

   const isTerminal =
      job.status === "COMPLETED" || job.status === "PARTIAL_SUCCESS" || job.status === "FAILED";

   // Easing logic for processing counter
   useEffect(() => {
      if (job.processed > displayedProcessed) {
         const isDone = job.status === "COMPLETED" || job.status === "PARTIAL_SUCCESS";
         const delay = isDone ? 150 : 300; // Increment faster if complete to wrap up nicely
         const timer = setTimeout(() => {
            setDisplayedProcessed((prev) => Math.min(prev + 1, job.processed));
         }, delay);
         return () => clearTimeout(timer);
      } else if (job.processed < displayedProcessed) {
         const timer = setTimeout(() => {
            setDisplayedProcessed(job.processed);
         }, 0);
         return () => clearTimeout(timer);
      }
   }, [job.processed, displayedProcessed, job.status]);

   // Calculate percentage based on smooth displayed count
   const displayedPercentage =
      job.total > 0
         ? Math.min(100, Math.round((displayedProcessed / job.total) * 100))
         : job.percentage;

   // Elapsed time tracker - freezes when terminal
   useEffect(() => {
      if (isTerminal) {
         const endTime = job.lastUpdatedAt || Date.now();
         const seconds = Math.max(0, Math.floor((endTime - job.connectedAt) / 1000));
         const timer = setTimeout(() => {
            setElapsedSeconds(seconds);
         }, 0);
         return () => clearTimeout(timer);
      }

      const interval = setInterval(() => {
         setElapsedSeconds(Math.floor((Date.now() - job.connectedAt) / 1000));
      }, 1000);
      return () => clearInterval(interval);
   }, [job.connectedAt, isTerminal, job.lastUpdatedAt]);

   // Auto-dismiss on COMPLETED after 5s
   useEffect(() => {
      if (job.status === "COMPLETED") {
         const timer = setTimeout(onDismiss, 5000);
         return () => clearTimeout(timer);
      }
   }, [job.status, onDismiss]);

   // Invalidate incomplete uploads query and refresh page data when processing finishes
   useEffect(() => {
      if (job.status === "COMPLETED" || job.status === "PARTIAL_SUCCESS") {
         queryClient.invalidateQueries({ queryKey: ["incomplete-uploads"] });
         router.refresh();
      }
   }, [job.status, queryClient, router]);

   const formatElapsed = useCallback((seconds: number) => {
      if (seconds < 60) return `${seconds}s`;
      const mins = Math.floor(seconds / 60);
      const secs = seconds % 60;
      return `${mins}m ${secs}s`;
   }, []);

   // Minimized state - compact icon
   if (minimized) {
      return (
         <motion.div
            layout
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.8, opacity: 0 }}
            style={{ bottom: `${16 + bottomOffset + index * 8}px` }}
            className="fixed right-4 z-50"
         >
            <button
               onClick={() => setMinimized(false)}
               className="relative flex h-14 w-14 items-center justify-center rounded-full bg-white/90 dark:bg-gray-900/85 backdrop-blur-xl shadow-2xl border border-gray-200/50 dark:border-white/10 transition-transform hover:scale-105"
            >
               <ImageIcon className="h-6 w-6 text-gray-700 dark:text-white" />
               {!isTerminal && (
                  <span className="absolute -top-1 -right-1 flex h-5 w-5">
                     <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-blue-400 opacity-75" />
                     <span className="relative inline-flex h-5 w-5 items-center justify-center rounded-full bg-blue-500 text-[10px] font-bold text-white">
                        {displayedPercentage}%
                     </span>
                  </span>
               )}
               {job.status === "COMPLETED" && (
                  <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-emerald-500">
                     <CheckCircle2 className="h-3 w-3 text-white" />
                  </span>
               )}
               {(job.status === "FAILED" || job.status === "PARTIAL_SUCCESS") && (
                  <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-amber-500">
                     <AlertTriangle className="h-3 w-3 text-white" />
                  </span>
               )}
            </button>
         </motion.div>
      );
   }

   const statusConfig = getStatusConfig(job.status);

   return (
      <motion.div
         layout
         initial={{ y: 100, opacity: 0, scale: 0.95 }}
         animate={{ y: 0, opacity: 1, scale: 1 }}
         exit={{ y: 100, opacity: 0, scale: 0.95 }}
         transition={{ type: "spring", damping: 25, stiffness: 300 }}
         style={{ bottom: `${16 + bottomOffset + index * 8}px` }}
         className="fixed right-4 z-50 w-96 rounded-2xl bg-white/90 dark:bg-gray-900/85 backdrop-blur-xl shadow-2xl border border-gray-200/80 dark:border-white/10 overflow-hidden"
      >
         {/* Header */}
         <div className="flex items-center justify-between px-4 pt-4 pb-2">
            <div className="flex items-center gap-2.5">
               <div
                  className={`flex h-8 w-8 items-center justify-center rounded-lg ${statusConfig.iconBg}`}
               >
                  {statusConfig.icon}
               </div>
               <div>
                  <h4 className="text-sm font-semibold text-gray-900 dark:text-white leading-tight">
                     {t(statusConfig.titleKey)}
                  </h4>
                  <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-[200px]">
                     {job.propertyName}
                  </p>
               </div>
            </div>
            <div className="flex items-center gap-1">
               <button
                  onClick={() => setMinimized(true)}
                  className="flex h-7 w-7 items-center justify-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 dark:hover:bg-white/10 hover:text-gray-950 dark:hover:text-white"
               >
                  <Minimize2 className="h-3.5 w-3.5" />
               </button>
               {isTerminal && (
                  <button
                     onClick={onDismiss}
                     className="flex h-7 w-7 items-center justify-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 dark:hover:bg-white/10 hover:text-gray-950 dark:hover:text-white"
                  >
                     <X className="h-3.5 w-3.5" />
                  </button>
               )}
            </div>
         </div>

         {/* Progress bar */}
         <div className="px-4 py-2">
            <div className="h-2 w-full overflow-hidden rounded-full bg-gray-100 dark:bg-white/10">
               <div
                  className={`h-full rounded-full transition-all duration-600 ease-out ${statusConfig.barClass}`}
                  style={{ width: `${Math.max(displayedPercentage, 2)}%` }}
               />
            </div>
         </div>

         {/* Details */}
         <div className="flex items-center justify-between px-4 pb-4">
            <div className="flex flex-col gap-0.5">
               {job.status === "COMPLETED" ? (
                  <span className="text-xs text-emerald-600 dark:text-emerald-400 font-medium">
                     {t("completedIn", { time: elapsedSeconds.toString() })}
                  </span>
               ) : job.status === "FAILED" ? (
                  <span className="text-xs text-red-600 dark:text-red-400 font-medium">
                     {t("failed")}
                  </span>
               ) : job.status === "PARTIAL_SUCCESS" ? (
                  <span className="text-xs text-amber-600 dark:text-amber-400 font-medium">
                     {t("imagesFailed", {
                        success: (job.processed - job.failed).toString(),
                        failed: job.failed.toString(),
                     })}
                  </span>
               ) : (
                  <span className="text-xs text-gray-600 dark:text-gray-300">
                     {t("imagesProcessed", {
                        processed: displayedProcessed.toString(),
                        total: job.total.toString(),
                     })}
                  </span>
               )}
               {!isTerminal && (
                  <span className="text-[10px] text-gray-400 dark:text-gray-500">
                     {formatElapsed(elapsedSeconds)}
                  </span>
               )}
            </div>
            <span className="text-lg font-bold tabular-nums text-gray-900 dark:text-white">
               {displayedPercentage}%
            </span>
         </div>
      </motion.div>
   );
}

function getStatusConfig(status: UploadJob["status"]) {
   switch (status) {
      case "PROCESSING":
         return {
            titleKey: "processing" as const,
            iconBg: "bg-blue-500/20",
            icon: <Loader2 className="h-4 w-4 text-blue-400 animate-spin" />,
            barClass:
               "bg-gradient-to-r from-blue-500 via-blue-400 to-blue-500 animate-shimmer bg-[length:200%_100%]",
         };
      case "STALLED":
         return {
            titleKey: "stalled" as const,
            iconBg: "bg-amber-500/20",
            icon: <Loader2 className="h-4 w-4 text-amber-400 animate-spin" />,
            barClass: "bg-amber-500 animate-pulse",
         };
      case "COMPLETED":
         return {
            titleKey: "completed" as const,
            iconBg: "bg-emerald-500/20",
            icon: <CheckCircle2 className="h-4 w-4 text-emerald-400" />,
            barClass: "bg-emerald-500",
         };
      case "PARTIAL_SUCCESS":
         return {
            titleKey: "partialSuccess" as const,
            iconBg: "bg-amber-500/20",
            icon: <AlertTriangle className="h-4 w-4 text-amber-400" />,
            barClass: "bg-amber-500",
         };
      case "FAILED":
         return {
            titleKey: "failed" as const,
            iconBg: "bg-red-500/20",
            icon: <AlertTriangle className="h-4 w-4 text-red-400" />,
            barClass: "bg-red-500",
         };
   }
}
