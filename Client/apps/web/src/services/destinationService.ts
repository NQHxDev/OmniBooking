import apiClient from "@/lib/api/apiClient";
import { ApiResponse } from "@/lib/api/services/authService";

export interface DestinationSuggestionResponse {
   id: string;
   name: string;
   type: "CITY" | "LANDMARK" | "HOTEL";
   country: string;
   countryCode: string;
   location: {
      lat: number;
      lon: number;
   };
   displayName: string;
   imageUrl?: string;
   propertyCount?: number;
}

export const destinationService = {
   getTrending: async (
      locale: string = "vi",
      clientIp?: string
   ): Promise<DestinationSuggestionResponse[]> => {
      const headers: Record<string, string> = {};
      if (clientIp) {
         headers["X-Forwarded-For"] = clientIp;
      }
      const response = await apiClient.get<unknown, ApiResponse<DestinationSuggestionResponse[]>>(
         "/destinations/trending",
         {
            params: { locale },
            headers,
         }
      );
      return response.data || [];
   },

   search: async (
      query: string,
      locale: string = "vi"
   ): Promise<DestinationSuggestionResponse[]> => {
      const response = await apiClient.get<unknown, ApiResponse<DestinationSuggestionResponse[]>>(
         "/destinations/search",
         {
            params: { query, locale },
         }
      );
      return response.data || [];
   },
};
