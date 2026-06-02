import apiClient from "@/lib/api/apiClient";
import { ApiResponse } from "@/types/api";
import { RegistrationResponseJSON } from "@simplewebauthn/browser";

export interface PasskeyRegistrationOptions {
   challenge: string;
   rpId: string;
   rpName: string;
   userId: string;
   username: string;
   userDisplayName: string;
}

export interface Passkey {
   id: string;
   label: string;
   credentialId: string;
   lastUsedAt: string;
   createdAt: string;
}

export const passkeyService = {
   getRegistrationOptions: async () => {
      const response = await apiClient.post<unknown, ApiResponse<PasskeyRegistrationOptions>>(
         "/auth/passkey/register/options"
      );
      return response.data;
   },

   verifyRegistration: async (data: RegistrationResponseJSON & { label?: string }) => {
      const response = await apiClient.post<unknown, ApiResponse<void>>(
         "/auth/passkey/register/verify",
         data
      );
      return response;
   },

   checkStatus: async (): Promise<boolean> => {
      const response = await apiClient.get<unknown, ApiResponse<boolean>>("/auth/passkey/status");

      return response.data;
   },

   listPasskeys: async () => {
      const response = await apiClient.get<unknown, ApiResponse<Passkey[]>>("/auth/passkey");
      return response.data;
   },

   deletePasskey: async (id: string) => {
      const response = await apiClient.delete<ApiResponse<void>>(`/auth/passkey/${id}`);
      return response;
   },
};
