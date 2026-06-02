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

   /**
    * Lấy trạng thái kích hoạt 2FA của người dùng
    */
   get2FAStatus: async (): Promise<"UNSET" | "DISABLED" | "ENABLED"> => {
      const response = await apiClient.get("/auth/2fa/status");
      return response.data;
   },

   /**
    * Khởi tạo quá trình thiết lập 2FA
    */
   setup2FA: async (): Promise<{ secretKey: string; qrCodeUri: string }> => {
      const response = await apiClient.post("/auth/2fa/setup");
      return response.data;
   },

   /**
    * Xác nhận OTP để kích hoạt 2FA
    */
   enable2FA: async (code: string): Promise<string[]> => {
      const response = await apiClient.post("/auth/2fa/enable", { code });
      return response.data;
   },

   /**
    * Vô hiệu hóa 2FA
    */
   disable2FA: async (code: string): Promise<void> => {
      await apiClient.post("/auth/2fa/disable", { code });
   },

   /**
    * Gỡ bỏ hoàn toàn 2FA khỏi tài khoản
    */
   remove2FA: async (code: string): Promise<void> => {
      await apiClient.post("/auth/2fa/remove", { code });
   },
};
