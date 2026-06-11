package com.omnibooking.services.communication;

import com.omnibooking.config.AppProperties;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResendEmailService {

   private final Resend resend;

   private final String fromEmail;

   public ResendEmailService(AppProperties appProperties) {
      this.resend = new Resend(appProperties.getMail().getResendApiKey());
      this.fromEmail = appProperties.getMail().getFromEmail();
   }

   @CircuitBreaker(name = "externalService", fallbackMethod = "sendEmailFallback")
   @Retry(name = "externalService")
   public void sendHtmlEmail(String to, String subject, String htmlContent) {
      sendHtmlEmail(to, subject, htmlContent, null);
   }

   @CircuitBreaker(name = "externalService", fallbackMethod = "sendEmailFallback")
   @Retry(name = "externalService")
   public void sendHtmlEmail(String to, String subject, String htmlContent, String idempotencyKey) {
      CreateEmailOptions.Builder paramsBuilder = CreateEmailOptions.builder()
            .from(fromEmail)
            .to(to)
            .subject(subject)
            .html(htmlContent);

      if (idempotencyKey != null && !idempotencyKey.isBlank()) {
         paramsBuilder.headers(java.util.Map.of("Idempotency-Key", idempotencyKey));
      }

      CreateEmailOptions params = paramsBuilder.build();

      try {
         CreateEmailResponse data = resend.emails().send(params);
         log.info("[Resend] Email sent successfully to {}. Message ID: {}", to, data.getId());
      } catch (ResendException e) {
         log.error("[Resend] Failed to send email to {}. Error: {}", to, e.getMessage());
         throw new RuntimeException("Email sending failed", e);
      }
   }

   public void sendEmailFallback(String to, String subject, String htmlContent, Throwable t) {
      log.error("[Fallback] Resilience4j triggered for email to {}. Reason: {}", to, t.getMessage());
   }

   public void sendEmailFallback(String to, String subject, String htmlContent, String idempotencyKey, Throwable t) {
      log.error("[Fallback] Resilience4j triggered for email to {} with idempotencyKey {}. Reason: {}", to,
            idempotencyKey, t.getMessage());
   }

}
