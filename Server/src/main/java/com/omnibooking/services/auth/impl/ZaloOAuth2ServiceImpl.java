package com.omnibooking.services.auth.impl;

import com.omnibooking.config.AppProperties;
import com.omnibooking.dto.oauth.OAuth2UserInfo;
import com.omnibooking.dto.oauth.ZaloUserInfo;
import com.omnibooking.services.auth.OAuth2ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Objects;

@Service("zalo")
@RequiredArgsConstructor
@Slf4j
public class ZaloOAuth2ServiceImpl implements OAuth2ProviderService {

   private final AppProperties appProperties;
   private final RestTemplate restTemplate;

   private static final String ZALO_AUTH_URL = "https://oauth.zaloapp.com/v4/permission";
   private static final String ZALO_TOKEN_URL = "https://oauth.zaloapp.com/v4/access_token";
   private static final String ZALO_USER_INFO_URL = "https://graph.zalo.me/v2.0/me";

   @Override
   public String generateAuthUrl() {
      return UriComponentsBuilder.fromUriString(ZALO_AUTH_URL)
            .queryParam("app_id", appProperties.getOauth2().getZalo().getClientId())
            .queryParam("redirect_uri", appProperties.getOauth2().getZalo().getRedirectUri())
            .queryParam("state", "zalo_auth") // You might want to generate a random state
            .build().encode().toUriString();
   }

   @Override
   public OAuth2UserInfo exchangeCodeForUserInfo(String code, String state) {
      log.info("[OAuth2] Fetching Zalo user info for code: {}", code);

      String accessToken = exchangeCodeForToken(code);
      return fetchUserInfoFromZalo(accessToken);
   }

   private String exchangeCodeForToken(String code) {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      headers.set("secret_key", appProperties.getOauth2().getZalo().getClientSecret());

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("code", code);
      params.add("app_id", appProperties.getOauth2().getZalo().getClientId());
      params.add("grant_type", "authorization_code");

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

      ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            ZALO_TOKEN_URL,
            Objects.requireNonNull(HttpMethod.POST),
            request,
            new ParameterizedTypeReference<Map<String, Object>>() {
            });

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
         log.error("[OAuth2] Failed to exchange Zalo code. Status: {}", response.getStatusCode());
         throw new RuntimeException("Failed to exchange Zalo code for tokens");
      }

      Map<String, Object> body = Objects.requireNonNull(response.getBody());
      if (body.containsKey("error")) {
         log.error("[OAuth2] Zalo Token Error: {}", body.get("error_description"));
         throw new RuntimeException("Zalo OAuth error: " + body.get("error_description"));
      }

      return (String) body.get("access_token");
   }

   private ZaloUserInfo fetchUserInfoFromZalo(String accessToken) {
      HttpHeaders headers = new HttpHeaders();
      headers.set("access_token", Objects.requireNonNull(accessToken));

      HttpEntity<Void> request = new HttpEntity<>(headers);

      // We need to specify fields to get name and picture
      String url = UriComponentsBuilder.fromUriString(ZALO_USER_INFO_URL)
            .queryParam("fields", "id,name,picture,email")
            .build().toUriString();

      ResponseEntity<ZaloUserInfo> response = restTemplate.exchange(url, Objects.requireNonNull(HttpMethod.GET),
            request, ZaloUserInfo.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
         log.error("[OAuth2] Failed to fetch Zalo user info. Status: {}", response.getStatusCode());
         throw new RuntimeException("Failed to fetch user info from Zalo");
      }

      return response.getBody();
   }

   @Override
   public String getProviderName() {
      return "zalo";
   }
}
