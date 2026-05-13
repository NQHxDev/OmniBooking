package com.omnibooking.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SecurityOtpVerifyRequest {

   @NotBlank(message = "Mã OTP không được để trống")
   private String otp;

}
