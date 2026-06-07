import apiClient from "../apiClient";
import { ApiResponse } from "./authService";

export interface CreateReviewRequest {
   bookingId: string;
   rating: number;
   comment?: string;
}

export interface ReviewReplyRequest {
   reply: string;
}

export interface ReviewResponse {
   id: string;
   bookingId: string;
   propertyId: string;
   propertyName: string;
   userId: string;
   userName: string;
   userAvatarUrl?: string;
   rating: number;
   comment: string;
   reply?: string;
   status: string;
   replyUpdatedAt?: string;
   createdAt: string;
   updatedAt: string;
}

export interface PageResponse<T> {
   items: T[];
   currentPage: number;
   totalPages: number;
   totalElements: number;
   hasNext: boolean;
   hasPrevious: boolean;
}

export const reviewService = {
   create: async (request: CreateReviewRequest): Promise<ReviewResponse> => {
      const response = await apiClient.post<unknown, ApiResponse<ReviewResponse>>(
         "/reviews",
         request,
         { withCredentials: true }
      );
      return response.data;
   },
   reply: async (id: string, request: ReviewReplyRequest): Promise<ReviewResponse> => {
      const response = await apiClient.post<unknown, ApiResponse<ReviewResponse>>(
         `/reviews/${id}/reply`,
         request,
         { withCredentials: true }
      );
      return response.data;
   },
   delete: async (id: string, reason?: string): Promise<void> => {
      await apiClient.delete<unknown, ApiResponse<void>>(`/reviews/${id}`, {
         params: { reason },
         withCredentials: true,
      });
   },
   hide: async (id: string, reason: string): Promise<ReviewResponse> => {
      const response = await apiClient.post<unknown, ApiResponse<ReviewResponse>>(
         `/reviews/${id}/hide`,
         null,
         {
            params: { reason },
            withCredentials: true,
         }
      );
      return response.data;
   },
   restore: async (id: string): Promise<ReviewResponse> => {
      const response = await apiClient.post<unknown, ApiResponse<ReviewResponse>>(
         `/reviews/${id}/restore`,
         null,
         { withCredentials: true }
      );
      return response.data;
   },
   getByPropertyId: async (
      propertyId: string,
      page = 0,
      size = 10
   ): Promise<PageResponse<ReviewResponse>> => {
      const response = await apiClient.get<unknown, ApiResponse<PageResponse<ReviewResponse>>>(
         `/reviews/properties/${propertyId}`,
         {
            params: { page, size },
            withCredentials: true,
         }
      );
      return response.data;
   },
   getMyReviews: async (page = 0, size = 10): Promise<PageResponse<ReviewResponse>> => {
      const response = await apiClient.get<unknown, ApiResponse<PageResponse<ReviewResponse>>>(
         "/reviews/me",
         {
            params: { page, size },
            withCredentials: true,
         }
      );
      return response.data;
   },
   getPartnerReviews: async (page = 0, size = 10): Promise<PageResponse<ReviewResponse>> => {
      const response = await apiClient.get<unknown, ApiResponse<PageResponse<ReviewResponse>>>(
         "/reviews/partner",
         {
            params: { page, size },
            withCredentials: true,
         }
      );
      return response.data;
   },
};
