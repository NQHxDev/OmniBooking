package com.omnibooking.dto.auth.passkey;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyResponse {

   private UUID id;

   private String label;

   private String credentialId;

   private Instant lastUsedAt;

   private Instant createdAt;

}
