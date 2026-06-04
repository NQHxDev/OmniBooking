package com.omnibooking.constant;

public final class EventConstants {

   private EventConstants() {
      // Prevent instantiation
   }

   // User events
   public static final String USER_REGISTERED_MAIL = "USER_REGISTERED_MAIL";
   public static final String USER_REGISTERED = "USER_REGISTERED";
   public static final String USER_RESEND_VERIFICATION_MAIL = "USER_RESEND_VERIFICATION_MAIL";
   public static final String RESEND_VERIFICATION = "RESEND_VERIFICATION";
   public static final String USER_FORGOT_PASSWORD_MAIL = "USER_FORGOT_PASSWORD_MAIL";
   public static final String FORGOT_PASSWORD = "FORGOT_PASSWORD";
   public static final String SECURITY_OTP_SEND = "SECURITY_OTP_SEND";
   public static final String TWO_FACTOR_OTP_SEND = "2FA_OTP_SEND";
   public static final String PARTNER_OTP_SEND = "PARTNER_OTP_SEND";
   public static final String TWO_FACTOR_ENABLED = "TWO_FACTOR_ENABLED";

   // Property events
   public static final String PROPERTY_SYNC = "PROPERTY_SYNC";

   // Booking events
   public static final String BOOKING_CONFIRMED_MAIL = "BOOKING_CONFIRMED_MAIL";

}
