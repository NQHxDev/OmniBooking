package com.omnibooking.services;

import com.omnibooking.config.AppProperties;
import com.omnibooking.dto.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailService {

   private final EmailProducer emailProducer;
   private final MailTemplateService mailTemplateService;
   private final AppProperties appProperties;

   public void sendVerificationEmail(String toEmail, String userName, String token) {
      // Construct the verification link (pointing to Frontend)
      String verifyLink = appProperties.getClientUrl() + "/auth/verify?token=" + token;

      // Prepare template variables
      Map<String, Object> variables = new HashMap<>();
      variables.put("userName", userName);
      variables.put("verifyLink", verifyLink);

      // Render HTML content
      String htmlContent = mailTemplateService.buildHtmlContent("verify-email", variables);

      // Send to Kafka
      EmailEvent emailEvent = EmailEvent.builder()
            .to(toEmail)
            .subject("Xác nhận tài khoản OmniBooking của bạn")
            .content(htmlContent)
            .build();

      emailProducer.sendEmailEvent(emailEvent);
   }

   public void sendPartnerOtpEmail(String toEmail, String userName, String otpCode) {
      // Prepare template variables
      Map<String, Object> variables = new HashMap<>();
      variables.put("userName", userName);
      variables.put("otpCode", otpCode);

      // Render HTML content
      String htmlContent = mailTemplateService.buildHtmlContent("partner-otp", variables);

      // Send to Kafka
      EmailEvent emailEvent = EmailEvent.builder()
            .to(toEmail)
            .subject("Mã xác thực đối tác OmniBooking")
            .content(htmlContent)
            .build();

      emailProducer.sendEmailEvent(emailEvent);
   }
}
