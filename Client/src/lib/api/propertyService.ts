import apiClient from "./apiClient";
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
}

export const propertyService = {
   createProperty: async (data: PropertyRequest) => {
      const response = await apiClient.post<PropertyResponse>("/partner/properties", data);
      return response.data;
   },

   getMyProperties: async (): Promise<PropertyResponse[]> => {
      const response = await apiClient.get<ApiResponse<PropertyResponse[]>>(
         "/partner/properties/mine"
      );
      return response.data.data;
   },

   getMyPropertiesServer: async (cookiesStr: string): Promise<PropertyResponse[]> => {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1/";
      const url = `${baseUrl.endsWith("/") ? baseUrl : baseUrl + "/"}partner/properties/mine`;

      try {
         const res = await fetch(url, {
            headers: {
               Cookie: cookiesStr,
            },
            cache: "no-store",
         });

         if (!res.ok) return [];
         const json: ApiResponse<PropertyResponse[]> = await res.json();
         return json.data || [];
      } catch (error) {
         console.error("Failed to fetch properties on server:", error);
         return [];
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
