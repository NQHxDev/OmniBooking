import axios from "axios";
import { v7 as uuidv7 } from "uuid";
import { env } from "@/env";
import { toast } from "sonner";
import { getBaseURL } from "./config";

// Remove useAuthStore to avoid circular dependency

const apiClient = axios.create({
   baseURL: getBaseURL(),
   timeout: 15000,
   withCredentials: true,
   headers: {
      "Content-Type": "application/json",
   },
});

// Request Interceptor
apiClient.interceptors.request.use(
   (config) => {
      // Inject Request ID
      config.headers["X-Request-ID"] = uuidv7();

      // Add Auth Token if available (to be implemented with Zustand)
      // const token = useAuthStore.getState().token;
      // if (token) config.headers.Authorization = `Bearer ${token}`;

      return config;
   },
   (error) => Promise.reject(error)
);

// Response Interceptor
apiClient.interceptors.response.use(
   (response) => response.data, // Return only the data (ApiResponse structure)
   async (error) => {
      const originalRequest = error.config;
      const status = error.response ? error.response.status : null;
      const message =
         error.response?.data?.message || error.message || "An unexpected error occurred";

      const errorCode = error.response?.data?.errorCode;

      if (status === 401 && errorCode === "AUTH_006" && !originalRequest._retry) {
         originalRequest._retry = true;

         try {
            console.log("Token expired, attempting refresh...");
            // Call refresh endpoint (cookies are handled by browser)
            await axios.post(`${getBaseURL()}auth/refresh`, {}, { withCredentials: true });

            console.log("Refresh successful, retrying original request...");
            // Retry the original request
            return apiClient(originalRequest);
         } catch (refreshError: unknown) {
            console.error("Refresh failed, logging out...", refreshError);
            if (typeof window !== "undefined") {
               localStorage.removeItem("auth-storage");
               window.location.href = "/auth/login";
            }
            return Promise.reject(refreshError);
         }
      }

      // Only show global toast if not explicitly skipped
      if (status === 401 && (errorCode === "AUTH_005" || errorCode === "AUTH_007")) {
         if (typeof window !== "undefined" && !originalRequest?._skipRedirect) {
            localStorage.removeItem("auth-storage");
            window.location.href = "/auth/login";
         }
         return Promise.reject(error);
      }

      if (!error.config?._skipToast) {
         toast.error(message);
      }

      return Promise.reject(error.response?.data || { message: error.message });
   }
);

export default apiClient;
