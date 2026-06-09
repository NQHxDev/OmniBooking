import { useEffect, useRef } from "react";
import { useUploadProgressStore } from "../store/useUploadProgressStore";
import { MediaProgressSSE } from "../types/media";
import { propertyService } from "../api/propertyService";

/**
 * Custom hook that manages an EventSource connection for media upload progress.
 * Pass `null` as propertyId to disable the connection.
 * Automatically falls back to REST polling if the SSE connection fails or drops.
 */
export function useMediaProgressSSE(propertyId: string | null) {
   const updateFromSSE = useUploadProgressStore((s) => s.updateFromSSE);
   const eventSourceRef = useRef<EventSource | null>(null);
   const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);

   useEffect(() => {
      if (!propertyId) return;

      let isPollingActive = false;

      const stopPolling = () => {
         if (pollIntervalRef.current) {
            clearInterval(pollIntervalRef.current);
            pollIntervalRef.current = null;
         }
         isPollingActive = false;
      };

      const startPollingFallback = () => {
         if (isPollingActive) return;
         isPollingActive = true;
         console.warn(
            `[SSE] Connection issue. Falling back to REST polling for property: ${propertyId}`
         );

         const fetchProgress = async () => {
            try {
               const res = await propertyService.getMediaProgress(propertyId);
               if (res.data) {
                  updateFromSSE(propertyId, res.data);
                  const status = res.data.status;
                  if (
                     status === "COMPLETED" ||
                     status === "PARTIAL_SUCCESS" ||
                     status === "FAILED"
                  ) {
                     stopPolling();
                  }
               }
            } catch (err) {
               console.error("[Poll] Fallback fetch failed:", err);
            }
         };

         fetchProgress(); // immediate fetch
         pollIntervalRef.current = setInterval(fetchProgress, 3000); // poll every 3s
      };

      // Establish EventSource
      const sseUrl = `/api/v1/media/progress/${propertyId}/stream`;
      const eventSource = new EventSource(sseUrl, {
         withCredentials: true,
      });
      eventSourceRef.current = eventSource;

      eventSource.addEventListener("progress", (event: MessageEvent) => {
         try {
            const data: MediaProgressSSE = JSON.parse(event.data);
            updateFromSSE(propertyId, data);

            // If SSE is delivering events and we get a terminal state, stop everything
            const status = data.status;
            if (status === "COMPLETED" || status === "PARTIAL_SUCCESS" || status === "FAILED") {
               stopPolling();
            }
         } catch (err) {
            console.error("[SSE] Failed to parse progress event:", err);
         }
      });

      eventSource.addEventListener("heartbeat", () => {
         // no-op, keeps connection alive
      });

      eventSource.onerror = (err) => {
         console.error("[SSE] EventSource connection failed. Starting fallback REST polling.", err);
         startPollingFallback();
      };

      return () => {
         eventSource.close();
         eventSourceRef.current = null;
         stopPolling();
      };
   }, [propertyId, updateFromSSE]);
}
