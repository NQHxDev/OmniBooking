package com.omnibooking.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HashingService {

   private final Argon2PasswordEncoder argon2 = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

   public String hash(String value) {
      return argon2.encode(value);
   }

   public boolean verify(String rawValue, String hashedValue) {
      return argon2.matches(rawValue, hashedValue);
   }

}
