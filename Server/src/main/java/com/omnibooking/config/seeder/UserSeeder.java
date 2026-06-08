package com.omnibooking.config.seeder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.constant.SecurityConstants;
import com.omnibooking.model.Role;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.services.auth.CachedRoleService;
import com.omnibooking.repository.user.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSeeder {

   private final UserRepository userRepository;

   private final CachedRoleService cachedRoleService;

   private final PasswordEncoder passwordEncoder;

   private final ObjectMapper objectMapper;

   @PersistenceContext
   private EntityManager entityManager;

   @Transactional
   public void cleanUp() {
      log.info("Cleaning up existing test users ending with @omnibooking.com...");
      List<User> existingTestUsers = userRepository.findByEmailEndingWith("@omnibooking.com");
      if (!existingTestUsers.isEmpty()) {
         userRepository.deleteAll(existingTestUsers);
         entityManager.flush();
         entityManager.clear();
         log.info("Successfully deleted {} existing test users.", existingTestUsers.size());
      }
   }

   @Transactional
   public void seed(boolean force) {
      long userCount = userRepository.count();
      if (userCount > 0 && !force) {
         return;
      }

      Role userRole;
      try {
         userRole = cachedRoleService.getRoleByName(SecurityConstants.Roles.USER);
      } catch (Exception e) {
         log.error("ROLE_USER not found in database! Seeding users skipped...", e);
         return;
      }
      Set<Role> roles = Collections.singleton(userRole);

      List<MockUserDto> mockUsers;
      try (InputStream is = getClass().getResourceAsStream("/mock-users.json")) {
         if (is == null) {
            log.error("mock-users.json not found in resources! Please run 'make generate-mock-users' first...");
            return;
         }
         mockUsers = objectMapper.readValue(is, new TypeReference<List<MockUserDto>>() {
         });
      } catch (Exception e) {
         log.error("Failed to read or parse mock-users.json", e);
         return;
      }

      int totalToSeed = mockUsers.size();

      String encodedPassword = passwordEncoder.encode("Anhhung1@");

      int batchSize = 1000;
      int count = 0;

      for (int i = 0; i < totalToSeed; i++) {
         MockUserDto dto = mockUsers.get(i);

         User user = User.builder()
               .username(dto.getUsername())
               .email(dto.getEmail())
               .password(encodedPassword)
               .roles(roles)
               .isActive(true)
               .tokenVersion(0)
               .build();

         UserProfile profile = UserProfile.builder()
               .user(user)
               .displayName(dto.getDisplayName())
               .gender(dto.getGender())
               .address(dto.getAddress())
               .nationality(dto.getNationality())
               .points(0)
               .reputationScore(100.0)
               .isVerified(ThreadLocalRandom.current().nextBoolean())
               .build();

         user.setProfile(profile);

         entityManager.persist(user);
         entityManager.persist(profile);

         count++;

         if (count % batchSize == 0) {
            entityManager.flush();
            entityManager.clear();
            log.info("Seeded {}/{} users...", count, totalToSeed);
         }
      }
      entityManager.flush();
      entityManager.clear();
      log.info("Successfully seeded all {}/{} users into the database!", count, totalToSeed);
   }

   @Data
   public static class MockUserDto {
      private String username;
      private String email;
      private String displayName;
      private String gender;
      private String address;
      private String nationality;
   }

}
