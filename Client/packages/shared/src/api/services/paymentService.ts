import apiClient from "../apiClient";
import { ApiResponse } from "./authService";

export const paymentService = {
   createMomoPayment: async (bookingId: string): Promise<{ payUrl: string }> => {
      const response = await apiClient.post<unknown, ApiResponse<{ payUrl: string }>>(
         "/payments/momo/create",
         { bookingId },
         { withCredentials: true }
      );
      return response.data;
   },
};
