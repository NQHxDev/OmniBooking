export const getBaseURL = () => {
   if (typeof window !== "undefined" && typeof window.document !== "undefined") {
      return "/api/v1/";
   }
   const rawUrl = process.env.BACKEND_URL
      ? `${process.env.BACKEND_URL}/api/v1/`
      : process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1/";
   const url = rawUrl.endsWith("/api/v1/") ? rawUrl : `${rawUrl.replace(/\/$/, "")}/api/v1/`;
   return url;
};
