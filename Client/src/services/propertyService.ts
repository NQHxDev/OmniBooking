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
};
