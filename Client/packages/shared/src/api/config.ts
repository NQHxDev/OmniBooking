export const getBaseURL = () => {
   if (typeof window !== "undefined" && typeof window.document !== "undefined") {
      return "/api/v1/";
   }
   const url = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1/";
   return url.endsWith("/api/v1/") ? url : `${url.replace(/\/$/, "")}/api/v1/`;
};
