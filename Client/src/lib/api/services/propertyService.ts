import apiClient from "../apiClient";
import { ApiResponse } from "./authService";

export interface Page<T> {
   content: T[];
   totalElements: number;
   totalPages: number;
   size: number;
   number: number;
}

export interface PropertyDocument {
   id: string;
   name: string;
   description: string;
   propertyType: string;
   address: string;
   city: string;
   country: string;
   starRating: number;
   amenities: string[];
   minPrice: number;
   averageRating: number;
   reviewCount: number;
   mainImageUrl: string;
   location?: {
      lat: number;
      lon: number;
   };
}

export interface SearchParams {
   ss?: string;
   minPrice?: number;
   maxPrice?: number;
   stars?: number;
   propertyType?: string;
   amenities?: string[];
   minRating?: number;
   page?: number;
   size?: number;
}

export const propertyService = {
   search: async (params: SearchParams) => {
      const response = await apiClient.get<ApiResponse<Page<PropertyDocument>>>(
         "/properties/search",
         { params }
      );
      return (
         response.data.data || {
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: 10,
            number: 0,
         }
      );
   },
};
