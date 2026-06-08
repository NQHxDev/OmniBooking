import apiClient from "../apiClient";
import { ApiResponse } from "./authService";

export interface PriceRuleRequest {
   propertyId: string;
   roomTypeId?: string;
   name: string;
   ruleType: string;
   startDate?: string;
   endDate?: string;
   adjustmentType: string;
   adjustmentValue: number;
   occupancyThreshold?: number;
   priority?: number;
   isActive?: boolean;
}

export interface PriceRuleResponse {
   id: string;
   propertyId: string;
   roomTypeId?: string;
   name: string;
   ruleType: string;
   startDate?: string;
   endDate?: string;
   adjustmentType: string;
   adjustmentValue: number;
   occupancyThreshold?: number;
   priority: number;
   isActive: boolean;
}

export const priceRuleService = {
   create: async (request: PriceRuleRequest): Promise<PriceRuleResponse> => {
      const response = await apiClient.post<unknown, ApiResponse<PriceRuleResponse>>(
         "/pricing-rules",
         request,
         { withCredentials: true }
      );
      return response.data;
   },
   update: async (id: string, request: PriceRuleRequest): Promise<PriceRuleResponse> => {
      const response = await apiClient.put<unknown, ApiResponse<PriceRuleResponse>>(
         `/pricing-rules/${id}`,
         request,
         { withCredentials: true }
      );
      return response.data;
   },
   delete: async (id: string): Promise<void> => {
      await apiClient.delete<unknown, ApiResponse<void>>(`/pricing-rules/${id}`, {
         withCredentials: true,
      });
   },
   getByProperty: async (propertyId: string): Promise<PriceRuleResponse[]> => {
      const response = await apiClient.get<unknown, ApiResponse<PriceRuleResponse[]>>(
         `/pricing-rules/property/${propertyId}`,
         { withCredentials: true }
      );
      return response.data;
   },
   getById: async (id: string): Promise<PriceRuleResponse> => {
      const response = await apiClient.get<unknown, ApiResponse<PriceRuleResponse>>(
         `/pricing-rules/${id}`,
         { withCredentials: true }
      );
      return response.data;
   },
};
