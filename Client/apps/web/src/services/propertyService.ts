import apiClient from "@/lib/api/apiClient";
import { ApiResponse } from "@/lib/api/services/authService";
import { PageResponse, ReviewResponse } from "@omnibooking/shared";

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
   currentPrice?: number;
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
   averageRating?: number;
   reviewCount?: number;
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

   getPropertyReviews: async (
      propertyId: string,
      page = 0,
      size = 10
   ): Promise<PageResponse<ReviewResponse>> => {
      try {
         const response = await apiClient.get<unknown, ApiResponse<PageResponse<ReviewResponse>>>(
            `/reviews/properties/${propertyId}?page=${page}&size=${size}`
         );
         return (
            response.data || {
               items: [],
               currentPage: 0,
               totalPages: 0,
               totalElements: 0,
               hasNext: false,
               hasPrevious: false,
            }
         );
      } catch (error) {
         console.error("Failed to fetch property reviews", error);
         return {
            items: [],
            currentPage: 0,
            totalPages: 0,
            totalElements: 0,
            hasNext: false,
            hasPrevious: false,
         };
      }
   },
};
