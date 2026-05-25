package com.omnibooking.validation;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

   private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

   @Override
   public void initialize(ValidPhoneNumber constraintAnnotation) {
   }

   @Override
   public boolean isValid(String value, ConstraintValidatorContext context) {
      // Phone number is optional, accept null or empty string
      if (value == null || value.trim().isEmpty()) {
         return true;
      }

      try {
         String phone = value.trim();
         // E.164 format requires a '+' prefix
         if (!phone.startsWith("+")) {
            log.warn("Phone number validation failed: E.164 phone must start with '+': {}", phone);
            return false;
         }

         PhoneNumber parsedNumber = phoneNumberUtil.parse(phone, null);
         boolean isValid = phoneNumberUtil.isValidNumber(parsedNumber);
         if (!isValid) {
            log.warn("Google PhoneNumberUtil validation failed for number: {}", phone);
         }
         return isValid;
      } catch (Exception e) {
         log.warn("Exception occurred while parsing phone number: {}", value, e);
         return false;
      }
   }

}
