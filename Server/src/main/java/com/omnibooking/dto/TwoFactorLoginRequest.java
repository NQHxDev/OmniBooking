package com.omnibooking.dto;

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
public class TwoFactorLoginRequest {

   private String email;

   private String password;

   private String code;

   private boolean rememberMe;

}
