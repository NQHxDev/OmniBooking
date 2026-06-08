package com.omnibooking.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberValidatorTest {

   private PhoneNumberValidator validator;

   @BeforeEach
   void setUp() {
      validator = new PhoneNumberValidator();
   }

   @Test
   void testNullOrEmptyValues_ShouldBeValid() {
      assertThat(validator.isValid(null, null)).isTrue();
      assertThat(validator.isValid("", null)).isTrue();
      assertThat(validator.isValid("   ", null)).isTrue();
   }

   @Test
   void testValidE164PhoneNumbers_ShouldBeValid() {
      // Vietnam
      assertThat(validator.isValid("+84987654321", null)).isTrue();
      // US
      assertThat(validator.isValid("+12025550143", null)).isTrue();
      // UK
      assertThat(validator.isValid("+447911123456", null)).isTrue();
   }

   @Test
   void testInvalidPhoneNumbers_ShouldBeInvalid() {
      // Missing '+' prefix
      assertThat(validator.isValid("84987654321", null)).isFalse();
      // Too short
      assertThat(validator.isValid("+123", null)).isFalse();
      // Plain text
      assertThat(validator.isValid("not-a-phone-number", null)).isFalse();
      // Invalid digits
      assertThat(validator.isValid("+84123", null)).isFalse();
   }

}
