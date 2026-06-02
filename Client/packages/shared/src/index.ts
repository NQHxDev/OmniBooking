// Export API core
export { default as apiClient } from "./api/apiClient";
export * from "./api/config";

// Export Services (renamed to avoid conflicts where necessary)
export { propertyService as partnerPropertyService } from "./api/propertyService";
export { propertyService as publicPropertyService } from "./api/services/propertyService";
export { authService } from "./api/services/authService";
export { bookingService } from "./api/services/bookingService";
export { partnerService } from "./api/services/partnerService";
export { profileService } from "./api/services/profileService";
export { securityService } from "./api/services/securityService";

// Export Types
export * from "./types/api";
export * from "./types/user";

// Export Utilities
export * from "./utils/url";
