package com.omnibooking.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.model.Role;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.repository.RoleRepository;
import com.omnibooking.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

   private final UserRepository userRepository;
   private final RoleRepository roleRepository;
   private final PasswordEncoder passwordEncoder;
   private final ObjectMapper objectMapper;

   @PersistenceContext
   private EntityManager entityManager;

   @Override
   @Transactional
   public void run(String... args) throws Exception {
      boolean hasSeedArg = false;
      for (String arg : args) {
         if ("--seed".equals(arg)) {
            hasSeedArg = true;
            break;
         }
      }

      long userCount = userRepository.count();
      // Only seed if DB is empty of test users or if --seed argument is explicitly provided
      if (userCount > 0 && !hasSeedArg) {
         log.info("Database already contains users (count: {}). Seeding skipped.", userCount);
         return;
      }

      log.info("Starting database seeding process...");

      if (hasSeedArg) {
         log.info("Force seeding active. Cleaning up existing test users ending with @omnibooking.com...");
         List<User> existingTestUsers = userRepository.findByEmailEndingWith("@omnibooking.com");
         if (!existingTestUsers.isEmpty()) {
            userRepository.deleteAll(existingTestUsers);
            // Flush changes to ensure they are deleted from DB before we insert
            entityManager.flush();
            entityManager.clear();
            log.info("Successfully deleted {} existing test users.", existingTestUsers.size());
         }
      }

      // Fetch the ROLE_USER role
      Optional<Role> userRoleOpt = roleRepository.findByName("ROLE_USER");
      if (userRoleOpt.isEmpty()) {
         log.error("ROLE_USER not found in database! Make sure Flyway migrations have run successfully.");
         return;
      }
      Role userRole = userRoleOpt.get();
      Set<Role> roles = Collections.singleton(userRole);

      // Read mock users from JSON file
      log.info("Reading mock-users.json from classpath...");
      List<MockUserDto> mockUsers;
      try (InputStream is = getClass().getResourceAsStream("/mock-users.json")) {
         if (is == null) {
            log.error("mock-users.json not found in resources! Please run 'make generate-mock-users' first.");
            return;
         }
         mockUsers = objectMapper.readValue(is, new TypeReference<List<MockUserDto>>() {});
      } catch (Exception e) {
         log.error("Failed to read or parse mock-users.json", e);
         return;
      }

      int totalToSeed = mockUsers.size();
      log.info("Parsed {} mock users to seed.", totalToSeed);

      // Hash the password ONCE for extreme performance boost
      log.info("Pre-encoding default password 'Anhhung1@' (Argon2)...");
      String encodedPassword = passwordEncoder.encode("Anhhung1@");
      log.info("Password encoded successfully. Starting batch insert...");

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
               .isVerified(true)
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

      // Flush any remaining entities
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
