package com.omnibooking.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
@Validated
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

}
