"use client";

import { useCallback } from "react";
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

export default function UploadProgressManager() {
   const jobs = useUploadProgressStore((s) => s.jobs);
   const dismissJob = useUploadProgressStore((s) => s.dismissJob);

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
               <div key={job.propertyId} style={{ transform: `translateY(-${index * 8}px)` }}>
                  <UploadProgressWidget job={job} onDismiss={() => handleDismiss(job.propertyId)} />
               </div>
            ))}
         </AnimatePresence>
      </>
   );
}
