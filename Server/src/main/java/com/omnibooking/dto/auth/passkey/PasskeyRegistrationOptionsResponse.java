package com.omnibooking.dto.auth.passkey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyRegistrationOptionsResponse {

   private String challenge;

   private String rpId;

   private String rpName;

   private String userId;

   private String username;

   private String userDisplayName;

}
