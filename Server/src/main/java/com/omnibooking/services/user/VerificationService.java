package com.omnibooking.services.user;

import java.util.UUID;

public interface VerificationService {

   String createVerificationToken(UUID userId);

   UUID verifyToken(String token);

}
