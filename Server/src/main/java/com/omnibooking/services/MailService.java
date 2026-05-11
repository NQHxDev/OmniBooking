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

   public EmailEvent buildVerificationEmailEvent(String toEmail, String userName, String token) {
      String verifyLink = appProperties.getClientUrl() + "/auth/verify?token=" + token;
      Map<String, Object> variables = new HashMap<>();
      variables.put("userName", userName);
      variables.put("verifyLink", verifyLink);
      String htmlContent = mailTemplateService.buildHtmlContent("verify-email", variables);

      return EmailEvent.builder()
            .to(toEmail)
            .subject("Xác nhận tài khoản OmniBooking của bạn")
            .content(htmlContent)
            .build();
   }

   public void sendVerificationEmail(String toEmail, String userName, String token) {
      emailProducer.sendEmailEvent(buildVerificationEmailEvent(toEmail, userName, token));
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

   public EmailEvent buildForgotPasswordEmailEvent(String toEmail, String userName, String token) {
      String resetLink = appProperties.getClientUrl() + "/auth/reset-password?token=" + token;
      Map<String, Object> variables = new HashMap<>();
      variables.put("userName", userName);
      variables.put("resetLink", resetLink);
      String htmlContent = mailTemplateService.buildHtmlContent("reset-password", variables);

      return EmailEvent.builder()
            .to(toEmail)
            .subject("Đặt lại mật khẩu OmniBooking")
            .content(htmlContent)
            .build();
   }

   public void sendForgotPasswordEmail(String toEmail, String userName, String token) {
      emailProducer.sendEmailEvent(buildForgotPasswordEmailEvent(toEmail, userName, token));
   }

}
