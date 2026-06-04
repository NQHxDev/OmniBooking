package com.omnibooking.services.auth;

import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.LoginRequest;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.dto.TwoFactorLoginRequest;
import com.omnibooking.dto.oauth.OAuth2UserInfo;
import com.omnibooking.security.RedisSessionInfo;

import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

   AuthResponse register(RegisterRequest request, String ip, String userAgent, HttpServletResponse response,
         boolean rememberMe);

   AuthResponse login(LoginRequest request, String ip, String userAgent, HttpServletResponse response);

   AuthResponse loginWith2FA(TwoFactorLoginRequest request, String ip, String userAgent, HttpServletResponse response);

   AuthResponse loginWithOAuth2(String provider, OAuth2UserInfo userInfo, String ip,
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

   AuthResponse finalizeRegistration(String accessToken, String ip, String userAgent, HttpServletResponse response);

   boolean checkEmail(String email);

   AuthResponse activateGuest(String token, String password, String ip, String userAgent, HttpServletResponse response);

}
