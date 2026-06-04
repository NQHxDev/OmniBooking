package com.omnibooking.config.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

   private final UserSeeder userSeeder;

   private final PropertySeeder propertySeeder;

   @Override
   public void run(String... args) throws Exception {
      boolean hasSeedArg = false;
      for (String arg : args) {
         if ("--seed".equals(arg)) {
            hasSeedArg = true;
            break;
         }
      }

      if (hasSeedArg) {
         log.info("Force seeding active! Triggering cleanup in correct dependency order...");
         propertySeeder.cleanUp();
         userSeeder.cleanUp();
      }

      userSeeder.seed(hasSeedArg);
      propertySeeder.seed(hasSeedArg);
   }

}
