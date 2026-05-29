package com.omnibooking.dto.profile;

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
public class ChangePasswordRequest {

   private String currentPassword;

   @NotBlank(message = "Mật khẩu mới không được để trống")
   @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
   private String newPassword;

}
