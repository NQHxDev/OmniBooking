package com.omnibooking.config;

import lombok.Data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

   @NotBlank
   private String baseUrl;

   @NotBlank
   private String clientUrl;

   @NotBlank
   private String contextPath;

   private final Security security = new Security();

   private final Mail mail = new Mail();

   private final Cloudinary cloudinary = new Cloudinary();

   private final Oauth2 oauth2 = new Oauth2();

   private final Geo geo = new Geo();

   private final Currency currency = new Currency();

   private final Webauthn webauthn = new Webauthn();

   private final Turnstile turnstile = new Turnstile();

   private final Sentry sentry = new Sentry();

   @Data
   public static class Sentry {
      private String dsn;
      private String environment = "development";
      private String release;
      private double tracesSampleRate = 1.0;
      private boolean enabled = true;
   }

   @Data
   public static class Webauthn {
      @NotBlank
      private String rpId;
      @NotBlank
      private String rpName;
      @NotBlank
      private String origin;
   }

   @Data
   public static class Currency {
      private String apiKey;
      private String baseCurrency = "USD";
      private String providerUrl = "https://v6.exchangerate-api.com/v6/%s/latest/%s";
   }

   @Data
   public static class Geo {
      private String dbPath = "geo/GeoLite2-City.mmdb";
      private String defaultCountry = "VN";
   }

   @Data
   public static class Oauth2 {
      private final Google google = new Google();

      @Data
      public static class Google {
         @NotBlank
         private String clientId;
         @NotBlank
         private String clientSecret;
         @NotBlank
         private String redirectUri;
         @NotBlank
         private String frontendCallbackUrl;
      }

      private final Zalo zalo = new Zalo();

      @Data
      public static class Zalo {
         @NotBlank
         private String clientId;
         @NotBlank
         private String clientSecret;
         @NotBlank
         private String redirectUri;
         @NotBlank
         private String frontendCallbackUrl;
      }
   }

   @Data
   public static class Security {
      @NotBlank
      private String jwtSecret;
      private long jwtExpirationMs;
      @NotBlank
      private String encryptionSecret;
      @NotBlank
      private String hashPepper;
      private String activeKeyId;
      private Map<String, String> keys = new HashMap<>();
      private boolean twoFactorEnabled = true;
      private String credentialEncryptionKeyVersion = "v1";
      private String auditSecret = "defaultAuditSecretKeyForSignatureVerification";
      private String activeFingerprintPepperVersion = "v1";
      private Map<String, String> fingerprintPeppers = new HashMap<>();
      private long refreshGracePeriodMs = 15000;
      private boolean allowLegacyFingerprint = true;
      private boolean enableFingerprintVersioning = true;
      private boolean enableCsrfRotation = true;
      private boolean enableRefreshReplayDetection = true;
      private boolean cookieSecure;
      private String cookieDomain;
      private String csrfSecret;
      private List<String> csrfBypassPatterns = Arrays.asList(
            "/auth/login", "/auth/login/**",
            "/**/auth/login", "/**/auth/login/**",
            "/auth/register", "/auth/register/**",
            "/**/auth/register", "/**/auth/register/**",
            "/auth/refresh", "/auth/refresh/**",
            "/**/auth/refresh", "/**/auth/refresh/**",
            "/auth/logout", "/auth/logout/**",
            "/**/auth/logout", "/**/auth/logout/**",
            "/auth/2fa/login", "/auth/2fa/login/**",
            "/**/auth/2fa/login", "/**/auth/2fa/login/**",
            "/auth/forgot-password", "/auth/forgot-password/**",
            "/**/auth/forgot-password", "/**/auth/forgot-password/**",
            "/auth/reset-password", "/auth/reset-password/**",
            "/**/auth/reset-password", "/**/auth/reset-password/**",
            "/auth/activate-guest", "/auth/activate-guest/**",
            "/**/auth/activate-guest", "/**/auth/activate-guest/**",
            "/auth/finalize-registration", "/auth/finalize-registration/**",
            "/**/auth/finalize-registration", "/**/auth/finalize-registration/**",
            "/payments/*/callback", "/**/payments/*/callback");
   }

   @Data
   public static class Mail {
      @NotBlank
      private String resendApiKey;
      @NotBlank
      private String fromEmail;
   }

   @Data
   public static class Cloudinary {
      @NotBlank
      private String cloudName;
      @NotBlank
      private String apiKey;
      @NotBlank
      private String apiSecret;
   }

   @Data
   public static class Turnstile {
      private boolean enabled = true;
      @NotBlank
      private String secretKey;
      @NotBlank
      private String verifyUrl = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
   }

}
