import axios from "axios";
import { v7 as uuidv7 } from "uuid";
import { env } from "@/env";
import { toast } from "sonner";

const apiClient = axios.create({
   baseURL: env.NEXT_PUBLIC_API_URL,
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
   (error) => {
      const status = error.response ? error.response.status : null;
      const message =
         error.response?.data?.message || error.message || "An unexpected error occurred";

      if (status === 401) {
         // Handle Unauthorized (Logout user)
         console.error("Unauthorized! Redirecting to login...");
      }

      toast.error(message);

      return Promise.reject(error.response?.data || error.message);
   }
);

export default apiClient;
