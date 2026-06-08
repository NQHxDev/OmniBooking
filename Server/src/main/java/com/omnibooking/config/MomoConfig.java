package com.omnibooking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "momo")
public class MomoConfig {

   private String partnerCode;

   private String accessKey;

   private String secretKey;

   private String apiUrl;

   private String returnUrl;

   private String notifyUrl;

}
