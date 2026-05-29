package com.omnibooking.services.auth.impl;

import com.omnibooking.config.AppProperties;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.services.auth.TurnstileService;
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

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TurnstileServiceImpl implements TurnstileService {

   private final RestTemplate restTemplate;
   private final AppProperties appProperties;

   @Override
   public void verifyToken(String token, String remoteIp) {
      if (!appProperties.getTurnstile().isEnabled()) {
         log.info("Cloudflare Turnstile verification is disabled");
         return;
      }

      if (token == null || token.trim().isEmpty()) {
         log.warn("Cloudflare Turnstile token is missing or empty");
         throw new AppException(ErrorCode.INVALID_CAPTCHA);
      }

      try {
         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

         MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
         map.add("secret", appProperties.getTurnstile().getSecretKey());
         map.add("response", token);
         if (remoteIp != null) {
            map.add("remoteip", remoteIp);
         }

         HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
         String url = appProperties.getTurnstile().getVerifyUrl();

         ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
               url,
               Objects.requireNonNull(HttpMethod.POST),
               request,
               new ParameterizedTypeReference<Map<String, Object>>() {
               });
         Map<String, Object> body = response.getBody();

         if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
            log.warn("Cloudflare Turnstile verification failed. Response body: {}", body);
            throw new AppException(ErrorCode.INVALID_CAPTCHA);
         }

         log.info("Cloudflare Turnstile verification succeeded for IP: {}", remoteIp);
      } catch (AppException e) {
         throw e;
      } catch (Exception e) {
         log.error("Error occurred while calling Cloudflare Turnstile API", e);
         throw new AppException(ErrorCode.INVALID_CAPTCHA, "Failed to verify CAPTCHA. Please try again.");
      }
   }
}
