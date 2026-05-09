import axios from "axios";
import { v7 as uuidv7 } from "uuid";
import { env } from "@/env";
import { toast } from "sonner";

const getBaseURL = () => {
   const url = env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1/";
   return url.endsWith("/api/v1/") ? url : `${url.replace(/\/$/, "")}/api/v1/`;
};

const apiClient = axios.create({
   baseURL: getBaseURL(),
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

      // If error is 401 and errorCode is TOKEN_EXPIRED (AUTH_006)
      if (status === 401 && errorCode === "AUTH_006" && !originalRequest._retry) {
         originalRequest._retry = true;

         try {
            console.log("Token expired, attempting refresh...");
            // Call refresh endpoint (cookies are handled by browser)
            await axios.post(`${getBaseURL()}auth/refresh`, {}, { withCredentials: true });

            console.log("Refresh successful, retrying original request...");
            // Retry the original request
            return apiClient(originalRequest);
         } catch (refreshError) {
            console.error("Refresh failed, logging out...", refreshError);
            // If refresh fails, we could redirect to login or clear state
            // window.location.href = "/auth/login";
            return Promise.reject(refreshError);
         }
      }

      toast.error(message);
      return Promise.reject(error.response?.data || error.message);
   }
);

export default apiClient;
