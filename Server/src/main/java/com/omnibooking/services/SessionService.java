package com.omnibooking.services;

import com.omnibooking.security.RedisSessionInfo;

import java.util.Set;
import java.util.UUID;

public interface SessionService {

   void saveSession(UUID userId, String username, String email, String fullName, Set<String> roles, UUID sessionId,
         UUID refreshToken,
         String ip, String userAgent);

   RedisSessionInfo getSession(UUID sessionId);

   void deleteSession(UUID sessionId);

   void revokeAllUserSessions(UUID userId);

   boolean isValidSession(UUID sessionId, UUID refreshToken);

}
