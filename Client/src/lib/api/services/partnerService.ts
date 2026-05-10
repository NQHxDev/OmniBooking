import apiClient from "../apiClient";
import { type User } from "@/store/useAuthStore";
import { type ApiResponse } from "./authService";

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
};
