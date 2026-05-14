package com.omnibooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

   @NotBlank(message = "Token is required")
   private String token;

   @NotBlank(message = "New password is required")
   @Size(min = 6, message = "Password must be at least 6 characters")
   private String newPassword;

   @Builder.Default
   private boolean logoutAll = false;

}
