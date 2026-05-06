package com.omnibooking.services;

import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.LoginRequest;
import com.omnibooking.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

public interface AuthService {

   AuthResponse register(RegisterRequest request);

   AuthResponse login(LoginRequest request, String ip, String userAgent, HttpServletResponse response);

   AuthResponse refresh(String sessionId, String refreshToken, String ip, String userAgent,
         HttpServletResponse response);

   void logout(UUID sessionId, UUID userId, HttpServletResponse response);

}
