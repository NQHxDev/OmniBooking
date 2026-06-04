package com.omnibooking.services.core;

import com.omnibooking.constant.EventConstants;
import com.omnibooking.dto.event.EmailEvent;
import com.omnibooking.dto.event.PropertySyncEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OutboxEventRegistry {

   private static final Map<String, Class<?>> REGISTRY = new ConcurrentHashMap<>();

   static {
      REGISTRY.put(EventConstants.USER_REGISTERED_MAIL, EmailEvent.class);
      REGISTRY.put(EventConstants.USER_REGISTERED, EmailEvent.class);
      REGISTRY.put(EventConstants.USER_RESEND_VERIFICATION_MAIL, EmailEvent.class);
      REGISTRY.put(EventConstants.RESEND_VERIFICATION, EmailEvent.class);
      REGISTRY.put(EventConstants.USER_FORGOT_PASSWORD_MAIL, EmailEvent.class);
      REGISTRY.put(EventConstants.FORGOT_PASSWORD, EmailEvent.class);
      REGISTRY.put(EventConstants.SECURITY_OTP_SEND, EmailEvent.class);
      REGISTRY.put(EventConstants.TWO_FACTOR_OTP_SEND, EmailEvent.class);
      REGISTRY.put(EventConstants.PARTNER_OTP_SEND, EmailEvent.class);
      REGISTRY.put(EventConstants.TWO_FACTOR_ENABLED, EmailEvent.class);
      REGISTRY.put(EventConstants.PROPERTY_SYNC, PropertySyncEvent.class);
      REGISTRY.put(EventConstants.BOOKING_CONFIRMED_MAIL, EmailEvent.class);
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
