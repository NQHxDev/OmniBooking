package com.omnibooking.services.auth;

import com.omnibooking.dto.TwoFactorSetupResponse;
import java.util.List;
import java.util.UUID;

public interface TwoFactorAuthService {

   TwoFactorSetupResponse initiate2FA(UUID userId);

   List<String> enable2FA(UUID userId, String code);

   void disable2FA(UUID userId, String code);

   void remove2FA(UUID userId, String code);

   boolean verifyCode(UUID userId, String code);

   boolean is2FAEnabledForUser(UUID userId);

   String get2FAStatusString(UUID userId);

}
