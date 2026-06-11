import { v7 as uuidv7 } from "uuid";
import apiClient from "../apiClient";
import { ApiResponse } from "./authService";

export interface CreateBookingRequest {
   roomTypeId: string;
   checkInDate: string;
   checkOutDate: string;
   numRooms: number;
   guestName: string;
   guestEmail: string;
   guestPhone?: string;
   specialRequests?: string;
   couponId?: string;
   reservationToken?: string;
   currency?: string;
   paymentMethod?: string;
   guestCount?: number;
}

export interface BookingResponse {
   id: string;
   bookingCode: string;
   guestName: string;
   guestEmail: string;
   propertyName: string;
   roomTypeName: string;
   checkInDate: string;
   checkOutDate: string;
   numRooms: number;
   totalPrice: number;
   finalPrice: number;
   status: string;
   activationToken?: string;
   currency?: string;
   depositAmount?: number;
   requiresDeposit?: boolean;
   paymentMethod?: string;
}

export interface StayPriceResult {
   dailyPrices: {
      date: string;
      basePrice: number;
      seasonalAdjustment: number;
      weekendAdjustment: number;
      occupancyAdjustment: number;
      finalPrice: number;
      appliedRuleIds: string[];
   }[];
   totalBasePrice: number;
   totalSeasonalAdjustment: number;
   totalWeekendAdjustment: number;
   totalOccupancyAdjustment: number;
   totalCouponDiscount: number;
   totalFinalPrice: number;
   appliedCouponId?: string;
   appliedCouponCode?: string;
}

export const bookingService = {
   create: async (
      request: CreateBookingRequest,
      idempotencyKey?: string
   ): Promise<BookingResponse> => {
      const key = idempotencyKey || uuidv7();
      const response = await apiClient.post<unknown, ApiResponse<BookingResponse>>(
         "/bookings",
         request,
         {
            headers: {
               "X-Idempotency-Key": key,
            },
            withCredentials: true,
         }
      );
      return response.data;
   },
   getById: async (id: string): Promise<BookingResponse> => {
      const response = await apiClient.get<unknown, ApiResponse<BookingResponse>>(
         `/bookings/${id}`,
         { withCredentials: true }
      );
      return response.data;
   },
   getMyBookings: async (): Promise<BookingResponse[]> => {
      const response = await apiClient.get<unknown, ApiResponse<BookingResponse[]>>(
         "/bookings/mine",
         { withCredentials: true }
      );
      return response.data;
   },
   calculatePrice: async (params: {
      propertyId: string;
      roomTypeId: string;
      checkIn: string;
      checkOut: string;
      guestCount: number;
      couponCode?: string;
   }): Promise<StayPriceResult> => {
      const response = await apiClient.get<unknown, ApiResponse<StayPriceResult>>(
         "/bookings/calculate-price",
         {
            params,
            withCredentials: true,
         }
      );
      return response.data;
   },
};
