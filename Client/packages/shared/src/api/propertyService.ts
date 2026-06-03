import apiClient from "./apiClient";
import { getBaseURL } from "./config";
import { ApiResponse } from "./services/authService";

export interface RoomTypeRequest {
   name: string;
   description?: string;
   basePrice: number;
   capacityAdults: number;
   capacityChildren: number;
   totalRooms: number;
   roomSizeSqm: number;
   bedType: string;
}

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
   businessRegistrationNumber?: string;
   taxCode?: string;
   legalOwnerName?: string;
   amenities?: string[];
   roomTypes?: RoomTypeRequest[];
}

export interface PropertyResponse {
   id: string;
   name: string;
   propertyType: string;
   city: string;
   country: string;
   imageUrl?: string;
}

export interface PartnerLegalProfileResponse {
   id: string;
   businessRegistrationNumber: string;
   taxCode: string;
   legalOwnerName: string;
}

export interface RoomTypeResponse {
   id: string;
   name: string;
   description?: string;
   basePrice: number;
   capacityAdults: number;
   capacityChildren: number;
   totalRooms: number;
   roomSizeSqm: number;
   bedType: string;
}

export interface PropertyDetailResponse {
   id: string;
   name: string;
   description: string;
   propertyType: string;
   address: string;
   city: string;
   country: string;
   starRating?: number;
   checkInTime: string;
   checkOutTime: string;
   imageUrl?: string;
   businessRegistrationNumber?: string;
   taxCode?: string;
   legalOwnerName?: string;
   amenities?: string[];
   roomTypes?: RoomTypeResponse[];
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

   getLegalProfiles: async (): Promise<PartnerLegalProfileResponse[]> => {
      const response = await apiClient.get<unknown, ApiResponse<PartnerLegalProfileResponse[]>>(
         "/partner/properties/legal-profiles"
      );
      return response.data || [];
   },

   getPropertyDetail: async (id: string): Promise<PropertyDetailResponse> => {
      const response = await apiClient.get<unknown, ApiResponse<PropertyDetailResponse>>(
         `/partner/properties/${id}`
      );
      return response.data;
   },

   getPropertyDetailServer: async (
      id: string,
      cookiesStr: string,
      fingerprint?: string
   ): Promise<PropertyDetailResponse | null> => {
      const url = `${getBaseURL()}partner/properties/${id}`;

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

         const json: ApiResponse<PropertyDetailResponse> = await res.json();
         return json.data;
      } catch {
         return null;
      }
   },
};
