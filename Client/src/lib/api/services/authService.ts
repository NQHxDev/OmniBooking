import apiClient from "../apiClient";
import { type User } from "@/store/useAuthStore";

export interface ApiResponse<T> {
   message: string;
   errorCode?: string;
   data: T;
}

export interface LoginRequest {
   email: string;
   password: string;
}

export interface RegisterRequest {
   email: string;
   password: string;
   fullName: string;
}

export const authService = {
   /**
    * Authenticates a user with email and password.
    */
   login: async (payload: LoginRequest) => {
      return apiClient.post<ApiResponse<User>>("/auth/login", payload, {
         withCredentials: true,
      });
   },

   /**
    * Registers a new user.
    */
   register: async (payload: RegisterRequest) => {
      return apiClient.post<ApiResponse<User>>("/auth/register", payload, {
         withCredentials: true,
      });
   },

   /**
    * Verifies user email with a token.
    */
   verifyEmail: async (token: string) => {
      return apiClient.get(`/auth/verify?token=${token}`);
   },

   /**
    * Logs out the current user (to be implemented on backend).
    */
   logout: async () => {
      return apiClient.post("/auth/logout", {}, { withCredentials: true });
   },
};
