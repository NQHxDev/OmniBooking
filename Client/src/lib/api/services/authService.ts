import apiClient from "../apiClient";
import { type User } from "@/store/useAuthStore";
import { v7 as uuidv7 } from "uuid";

export interface ApiResponse<T> {
   message: string;
   errorCode?: string;
   data: T;
   requestId?: string;
}

export interface LoginRequest {
   email: string;
   password: string;
   rememberMe?: boolean;
}

export interface RegisterRequest {
   email: string;
   password: string;
   fullName: string;
   rememberMe?: boolean;
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
    * Authenticates a user with 2FA OTP code.
    */
   loginWith2FA: async (payload: {
      email: string;
      password: string;
      code: string;
      rememberMe?: boolean;
   }): Promise<User> => {
      const response = (await apiClient.post("/auth/2fa/login", payload, {
         withCredentials: true,
         // @ts-expect-error - Custom axios config flag for interceptor
         _skipToast: true,
      })) as unknown as ApiResponse<User>;
      return response.data;
   },

   /**
    * Registers a new user.
    */
   register: async (payload: RegisterRequest): Promise<ApiResponse<User>> => {
      const idempotencyKey = uuidv7();
      const response = (await apiClient.post("/auth/register", payload, {
         withCredentials: true,
         headers: {
            "X-Idempotency-Key": idempotencyKey,
         },
         // @ts-expect-error - Custom axios config flag for interceptor
         _skipToast: true,
      })) as unknown as ApiResponse<User>;
      return response;
   },

   /**
    * Verifies user email with a token.
    */
   verifyEmail: async (token: string) => {
      return apiClient.get<ApiResponse<void>>(`/auth/verify?token=${token}`);
   },

   /**
    * Resends the verification email to the logged-in user.
    */
   resendVerification: async () => {
      return apiClient.post<ApiResponse<void>>("/auth/resend-verification");
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

   /**
    * Finalizes registration session by setting HttpOnly cookies.
    */
   finalizeRegistration: async (accessToken: string): Promise<User> => {
      const response = (await apiClient.post(
         "/auth/finalize-registration",
         { accessToken },
         {
            withCredentials: true,
            // @ts-expect-error - Custom axios config flag for interceptor
            _skipToast: true,
         }
      )) as unknown as ApiResponse<User>;
      return response.data;
   },
};
