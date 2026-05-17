package com.omnibooking.services.auth;

import com.omnibooking.dto.auth.passkey.PasskeyRegistrationOptionsResponse;
import com.omnibooking.dto.auth.passkey.PasskeyRegistrationVerifyRequest;
import com.omnibooking.dto.auth.passkey.PasskeyResponse;

import java.util.List;
import java.util.UUID;

public interface PasskeyService {

   PasskeyRegistrationOptionsResponse generateRegistrationOptions(UUID userId);

   void verifyRegistration(UUID userId, PasskeyRegistrationVerifyRequest request);

   boolean hasPasskeys(UUID userId);

   List<PasskeyResponse> listPasskeys(UUID userId);

   void deletePasskey(UUID userId, UUID passkeyId);

}
