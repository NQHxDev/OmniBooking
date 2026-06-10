package com.omnibooking.services.media;

import com.omnibooking.config.AppProperties;
import com.omnibooking.dto.MediaProgress;
import com.omnibooking.dto.event.MediaProgressUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Scheduled component that detects stalled media upload jobs using a Redis Sorted Set index.
 * Uses ZRANGEBYSCORE (O(log N + M)) instead of SCAN for predictable performance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaStallDetector {

   private static final String ACTIVE_ZSET_KEY = "media:progress:active";

   private final StringRedisTemplate redisTemplate;
   private final MediaProgressService progressService;
   private final ApplicationEventPublisher eventPublisher;
   private final AppProperties appProperties;

   /**
    * Scans for stalled jobs using ZSET index.
    * O(log N + M) where M = number of stale results.
    */
   @Scheduled(fixedRateString = "${app.media.progress.stall-check-interval:30}000")
   public void detectStalledJobs() {
      var zsetOps = redisTemplate.opsForZSet();
      if (zsetOps == null) {
         return;
      }
      long stallTimeout = appProperties.getMedia().getProgress().getStallTimeout() * 1000L;
      double staleThreshold = Instant.now().toEpochMilli() - stallTimeout;

      Set<String> stalePropertyIds = zsetOps
            .rangeByScore(ACTIVE_ZSET_KEY, 0, staleThreshold);

      if (stalePropertyIds == null || stalePropertyIds.isEmpty()) return;

      log.info("[StallDetector] Found {} potentially stalled jobs", stalePropertyIds.size());

      for (String propertyIdStr : stalePropertyIds) {
         try {
            UUID propertyId = UUID.fromString(propertyIdStr);
            progressService.getProgress(propertyId).ifPresent(progress -> {
               if ("PROCESSING".equals(progress.status())) {
                  // Transition to STALLED
                  redisTemplate.opsForHash().put(
                        "media:progress:" + propertyIdStr,
                        "status", "STALLED");

                  MediaProgress stalledProgress = new MediaProgress(
                        progress.total(), progress.queued(),
                        progress.processed(), progress.failed(),
                        "STALLED", progress.percentage(),
                        progress.lastUpdatedAt()
                  );

                  eventPublisher.publishEvent(
                        new MediaProgressUpdatedEvent(this, propertyId, stalledProgress));

                  log.warn("[StallDetector] Property {} marked as STALLED " +
                              "(last update: {}ms ago, threshold: {}ms)",
                        propertyId,
                        Instant.now().toEpochMilli() - progress.lastUpdatedAt(),
                        stallTimeout);
               }
            });
         } catch (IllegalArgumentException e) {
            log.error("[StallDetector] Invalid UUID in ZSET: {}", propertyIdStr, e);
         }
      }
   }

}
