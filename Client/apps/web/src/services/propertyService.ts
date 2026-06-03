import apiClient from "@/lib/api/apiClient";
import { ApiResponse } from "@/lib/api/services/authService";

export interface PropertyResponse {
   id: string;
   name: string;
   propertyType: string;
   city: string;
   country: string;
   imageUrl: string;
   price?: number; // Optional if available
   rating?: number; // Optional if available
}

export interface RoomTypeResponse {
   id: string;
   name: string;
   description: string;
   basePrice: number;
   capacityAdults: number;
   capacityChildren: number;
   totalRooms: number;
   roomSizeSqm?: number;
   bedType?: string;
}

export interface PropertyDetailResponse {
   id: string;
   name: string;
   description: string;
   propertyType: string;
   address: string;
   city: string;
   country: string;
   starRating: number;
   checkInTime?: string;
   checkOutTime?: string;
   imageUrl?: string;
   imageUrls?: string[];
   amenities?: string[];
   roomTypes?: RoomTypeResponse[];
}

export const propertyService = {
   getFeatured: async (limit: number = 6): Promise<PropertyResponse[]> => {
      const response = await apiClient.get<unknown, ApiResponse<PropertyResponse[]>>(
         `/properties/featured?limit=${limit}`
      );
      return response.data || [];
   },

   getNew: async (limit: number = 15): Promise<PropertyResponse[]> => {
      const response = await apiClient.get<unknown, ApiResponse<PropertyResponse[]>>(
         `/properties/new?limit=${limit}`
      );
      return response.data || [];
   },

   getPropertyDetail: async (id: string): Promise<PropertyDetailResponse | null> => {
      try {
         const response = await apiClient.get<unknown, ApiResponse<PropertyDetailResponse>>(
            `/properties/${id}`
         );
         return response.data || null;
      } catch (error) {
         console.error("Failed to fetch property detail", error);
         return null;
      }
   },
};
