import apiClient from "../apiClient";
import { type User } from "../../types/user";
import { type ApiResponse } from "./authService";
import { getBaseURL } from "../config";

export interface PartnerStatsResponse {
   monthlyRevenue: string;
   monthlyRevenueChange: string;
   monthlyRevenueUp: boolean;
   totalBookings: string;
   totalBookingsChange: string;
   totalBookingsUp: boolean;
   newCustomers: string;
   newCustomersChange: string;
   newCustomersUp: boolean;
   ratingScore: string;
   ratingScoreChange: string;
   ratingScoreUp: boolean;
}

export interface PartnerBookingResponse {
   id: string;
   propertyName: string;
   roomTypeName: string;
   checkInDate: string;
   checkOutDate: string;
   numRooms: number;
   totalPrice: number;
   finalPrice: number;
   status: "PENDING" | "CONFIRMED" | "STAYED" | "CANCELLED" | "REFUNDED";
   guestName: string;
   guestEmail: string;
   guestPhone: string | null;
   specialRequests: string | null;
   createdAt: string;
}

export const partnerService = {
   /**
    * Sends a verification OTP to the logged-in partner's email via Kafka.
    */
   sendOtp: async () => {
      return apiClient.post<ApiResponse<void>>("/partner/send-otp");
   },

   /**
    * Verifies the OTP code provided by the partner.
    */
   verifyOtp: async (code: string) => {
      return apiClient.post<ApiResponse<void>>(`/partner/verify-otp?code=${code}`);
   },

   /**
    * Completes the partner registration process (to be expanded later).
    */
   completeRegistration: async (): Promise<User> => {
      const response = (await apiClient.post("/partner/complete")) as unknown as ApiResponse<User>;
      return response.data;
   },

   /**
    * Fetches partner stats on client side.
    */
   getStats: async (): Promise<PartnerStatsResponse> => {
      const response = await apiClient.get<unknown, ApiResponse<PartnerStatsResponse>>(
         "/partner/stats"
      );
      return response.data;
   },

   /**
    * Fetches partner stats on server side for Next.js.
    */
   getStatsServer: async (
      cookiesStr: string,
      fingerprint?: string
   ): Promise<PartnerStatsResponse | null> => {
      const url = `${getBaseURL()}partner/stats`;

      try {
         const headers: Record<string, string> = {
            Cookie: cookiesStr,
         };

         if (fingerprint) {
            headers["x-fgp"] = fingerprint;
         }

         const res = await fetch(url, {
            headers,
            cache: "no-store",
         });

         if (!res.ok) return null;

         const json: ApiResponse<PartnerStatsResponse> = await res.json();
         return json.data;
      } catch {
         return null;
      }
   },

   /**
    * Fetches partner bookings on client side.
    */
   getBookings: async (): Promise<PartnerBookingResponse[]> => {
      const response = await apiClient.get<unknown, ApiResponse<PartnerBookingResponse[]>>(
         "/partner/bookings"
      );
      return response.data;
   },

   /**
    * Fetches partner bookings on server side for Next.js.
    */
   getBookingsServer: async (
      cookiesStr: string,
      fingerprint?: string
   ): Promise<PartnerBookingResponse[] | null> => {
      const url = `${getBaseURL()}partner/bookings`;

      try {
         const headers: Record<string, string> = {
            Cookie: cookiesStr,
         };

         if (fingerprint) {
            headers["x-fgp"] = fingerprint;
         }

         const res = await fetch(url, {
            headers,
            cache: "no-store",
         });

         if (!res.ok) return null;

         const json: ApiResponse<PartnerBookingResponse[]> = await res.json();
         return json.data;
      } catch {
         return null;
      }
   },
};
