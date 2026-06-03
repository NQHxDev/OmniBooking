package com.omnibooking.security;

import com.omnibooking.services.auth.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import com.omnibooking.dto.ApiResponse;
import com.omnibooking.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

   private final JWTService jwtService;
   private final CustomUserDetailsService userDetailsService;
   private final StringRedisTemplate redisTemplate;
   private final ObjectMapper objectMapper;
   private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

   @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:3000}")
   private String allowedOrigins;

   @Bean
   public PasswordEncoder passwordEncoder() {
      return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
   }

   @Bean
   public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
      return config.getAuthenticationManager();
   }

   @Bean
   public CorsConfigurationSource corsConfigurationSource() {
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowCredentials(true);
      if (allowedOrigins != null && !allowedOrigins.isBlank()) {
         config.setAllowedOrigins(java.util.Arrays.stream(allowedOrigins.split(","))
               .map(String::trim)
               .filter(s -> !s.isEmpty())
               .collect(java.util.stream.Collectors.toList()));
      }
      config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-CSRF-Token",
            "X-Requested-With",
            "X-Request-ID",
            "X-Correlation-ID",
            "X-User-ID",
            "X-Tenant-ID",
            "sentry-trace",
            "baggage",
            "x-fgp",
            "Accept",
            "Origin",
            "X-Idempotency-Key"));
      config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
      config.setExposedHeaders(List.of("Set-Cookie", "X-Request-ID", "X-Correlation-ID"));
      config.setMaxAge(3600L);
      source.registerCorsConfiguration("/**", config);
      return source;
   }

   @Bean
   public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
      JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService, redisTemplate,
            meterRegistry);
      CustomCsrfFilter csrfFilter = new CustomCsrfFilter(objectMapper, allowedOrigins,
            meterRegistry);

      http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                  .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                  .requestMatchers("/auth/passkey/**").authenticated()
                  .requestMatchers("/auth/login", "/auth/register", "/auth/verify", "/auth/refresh", "/auth/logout",
                        "/auth/2fa/login",
                        "/auth/forgot-password", "/auth/reset-password", "/auth/google/**", "/auth/subscribe/**",
                        "/auth/finalize-registration",
                        "/auth/check-email", "/auth/activate-guest")
                  .permitAll()
                  .requestMatchers(HttpMethod.POST, "/bookings").permitAll()
                  .requestMatchers(HttpMethod.GET, "/bookings/**").permitAll()
                  .requestMatchers("/payments/**").permitAll()
                  .requestMatchers("/properties/search", "/properties/search/**").permitAll()
                  .requestMatchers("/destinations", "/destinations/**").permitAll()
                  .requestMatchers("/health/**").permitAll()
                  .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                  .requestMatchers("/currencies/**").permitAll()
                  .requestMatchers("/test/**").permitAll()
                  .requestMatchers("/actuator/**").permitAll()
                  // Allow public property details view if needed
                  .requestMatchers(HttpMethod.GET, "/properties/**").permitAll()
                  .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(csrfFilter, JwtAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                  .authenticationEntryPoint((request, response, authException) -> {
                     String requestId = (String) request.getAttribute("requestId");
                     ErrorCode errorCode = (ErrorCode) request.getAttribute("error_code");

                     if (errorCode == null) {
                        errorCode = ErrorCode.TOKEN_EXPIRED;
                     }

                     response.setStatus(errorCode.getStatus().value());
                     response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                     response.setCharacterEncoding("UTF-8");

                     ApiResponse<Object> apiResponse = ApiResponse.error(
                           errorCode.getMessage(),
                           errorCode.getCode(),
                           authException.getMessage(),
                           requestId);
                     response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
                  })
                  .accessDeniedHandler((request, response, accessDeniedException) -> {
                     String requestId = (String) request.getAttribute("requestId");
                     response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                     response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                     response.setCharacterEncoding("UTF-8");
                     ApiResponse<Object> apiResponse = ApiResponse.error(
                           "Bạn không có quyền thực hiện hành động này.",
                           "FORBIDDEN",
                           accessDeniedException.getMessage(),
                           requestId);
                     response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
                  }));

      return http.build();
   }

}
