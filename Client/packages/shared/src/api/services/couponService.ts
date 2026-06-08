import apiClient from "../apiClient";
import { ApiResponse } from "./authService";

export interface CouponRequest {
   code: string;
   discountType: string;
   discountValue: number;
   minBookingAmount?: number;
   maxDiscountAmount?: number;
   validFrom: string;
   validUntil: string;
   usageLimit?: number;
   propertyId?: string;
   isActive?: boolean;
}

export interface CouponResponse {
   id: string;
   code: string;
   discountType: string;
   discountValue: number;
   minBookingAmount: number;
   maxDiscountAmount?: number;
   validFrom: string;
   validUntil: string;
   usageLimit?: number;
   usedCount: number;
   reservedCount: number;
   propertyId?: string;
   isActive: boolean;
}

export interface ReserveCouponRequest {
   couponId: string;
   bookingSessionId: string;
   propertyId: string;
}

export interface ReserveCouponResponse {
   reservationId: string;
   reservationToken: string;
   expiresAt: string;
}

export const couponService = {
   create: async (request: CouponRequest): Promise<CouponResponse> => {
      const response = await apiClient.post<unknown, ApiResponse<CouponResponse>>(
         "/coupons",
         request,
         { withCredentials: true }
      );
      return response.data;
   },
   update: async (id: string, request: CouponRequest): Promise<CouponResponse> => {
      const response = await apiClient.put<unknown, ApiResponse<CouponResponse>>(
         `/coupons/${id}`,
         request,
         { withCredentials: true }
      );
      return response.data;
   },
   delete: async (id: string): Promise<void> => {
      await apiClient.delete<unknown, ApiResponse<void>>(`/coupons/${id}`, {
         withCredentials: true,
      });
   },
   getByProperty: async (propertyId: string): Promise<CouponResponse[]> => {
      const response = await apiClient.get<unknown, ApiResponse<CouponResponse[]>>(
         `/coupons/property/${propertyId}`,
         { withCredentials: true }
      );
      return response.data;
   },
   getById: async (id: string): Promise<CouponResponse> => {
      const response = await apiClient.get<unknown, ApiResponse<CouponResponse>>(`/coupons/${id}`, {
         withCredentials: true,
      });
      return response.data;
   },
   reserve: async (request: ReserveCouponRequest): Promise<ReserveCouponResponse> => {
      const response = await apiClient.post<unknown, ApiResponse<ReserveCouponResponse>>(
         "/coupons/reserve",
         request,
         { withCredentials: true }
      );
      return response.data;
   },
};
