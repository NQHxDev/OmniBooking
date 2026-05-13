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

   private Object response; // This will be complex, usually we map to a Map or specific WebAuthn structure

   private String label;

}
