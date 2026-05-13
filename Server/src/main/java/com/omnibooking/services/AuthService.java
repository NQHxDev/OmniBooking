package com.omnibooking.services;

import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.LoginRequest;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.security.RedisSessionInfo;

import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

public interface AuthService {

   AuthResponse register(RegisterRequest request, String ip, String userAgent, HttpServletResponse response,
         boolean rememberMe);

   AuthResponse login(LoginRequest request, String ip, String userAgent, HttpServletResponse response);

   AuthResponse loginWithOAuth2(String provider, com.omnibooking.dto.oauth.OAuth2UserInfo userInfo, String ip,
         String userAgent, HttpServletResponse response, boolean rememberMe);

   AuthResponse refresh(String sessionId, String refreshToken, String ip, String userAgent,
         HttpServletResponse response);

   void logout(UUID sessionId, UUID userId, HttpServletResponse response);

   void verifyEmail(String token);

   void resendVerification(UUID userId);

   void clearAllCookies(HttpServletResponse response);

   AuthResponse upgradeToPartner(UUID userId, String ip, String userAgent, HttpServletResponse response,
         boolean rememberMe);

   void forgotPassword(String email);

   void resetPassword(String token, String newPassword, boolean logoutAll);

   RedisSessionInfo getSessionInfo(String sessionId);

}
