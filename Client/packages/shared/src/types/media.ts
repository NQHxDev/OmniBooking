export interface MediaProgressSSE {
   total: number;
   queued: number;
   processed: number;
   failed: number;
   status: "PROCESSING" | "STALLED" | "COMPLETED" | "PARTIAL_SUCCESS" | "FAILED";
   percentage: number;
   lastUpdatedAt: number;
}

export interface UploadJob {
   propertyId: string;
   propertyName: string;
   total: number;
   queued: number;
   processed: number;
   failed: number;
   status: "PROCESSING" | "STALLED" | "COMPLETED" | "PARTIAL_SUCCESS" | "FAILED";
   percentage: number;
   lastUpdatedAt: number;
   dismissed: boolean;
   connectedAt: number;
}

export interface IncompleteUploadResponse {
   propertyId: string;
   propertyName: string;
   expectedCount: number;
   actualCount: number;
}
