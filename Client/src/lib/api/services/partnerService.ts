import apiClient from "../apiClient";

export const partnerService = {
   /**
    * Sends a verification OTP to the logged-in partner's email via Kafka.
    */
   sendOtp: async () => {
      return apiClient.post("/partner/send-otp");
   },

   /**
    * Verifies the OTP code provided by the partner.
    */
   verifyOtp: async (code: string) => {
      return apiClient.post(`/partner/verify-otp?code=${code}`);
   },

   /**
    * Completes the partner registration process (to be expanded later).
    */
   completeRegistration: async () => {
      const response = await apiClient.post("/partner/complete");
      return response.data;
   },
};
