package com.omnibooking.services.auth;

import java.util.UUID;

public interface SecurityVerificationService {
   /**
    * Gửi mã OTP xác thực các hành động nhạy cảm qua Email.
    */
   void sendSecurityOTP(UUID userId, String email);

   /**
    * Xác thực mã OTP. Nếu đúng, đánh dấu phiên làm việc là "Trusted" trong 30
    * phút.
    */
   boolean verifySecurityOTP(UUID userId, String otp);

   /**
    * Kiểm tra xem phiên làm việc hiện tại của User có đang trong trạng thái
    * "Trusted" (30p) hay không.
    */
   boolean isSessionTrusted(UUID userId);
}
