package com.omnibooking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {

   String message() default "Số điện thoại không hợp lệ theo định dạng E.164 quốc tế (ví dụ: +84987654321)";

   Class<?>[] groups() default {};

   Class<? extends Payload>[] payload() default {};

}
