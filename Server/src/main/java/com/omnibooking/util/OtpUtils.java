package com.omnibooking.util;

import java.security.SecureRandom;

public class OtpUtils {

   private static final SecureRandom secureRandom = new SecureRandom();

   private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // Exclude I, O to avoid confusion

   private static final String NUMBERS = "0123456789";

   /**
    * Generates a 6-character alphanumeric OTP with pattern A0A000.
    * A: Letter, 0: Number
    */
   public static String generateAlphanumericOtp() {
      StringBuilder sb = new StringBuilder();

      // Character 1: Letter
      sb.append(LETTERS.charAt(secureRandom.nextInt(LETTERS.length())));
      // Character 2: Number
      sb.append(NUMBERS.charAt(secureRandom.nextInt(NUMBERS.length())));
      // Character 3: Letter
      sb.append(LETTERS.charAt(secureRandom.nextInt(LETTERS.length())));
      // Character 4-6: Numbers
      for (int i = 0; i < 3; i++) {
         sb.append(NUMBERS.charAt(secureRandom.nextInt(NUMBERS.length())));
      }

      return sb.toString();
   }

}
