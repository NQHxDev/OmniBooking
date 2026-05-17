package com.omnibooking.services.auth.impl;

import com.omnibooking.config.AppProperties;
import com.omnibooking.dto.oauth.GoogleTokenResponse;
import com.omnibooking.dto.oauth.GoogleUserInfo;
import com.omnibooking.dto.oauth.OAuth2UserInfo;
import com.omnibooking.services.auth.OAuth2ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleOAuth2ServiceImpl implements OAuth2ProviderService {

   private final AppProperties appProperties;
   private final RestTemplate restTemplate;
   private final StringRedisTemplate redisTemplate;

   private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
   private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
   private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
   private static final String STATE_PREFIX = "oauth2:state:";

   @Override
   public String getProviderName() {
      return "google";
   }

   @Override
   public String generateAuthUrl() {
      String state = UUID.randomUUID().toString();
      // Store state in Redis with 15 mins expiration
      redisTemplate.opsForValue().set(STATE_PREFIX + state, "valid", 15, TimeUnit.MINUTES);

      return GOOGLE_AUTH_URL + "?" +
            "client_id=" + appProperties.getOauth2().getGoogle().getClientId() +
            "&redirect_uri=" + appProperties.getOauth2().getGoogle().getRedirectUri() +
            "&response_type=code" +
            "&scope=openid%20email%20profile" +
            "&state=" + state +
            "&access_type=offline" +
            "&prompt=consent";
   }

   @Override
   public OAuth2UserInfo exchangeCodeForUserInfo(String code, String state) {
      // Validate state
      String stateKey = STATE_PREFIX + state;
      if (Boolean.FALSE.equals(redisTemplate.hasKey(stateKey))) {
         log.error("[OAuth2] Invalid state: {}", state);
         throw new RuntimeException("Invalid state parameter");
      }
      redisTemplate.delete(stateKey);

      // Exchange code for tokens
      GoogleTokenResponse tokenResponse = exchangeCode(code);

      // Fetch user info
      return fetchUserInfo(tokenResponse.getAccessToken());
   }

   private GoogleTokenResponse exchangeCode(String code) {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      String body = "code=" + code +
            "&client_id=" + appProperties.getOauth2().getGoogle().getClientId() +
            "&client_secret=" + appProperties.getOauth2().getGoogle().getClientSecret() +
            "&redirect_uri=" + appProperties.getOauth2().getGoogle().getRedirectUri() +
            "&grant_type=authorization_code";

      HttpEntity<String> request = new HttpEntity<>(body, headers);
      ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request,
            GoogleTokenResponse.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
         log.error("[OAuth2] Failed to exchange code. Status: {}", response.getStatusCode());
         throw new RuntimeException("Failed to exchange code for tokens");
      }

      return response.getBody();
   }

   private GoogleUserInfo fetchUserInfo(String accessToken) {
      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(Objects.requireNonNull(accessToken));

      HttpEntity<Void> request = new HttpEntity<>(headers);
      ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(GOOGLE_USERINFO_URL, Objects.requireNonNull(
            HttpMethod.GET), request,
            GoogleUserInfo.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
         log.error("[OAuth2] Failed to fetch user info. Status: {}", response.getStatusCode());
         throw new RuntimeException("Failed to fetch user info from Google");
      }

      return response.getBody();
   }
}
