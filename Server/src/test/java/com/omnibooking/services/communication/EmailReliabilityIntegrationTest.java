package com.omnibooking.services.communication;

import com.omnibooking.config.AppProperties;
import com.resend.Resend;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmailReliabilityIntegrationTest {

   private ResendEmailService resendEmailService;

   @Mock
   private AppProperties appProperties;

   @Mock
   private AppProperties.Mail mailProperties;

   @Mock
   private Resend mockResend;

   @Mock
   private Emails mockEmails;

   @BeforeEach
   void setUp() {
      when(appProperties.getMail()).thenReturn(mailProperties);
      when(mailProperties.getResendApiKey()).thenReturn("test-key");
      when(mailProperties.getFromEmail()).thenReturn("noreply@omnibooking.com");

      resendEmailService = new ResendEmailService(appProperties);
      ReflectionTestUtils.setField(resendEmailService, "resend", mockResend);
   }

   @Test
   void testSendEmailWithIdempotencyKey() throws Exception {
      String to = "test@omnibooking.com";
      String subject = "Test Subject";
      String content = "<p>Hello</p>";
      String idempotencyKey = "unique-key-123";

      CreateEmailResponse mockResponse = new CreateEmailResponse();
      ReflectionTestUtils.setField(mockResponse, "id", "email-id-xyz");

      when(mockResend.emails()).thenReturn(mockEmails);
      when(mockEmails.send(any(CreateEmailOptions.class))).thenReturn(mockResponse);

      resendEmailService.sendHtmlEmail(to, subject, content, idempotencyKey);

      verify(mockEmails).send(argThat(options -> options != null &&
            options.getHeaders() != null &&
            idempotencyKey.equals(options.getHeaders().get("Idempotency-Key"))));
   }

   @Test
   void testSendEmailWithoutIdempotencyKey() throws Exception {
      String to = "test@omnibooking.com";
      String subject = "Test Subject";
      String content = "<p>Hello</p>";

      CreateEmailResponse mockResponse = new CreateEmailResponse();
      ReflectionTestUtils.setField(mockResponse, "id", "email-id-xyz");

      when(mockResend.emails()).thenReturn(mockEmails);
      when(mockEmails.send(any(CreateEmailOptions.class))).thenReturn(mockResponse);

      resendEmailService.sendHtmlEmail(to, subject, content);

      verify(mockEmails).send(any(CreateEmailOptions.class));
   }

}
