package com.omnibooking.config.observability;

import com.omnibooking.constant.ObservabilityConstants.Modules;

public final class ModuleTagResolver {

   private ModuleTagResolver() {
      // Prevent instantiation
   }

   public static String resolveModule(String className) {
      if (className == null || className.isEmpty()) {
         return Modules.INFRASTRUCTURE;
      }

      String lowerClass = className.toLowerCase();

      if (lowerClass.contains(".auth.") || lowerClass.contains("authcontroller") || lowerClass.contains("security")) {
         return Modules.AUTH;
      }
      if (lowerClass.contains(".user.") || lowerClass.contains("usercontroller")) {
         return Modules.AUTH;
      }
      if (lowerClass.contains(".property.") || lowerClass.contains("propertycontroller") || lowerClass.contains("roomcontroller") || lowerClass.contains("bookingcontroller") || lowerClass.contains(".core.") || lowerClass.contains(".partner.")) {
         return Modules.BOOKING;
      }
      if (lowerClass.contains(".payment.") || lowerClass.contains("paymentcontroller")) {
         return Modules.PAYMENT;
      }
      if (lowerClass.contains(".media.") || lowerClass.contains("mediacontroller")) {
         return Modules.MEDIA;
      }
      if (lowerClass.contains(".search.") || lowerClass.contains("searchcontroller")) {
         return Modules.SEARCH;
      }
      if (lowerClass.contains(".communication.") || lowerClass.contains("notification") || lowerClass.contains("mail") || lowerClass.contains("sms") || lowerClass.contains("worker")) {
         return Modules.NOTIFICATION;
      }

      return Modules.INFRASTRUCTURE;
   }

   public static String resolveModule(Class<?> clazz) {
      if (clazz == null) {
         return Modules.INFRASTRUCTURE;
      }
      return resolveModule(clazz.getName());
   }
}
