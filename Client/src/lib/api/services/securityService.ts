import apiClient from "../apiClient";

export interface SecurityStatusResponse {
   isTrusted: boolean;
   remainingTimeSeconds: number;
}

export const securityService = {
   /**
    * Yêu cầu gửi mã OTP bảo mật qua email
    */
   requestOTP: async (): Promise<void> => {
      await apiClient.post("/auth/security/otp/request");
   },

   /**
    * Xác thực mã OTP để nâng cấp phiên làm việc lên "Trusted"
    */
   verifyOTP: async (otp: string): Promise<void> => {
      await apiClient.post("/auth/security/otp/verify", { otp });
   },

   /**
    * Kiểm tra trạng thái bảo mật của phiên hiện tại
    */
   getStatus: async (): Promise<SecurityStatusResponse> => {
      const response = await apiClient.get("/auth/security/status");
      return response.data;
   },
};
