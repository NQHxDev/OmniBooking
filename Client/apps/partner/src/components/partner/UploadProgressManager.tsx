"use client";

import { useCallback, useState, useEffect } from "react";
import { AnimatePresence } from "framer-motion";
import { useUploadProgressStore } from "@/store/useUploadProgressStore";
import { useMediaProgressSSE } from "@/hooks/useMediaProgressSSE";
import { UploadJob } from "@/types/media";
import UploadProgressWidget from "./UploadProgressWidget";

/** Connects SSE for a single active (non-terminal) job */
function SSEConnector({ propertyId }: { propertyId: string }) {
   useMediaProgressSSE(propertyId);
   return null;
}

/** Custom hook to measure the height of active Sonner toasts in the DOM */
function useToastHeight() {
   const [height, setHeight] = useState(0);

   useEffect(() => {
      const updateHeight = () => {
         const toaster = document.querySelector("[data-sonner-toaster]");
         if (!toaster) {
            setHeight(0);
            return;
         }

         // Get all mounted/visible toast elements
         const toasts = toaster.querySelectorAll("[data-mounted='true']");
         if (toasts.length === 0) {
            setHeight(0);
            return;
         }

         // Calculate the vertical span occupied by the toasts from the bottom of the viewport
         const windowHeight = window.innerHeight;
         let highestPoint = windowHeight;

         toasts.forEach((toast) => {
            const rect = toast.getBoundingClientRect();
            if (rect.top < highestPoint && rect.height > 0) {
               highestPoint = rect.top;
            }
         });

         const occupiedHeight = windowHeight - highestPoint;
         // Add 16px of spacing if there are visible toasts
         setHeight(occupiedHeight > 0 ? occupiedHeight + 16 : 0);
      };

      // Initial measurement
      updateHeight();

      // Observe document.body to detect when toasts are added, updated, or removed in the DOM tree
      const observer = new MutationObserver(updateHeight);
      observer.observe(document.body, {
         childList: true,
         subtree: true,
         attributes: true,
         attributeFilter: ["style", "class", "data-mounted"],
      });

      return () => {
         observer.disconnect();
      };
   }, []);

   return height;
}

export default function UploadProgressManager() {
   const jobs = useUploadProgressStore((s) => s.jobs);
   const dismissJob = useUploadProgressStore((s) => s.dismissJob);
   const toastHeight = useToastHeight();

   const visibleJobs = Object.values(jobs).filter((job) => !job.dismissed);

   const activeJobIds = visibleJobs
      .filter((job) => job.status === "PROCESSING" || job.status === "STALLED")
      .map((job) => job.propertyId);

   const handleDismiss = useCallback(
      (propertyId: string) => {
         dismissJob(propertyId);
      },
      [dismissJob]
   );

   return (
      <>
         {/* SSE connections for active jobs */}
         {activeJobIds.map((id) => (
            <SSEConnector key={id} propertyId={id} />
         ))}

         {/* Render widgets */}
         <AnimatePresence mode="popLayout">
            {visibleJobs.map((job: UploadJob, index: number) => (
               <UploadProgressWidget
                  key={job.propertyId}
                  job={job}
                  index={index}
                  bottomOffset={toastHeight}
                  onDismiss={() => handleDismiss(job.propertyId)}
               />
            ))}
         </AnimatePresence>
      </>
   );
}
