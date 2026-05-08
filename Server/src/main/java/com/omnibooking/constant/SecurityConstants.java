package com.omnibooking.constant;

public final class SecurityConstants {

   private SecurityConstants() {
      // Private constructor to prevent instantiation
   }

   public static final class Roles {
      private Roles() {
      }

      public static final String ADMIN = "ROLE_ADMIN";
      public static final String MANAGER = "ROLE_MANAGER";
      public static final String USER = "ROLE_USER";
      public static final String PARTNER = "ROLE_PARTNER";
      public static final String DRIVER = "ROLE_DRIVER";
   }

   public static final class Permissions {
      private Permissions() {
      }

      // User Management
      public static final String USER_READ = "user:read";
      public static final String USER_WRITE = "user:write";

      // Property & Room
      public static final String PROPERTY_READ = "property:read";
      public static final String PROPERTY_WRITE = "property:write";
      public static final String PROPERTY_DELETE = "property:delete";
      public static final String ROOM_READ = "room:read";
      public static final String ROOM_WRITE = "room:write";
      public static final String ROOM_DELETE = "room:delete";

      // Booking
      public static final String BOOKING_READ = "booking:read";
      public static final String BOOKING_WRITE = "booking:write";
      public static final String BOOKING_CANCEL = "booking:cancel";
      public static final String BOOKING_MANAGE = "booking:manage";

      // Reviews
      public static final String REVIEW_READ = "review:read";
      public static final String REVIEW_WRITE = "review:write";
      public static final String REVIEW_DELETE = "review:delete";
      public static final String REVIEW_REPLY = "review:reply";

      // Financials
      public static final String EARNING_READ = "earning:read";

      // Profile
      public static final String PARTNER_PROFILE_READ = "partner:profile:read";
      public static final String PARTNER_PROFILE_WRITE = "partner:profile:write";

      // Transportation
      public static final String RIDE_READ = "ride:read";
      public static final String RIDE_WRITE = "ride:write";
      public static final String RIDE_MANAGE = "ride:manage";
      public static final String VEHICLE_READ = "vehicle:read";
      public static final String VEHICLE_WRITE = "vehicle:write";
      public static final String VEHICLE_DELETE = "vehicle:delete";
   }
}
