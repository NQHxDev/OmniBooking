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

let refreshPromise: Promise<User> | null = null;

export const authService = {
   /**
    * Authenticates a user with email and password.
    */
   login: async (payload: LoginRequest): Promise<User> => {
      const response = (await apiClient.post("/auth/login", payload, {
         withCredentials: true,
         // @ts-expect-error - Custom axios config flag for interceptor
         _skipToast: true,
      })) as unknown as ApiResponse<User>;
      return response.data;
   },

   /**
    * Registers a new user.
    */
   register: async (payload: RegisterRequest): Promise<User> => {
      const response = (await apiClient.post("/auth/register", payload, {
         withCredentials: true,
         // @ts-expect-error - Custom axios config flag for interceptor
         _skipToast: true,
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
    * Uses a promise cache to prevent parallel refresh requests.
    */
   refresh: async (): Promise<User> => {
      if (refreshPromise) return refreshPromise;

      refreshPromise = (async () => {
         try {
            const response = (await apiClient.post(
               "/auth/refresh",
               {},
               {
                  withCredentials: true,
                  // @ts-expect-error - Custom axios config flag for interceptor
                  _skipToast: true,
                  _skipRedirect: true,
               }
            )) as unknown as ApiResponse<User>;
            return response.data;
         } finally {
            refreshPromise = null;
         }
      })();

      return refreshPromise;
   },

   /**
    * Logs out the current user.
    */
   logout: async () => {
      return apiClient.post<ApiResponse<void>>("/auth/logout", {}, { withCredentials: true });
   },

   /**
    * Requests a password reset email.
    */
   forgotPassword: async (email: string) => {
      return apiClient.post<ApiResponse<void>>("/auth/forgot-password", { email });
   },

   /**
    * Resets the password using a token.
    */
   resetPassword: async (payload: { token: string; newPassword: string; logoutAll?: boolean }) => {
      return apiClient.post<ApiResponse<void>>("/auth/reset-password", payload);
   },

   /**
    * Gets the OAuth2 Auth URL for a specific provider.
    */
   getOAuth2Url: async (provider: string): Promise<ApiResponse<string>> => {
      const response = (await apiClient.get(`/auth/${provider}/url`, {
         withCredentials: true,
      })) as unknown as ApiResponse<string>;
      return response;
   },
};
