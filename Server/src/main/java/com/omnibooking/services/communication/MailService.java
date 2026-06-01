package com.omnibooking.services.communication;

import com.omnibooking.config.AppProperties;
import com.omnibooking.dto.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailService {

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

   public EmailEvent buildBookingConfirmationEmailEvent(
         String toEmail, String userName, String token, String bookingCode,
         String propertyName, String roomTypeName, String checkInDate, String checkOutDate,
         String totalPrice, String finalPrice, String secondaryTotalPrice, String secondaryFinalPrice) {
      String activateLink = token != null ? (appProperties.getClientUrl() + "/auth/activate?token=" + token) : null;
      Map<String, Object> variables = new HashMap<>();
      variables.put("userName", userName);
      variables.put("activateLink", activateLink);
      variables.put("bookingCode", bookingCode);
      variables.put("propertyName", propertyName);
      variables.put("roomTypeName", roomTypeName);
      variables.put("checkInDate", checkInDate);
      variables.put("checkOutDate", checkOutDate);
      variables.put("totalPrice", totalPrice);
      variables.put("finalPrice", finalPrice);
      variables.put("secondaryTotalPrice", secondaryTotalPrice);
      variables.put("secondaryFinalPrice", secondaryFinalPrice);

      String htmlContent = mailTemplateService.buildHtmlContent("booking-confirmation", variables);

      return EmailEvent.builder()
            .to(toEmail)
            .subject("Xác nhận đặt phòng thành công - OmniBooking")
            .content(htmlContent)
            .build();
   }

   public EmailEvent buildPartnerOtpEmailEvent(String toEmail, String userName, String otpCode) {
      Map<String, Object> variables = new HashMap<>();
      variables.put("userName", userName);
      variables.put("otpCode", otpCode);

      String htmlContent = mailTemplateService.buildHtmlContent("partner-otp", variables);

      return EmailEvent.builder()
            .to(toEmail)
            .subject("Mã xác thực đối tác OmniBooking")
            .content(htmlContent)
            .build();
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

   public EmailEvent buildSecurityOtpEmailEvent(String toEmail, String userName, String otpCode) {
      Map<String, Object> variables = new HashMap<>();
      variables.put("userName", userName);
      variables.put("otpCode", otpCode);
      String htmlContent = mailTemplateService.buildHtmlContent("security-otp", variables);

      return EmailEvent.builder()
            .to(toEmail)
            .subject("Mã xác thực hành động bảo mật OmniBooking")
            .content(htmlContent)
            .build();
   }

   public EmailEvent buildTwoFactorEnabledEmailEvent(String toEmail, String userName) {
      Map<String, Object> variables = new HashMap<>();
      variables.put("userName", userName);
      String htmlContent = mailTemplateService.buildHtmlContent("two-factor-enabled", variables);

      return EmailEvent.builder()
            .to(toEmail)
            .subject("Tính năng xác thực 2 yếu tố đã được kích hoạt")
            .content(htmlContent)
            .build();
   }
}
