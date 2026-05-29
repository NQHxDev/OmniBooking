package com.omnibooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

   @NotBlank(message = "Email is required")
   @Email(message = "Invalid email format")
   private String email;

   @NotBlank(message = "Password is required")
   private String password;

   private boolean rememberMe;

   private String turnstileToken;

}
