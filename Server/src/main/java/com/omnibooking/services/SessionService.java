package com.omnibooking.services;

import com.omnibooking.security.RedisSessionInfo;

import java.util.UUID;

public interface SessionService {

   void saveSession(UUID sessionId, RedisSessionInfo info, long expiryMs);

   RedisSessionInfo getSession(UUID sessionId);

   void deleteSession(UUID sessionId);

   void revokeAllUserSessions(UUID userId);

   boolean isValidSession(UUID sessionId, UUID refreshToken);

}
