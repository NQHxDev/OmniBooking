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
   currency?: string;
   paymentMethod?: string;
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

export const bookingService = {
   create: async (request: CreateBookingRequest): Promise<BookingResponse> => {
      const response = await apiClient.post<unknown, ApiResponse<BookingResponse>>(
         "/bookings",
         request,
         { withCredentials: true }
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
};
