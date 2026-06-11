import { create } from "zustand";
import { MediaProgressSSE, UploadJob } from "@/types/media";

interface UploadProgressState {
   jobs: Record<string, UploadJob>;
   addJob: (propertyId: string, propertyName: string, totalImages?: number) => void;
   updateFromSSE: (propertyId: string, data: MediaProgressSSE) => void;
   dismissJob: (propertyId: string) => void;
   removeJob: (propertyId: string) => void;
}

export const useUploadProgressStore = create<UploadProgressState>()((set) => ({
   jobs: {},

   addJob: (propertyId, propertyName, totalImages = 0) =>
      set((state) => ({
         jobs: {
            ...state.jobs,
            [propertyId]: {
               propertyId,
               propertyName,
               total: totalImages,
               queued: 0,
               processed: 0,
               failed: 0,
               status: "PROCESSING",
               percentage: 0,
               lastUpdatedAt: Date.now(),
               dismissed: false,
               connectedAt: Date.now(),
            },
         },
      })),

   updateFromSSE: (propertyId, data) =>
      set((state) => {
         const existing = state.jobs[propertyId];
         if (!existing) return state;

         return {
            jobs: {
               ...state.jobs,
               [propertyId]: {
                  ...existing,
                  total: data.total,
                  queued: data.queued,
                  processed: data.processed,
                  failed: data.failed,
                  status: data.status,
                  percentage: data.percentage,
                  lastUpdatedAt: data.lastUpdatedAt,
               },
            },
         };
      }),

   dismissJob: (propertyId) =>
      set((state) => {
         const existing = state.jobs[propertyId];
         if (!existing) return state;

         return {
            jobs: {
               ...state.jobs,
               [propertyId]: {
                  ...existing,
                  dismissed: true,
               },
            },
         };
      }),

   removeJob: (propertyId) =>
      set((state) => {
         const { [propertyId]: _, ...rest } = state.jobs;
         return { jobs: rest };
      }),
}));
