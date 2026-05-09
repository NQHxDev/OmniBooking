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
   login: async (payload: LoginRequest): Promise<User> => {
      const response = (await apiClient.post("/auth/login", payload, {
         withCredentials: true,
      })) as unknown as ApiResponse<User>;
      return response.data;
   },

   /**
    * Registers a new user.
    */
   register: async (payload: RegisterRequest): Promise<User> => {
      const response = (await apiClient.post("/auth/register", payload, {
         withCredentials: true,
      })) as unknown as ApiResponse<User>;
      return response.data;
   },

   /**
    * Verifies user email with a token.
    */
   verifyEmail: async (token: string) => {
      return apiClient.get<ApiResponse<void>>(`/auth/verify?token=${token}`);
   },

   /**
    * Refreshes the current session and returns latest user data.
    */
   refresh: async (): Promise<User> => {
      const response = (await apiClient.post(
         "/auth/refresh",
         {},
         {
            withCredentials: true,
         }
      )) as unknown as ApiResponse<User>;
      return response.data;
   },

   /**
    * Logs out the current user.
    */
   logout: async () => {
      return apiClient.post<ApiResponse<void>>("/auth/logout", {}, { withCredentials: true });
   },
};
