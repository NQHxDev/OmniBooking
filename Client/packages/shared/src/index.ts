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
export { reviewService } from "./api/services/reviewService";
export * from "./api/services/reviewService";
export { priceRuleService } from "./api/services/priceRuleService";
export * from "./api/services/priceRuleService";
export { couponService } from "./api/services/couponService";
export * from "./api/services/couponService";

// Export Types
export * from "./types/api";
export * from "./types/user";
export type {
   PropertyResponse,
   PropertyRequest,
   RoomTypeRequest,
   RoomTypeResponse,
   PropertyDetailResponse,
   PartnerLegalProfileResponse,
} from "./api/propertyService";
export type {
   BookingResponse,
   CreateBookingRequest,
   StayPriceResult,
} from "./api/services/bookingService";
export type { PartnerBookingResponse, PartnerStatsResponse } from "./api/services/partnerService";
export type { UserProfile, UpdateProfileRequest } from "./api/services/profileService";
export type { SecurityStatusResponse } from "./api/services/securityService";
export type { LoginRequest, RegisterRequest } from "./api/services/authService";

// Export Utilities
export * from "./utils/url";
