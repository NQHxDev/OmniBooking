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
}

export const destinationService = {
   getTrending: async (locale: string = "vi"): Promise<DestinationSuggestionResponse[]> => {
      const response = await apiClient.get<unknown, ApiResponse<DestinationSuggestionResponse[]>>(
         "/destinations/trending",
         {
            params: { locale },
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
