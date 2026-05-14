import axios from "axios";
import { v7 as uuidv7 } from "uuid";
import { toast } from "sonner";
import { getBaseURL } from "./config";

const apiClient = axios.create({
   baseURL: getBaseURL(),
   timeout: 15000,
   withCredentials: true,
   headers: {
      "Content-Type": "application/json",
   },
});

apiClient.interceptors.request.use(
   (config) => {
      // Inject Request ID for distributed tracing
      config.headers["X-Request-ID"] = uuidv7();

      return config;
   },
   (error) => Promise.reject(error)
);

// Global variables for synchronized refresh
interface PromiseHandlers {
   resolve: (value?: unknown) => void;
   reject: (reason?: unknown) => void;
}

let isRefreshing = false;
let failedQueue: PromiseHandlers[] = [];

const processQueue = (error: unknown, token: string | null = null) => {
   failedQueue.forEach((prom) => {
      if (error) {
         prom.reject(error);
      } else {
         prom.resolve(token);
      }
   });
   failedQueue = [];
};

// Response Interceptor
apiClient.interceptors.response.use(
   (response) => response.data,
   async (error) => {
      const originalRequest = error.config;
      const status = error.response ? error.response.status : null;
      const message =
         error.response?.data?.message || error.message || "An unexpected error occurred";

      const errorCode = error.response?.data?.errorCode;

      // Handle Token Expired (AUTH_006)
      if (status === 401 && errorCode === "AUTH_006" && !originalRequest._retry) {
         if (isRefreshing) {
            // If refresh is already in progress, add this request to the queue
            return new Promise((resolve, reject) => {
               failedQueue.push({ resolve, reject });
            })
               .then(() => {
                  return apiClient(originalRequest);
               })
               .catch((err) => {
                  return Promise.reject(err);
               });
         }

         originalRequest._retry = true;
         isRefreshing = true;

         return new Promise((resolve, reject) => {
            axios
               .post(`${getBaseURL()}auth/refresh`, {}, { withCredentials: true })
               .then(() => {
                  processQueue(null);
                  resolve(apiClient(originalRequest));
               })
               .catch((refreshError) => {
                  processQueue(refreshError);
                  if (typeof window !== "undefined") {
                     localStorage.removeItem("auth-storage");
                     window.location.href = "/auth/login";
                  }
                  reject(refreshError);
               })
               .finally(() => {
                  isRefreshing = false;
               });
         });
      }

      // Only show global toast if not explicitly skipped
      if (status === 401 && (errorCode === "AUTH_005" || errorCode === "AUTH_007")) {
         if (typeof window !== "undefined" && !originalRequest?._skipRedirect) {
            localStorage.removeItem("auth-storage");
            window.location.href = "/auth/login";
         }
         return Promise.reject(error);
      }

      if (errorCode === "AUTH_009" || errorCode === "AUTH_013") {
         return Promise.reject(error.response?.data || { message: error.message });
      }

      if (typeof window !== "undefined" && !error.config?._skipToast && !errorCode) {
         toast.error(message);
      }

      return Promise.reject(error.response?.data || { message: error.message });
   }
);

export default apiClient;
