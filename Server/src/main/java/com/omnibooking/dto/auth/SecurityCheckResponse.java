package com.omnibooking.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityCheckResponse {

   private boolean isTrusted;

   private long remainingTimeSeconds;

}
