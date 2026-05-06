package com.omnibooking.services;

import com.omnibooking.security.RedisSessionInfo;
import java.util.UUID;

public interface SessionService {

    void saveSession(UUID userId, String username, String email, String fullName, String role, UUID sessionId, UUID refreshToken,
          String ip, String userAgent);

   RedisSessionInfo getSession(UUID sessionId);

   void deleteSession(UUID sessionId);

   void revokeAllUserSessions(UUID userId);

   boolean isValidSession(UUID sessionId, UUID refreshToken);

}
