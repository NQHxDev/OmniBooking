package com.omnibooking.services.core;

import com.omnibooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class BloomFilterWarmupTask implements CommandLineRunner {

   private final UserRepository userRepository;

   private final BloomFilterService bloomFilterService;

   @Override
   public void run(String... args) {
      log.info("Starting Bloom Filter warmup for user emails...");

      // Tạo filter nếu chưa có (1% sai số, sức chứa ban đầu 10,000 users)
      bloomFilterService.createFilter(bloomFilterService.getEmailFilterName(), 0.01, 10000);

      int batchSize = 5000;
      UUID lastId = null;
      long totalWarmedUp = 0;
      boolean hasMore = true;

      // Truy vấn theo trang nhỏ sử dụng Keyset Pagination (u.id > lastId) để tránh
      // tràn bộ nhớ
      // và tối ưu hoá câu truy vấn (sử dụng chỉ mục của khoá chính id)
      Pageable limit = PageRequest.of(0, batchSize);

      while (hasMore) {
         List<Object[]> batch = userRepository.findEmailsForWarmup(lastId, limit);
         if (batch.isEmpty()) {
            hasMore = false;
         } else {
            for (Object[] row : batch) {
               UUID id = (UUID) row[0];
               String email = (String) row[1];
               if (email != null) {
                  bloomFilterService.add(email);
                  totalWarmedUp++;
               }
               lastId = id;
            }
            log.info("Warmed up batch of {} emails, lastId: {}", batch.size(), lastId);
         }
      }

      log.info("Bloom Filter warmup completed. Total warmed up user emails: {}", totalWarmedUp);
   }

}
