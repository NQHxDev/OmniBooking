package com.omnibooking.security;

import java.util.Set;
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

   private String fullName;

   private Set<String> roles;

   private String hashedRefreshToken;

   private String ip;

   private String userAgent;

   private long createdAt;

   private long lastAccessedAt;

   private boolean rememberMe;

   private Integer deviceVersion;

   private String platform;

   private String browserFamily;

   private String csrfNonce;

   private UUID refreshFamilyId;

   private UUID refreshTokenId;

   private UUID parentTokenId;

   private boolean used;

   private UUID childSessionId;

   private Long rotationTimestamp;

   private String encryptedChildCredentials;

   @Builder.Default
   private Integer sessionVersion = 1;

   private boolean active;

}
