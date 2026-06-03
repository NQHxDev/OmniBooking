package com.omnibooking.config.observability;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SentryPiiSanitizer {

   private SentryPiiSanitizer() {
      // Prevent instantiation
   }

   private static final Pattern JWT_PATTERN = Pattern.compile("ey[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*");
   private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}");
   // Simple phone pattern matching numbers with optional country prefix
   private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}");

   private static final String[] SENSITIVE_KEYS = {
      "password", "secret", "token", "authorization", "cookie", "card", "cvv", "payment",
      "passwd", "key", "credential", "private", "salt", "signature"
   };

   public static String sanitizeString(String input) {
      if (input == null || input.isEmpty()) {
         return input;
      }

      String sanitized = input;

      // Mask JWT
      Matcher jwtMatcher = JWT_PATTERN.matcher(sanitized);
      if (jwtMatcher.find()) {
         sanitized = jwtMatcher.replaceAll("[MASKED_JWT]");
      }

      // Mask Email
      Matcher emailMatcher = EMAIL_PATTERN.matcher(sanitized);
      if (emailMatcher.find()) {
         sanitized = emailMatcher.replaceAll("[MASKED_EMAIL]");
      }

      // Mask Phone
      Matcher phoneMatcher = PHONE_PATTERN.matcher(sanitized);
      if (phoneMatcher.find()) {
         sanitized = phoneMatcher.replaceAll("[MASKED_PHONE]");
      }

      return sanitized;
   }

   public static boolean isSensitiveKey(String key) {
      if (key == null) {
         return false;
      }
      String lowerKey = key.toLowerCase();
      for (String sensitiveKey : SENSITIVE_KEYS) {
         if (lowerKey.contains(sensitiveKey)) {
            return true;
         }
      }
      return false;
   }

   public static Object sanitizeValue(String key, Object value) {
      if (value == null) {
         return null;
      }

      if (isSensitiveKey(key)) {
         return "[MASKED_SENSITIVE_DATA]";
      }

      if (value instanceof String) {
         return sanitizeString((String) value);
      }

      if (value instanceof Map<?, ?> mapValue) {
         return sanitizeMap(mapValue);
      }

      return value;
   }

   public static Map<String, Object> sanitizeMap(Map<?, ?> map) {
      if (map == null) {
         return null;
      }
      Map<String, Object> sanitizedMap = new HashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
         String key = String.valueOf(entry.getKey());
         sanitizedMap.put(key, sanitizeValue(key, entry.getValue()));
      }
      return sanitizedMap;
   }
}
