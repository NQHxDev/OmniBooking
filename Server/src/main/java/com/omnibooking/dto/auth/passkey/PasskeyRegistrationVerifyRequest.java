package com.omnibooking.dto.auth.passkey;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyRegistrationVerifyRequest {

   private String id; // credentialId

   private String rawId;

   private String type;

   private Object response;

   private String label;

}
