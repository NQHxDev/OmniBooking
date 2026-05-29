package com.omnibooking.services.core;

import com.omnibooking.model.User;
import com.omnibooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BloomFilterWarmupTask implements CommandLineRunner {

   private final UserRepository userRepository;
   private final BloomFilterService bloomFilterService;

   @Override
   public void run(String... args) {
      log.info("Starting Bloom Filter warmup for user emails...");

      // 1. Tạo filter nếu chưa có (1% sai số, sức chứa ban đầu 10,000 users)
      bloomFilterService.createFilter(bloomFilterService.getEmailFilterName(), 0.01, 10000);

      // 2. Load tất cả email hiện có từ DB vào Bloom Filter
      // Lưu ý: Trong dự án cực lớn, ta nên dùng streaming/paging để tránh tràn bộ nhớ
      List<String> emails = userRepository.findAll().stream()
            .map(User::getEmail)
            .toList();

      if (!emails.isEmpty()) {
         emails.forEach(bloomFilterService::add);
         log.info("Warmed up Bloom Filter with {} emails.", emails.size());
      } else {
         log.info("No emails found in DB for Bloom Filter warmup.");
      }
   }
}
