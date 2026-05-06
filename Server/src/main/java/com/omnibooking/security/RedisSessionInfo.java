package com.omnibooking.security;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisSessionInfo {

   private UUID userId;

   private String username;

   private String email;

   private String role;

   private String hashedRefreshToken;

   private String ip;

   private String userAgent;

   private long createdAt;

}
