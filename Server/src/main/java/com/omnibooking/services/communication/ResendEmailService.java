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
      CreateEmailOptions params = CreateEmailOptions.builder()
            .from(fromEmail)
            .to(to)
            .subject(subject)
            .html(htmlContent)
            .build();

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
      // Here you could save the email to a 'failed_emails' table for later manual retry
   }
}
