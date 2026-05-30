package com.omnibooking.services.core;

import com.omnibooking.dto.event.EmailEvent;
import com.omnibooking.dto.event.PropertySyncEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OutboxEventRegistry {

   private static final Map<String, Class<?>> REGISTRY = new ConcurrentHashMap<>();

   static {
      REGISTRY.put("USER_REGISTERED_MAIL", EmailEvent.class);
      REGISTRY.put("USER_RESEND_VERIFICATION_MAIL", EmailEvent.class);
      REGISTRY.put("USER_FORGOT_PASSWORD_MAIL", EmailEvent.class);
      REGISTRY.put("SECURITY_OTP_SEND", EmailEvent.class);
      REGISTRY.put("2FA_OTP_SEND", EmailEvent.class);
      REGISTRY.put("PARTNER_OTP_SEND", EmailEvent.class);
      REGISTRY.put("PROPERTY_SYNC", PropertySyncEvent.class);
   }

   public static Class<?> getEventClass(String eventType) {
      Class<?> clazz = REGISTRY.get(eventType);
      if (clazz == null) {
         throw new IllegalArgumentException("Unknown event type: " + eventType);
      }
      return clazz;
   }

   public static void register(String eventType, Class<?> clazz) {
      REGISTRY.put(eventType, clazz);
   }
}
