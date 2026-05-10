import apiClient, { getBaseURL } from "./apiClient";
import { ApiResponse } from "./services/authService";

export interface PropertyRequest {
   name: string;
   description: string;
   propertyType: string;
   address: string;
   city: string;
   country: string;
   starRating: number;
   checkInTime: string;
   checkOutTime: string;
   cancellationPolicyId?: string;
}

export interface PropertyResponse {
   id: string;
   name: string;
   propertyType: string;
   city: string;
   country: string;
   imageUrl?: string;
}

export const propertyService = {
   createProperty: async (data: PropertyRequest) => {
      const response = await apiClient.post<unknown, ApiResponse<PropertyResponse>>(
         "/partner/properties",
         data
      );
      return response.data;
   },

   getMyProperties: async (): Promise<PropertyResponse[]> => {
      const response = await apiClient.get<unknown, ApiResponse<PropertyResponse[]>>(
         "/partner/properties/mine"
      );

      return response.data || [];
   },

   getMyPropertiesServer: async (
      cookiesStr: string,
      fingerprint?: string
   ): Promise<PropertyResponse[] | null> => {
      const url = `${getBaseURL()}partner/properties/mine`;

      try {
         const headers: Record<string, string> = {
            Cookie: cookiesStr,
         };

         if (fingerprint) {
            headers["x-fgp"] = fingerprint;
         }

         const res = await fetch(url, {
            headers,
            cache: "no-store",
         });

         if (!res.ok) return null;

         const json: ApiResponse<PropertyResponse[]> = await res.json();
         return json.data || [];
      } catch {
         return null;
      }
   },

   uploadMedia: async (
      file: File,
      entityId: string,
      entityType: string,
      isMain: boolean = false
   ) => {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("entityId", entityId);
      formData.append("entityType", entityType);
      formData.append("isMain", String(isMain));

      const response = await apiClient.post("/media/upload", formData, {
         headers: {
            "Content-Type": "multipart/form-data",
         },
      });
      return response.data;
   },
};
