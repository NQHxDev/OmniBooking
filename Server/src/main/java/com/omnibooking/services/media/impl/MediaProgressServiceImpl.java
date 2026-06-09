package com.omnibooking.services.media.impl;

import com.omnibooking.config.AppProperties;
import com.omnibooking.dto.MediaProgress;
import com.omnibooking.dto.event.MediaProgressUpdatedEvent;
import com.omnibooking.services.media.MediaProgressService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaProgressServiceImpl implements MediaProgressService {

   private static final String PROGRESS_KEY_PREFIX = "media:progress:";
   private static final String ACTIVE_ZSET_KEY = "media:progress:active";

   private final StringRedisTemplate redisTemplate;
   private final ApplicationEventPublisher eventPublisher;
   private final AppProperties appProperties;

   private DefaultRedisScript<Long> queuedScript;
   private DefaultRedisScript<Long> updateScript;

   @PostConstruct
   void initScripts() {
      queuedScript = new DefaultRedisScript<>();
      queuedScript.setScriptSource(
            new ResourceScriptSource(new ClassPathResource("scripts/media_progress_queued.lua")));
      queuedScript.setResultType(Long.class);

      updateScript = new DefaultRedisScript<>();
      updateScript.setScriptSource(
            new ResourceScriptSource(new ClassPathResource("scripts/media_progress_update.lua")));
      updateScript.setResultType(Long.class);
   }

   @Override
   public void initProgress(UUID propertyId, UUID ownerId, int totalImages) {
      String key = progressKey(propertyId);
      long now = Instant.now().toEpochMilli();
      long ttl = appProperties.getMedia().getProgress().getRetentionTtl();

      Map<String, String> fields = Map.of(
            "total", String.valueOf(totalImages),
            "queued", "0",
            "processed", "0",
            "failed", "0",
            "status", "PROCESSING",
            "percentage", "0",
            "lastUpdatedAt", String.valueOf(now),
            "ownerId", ownerId.toString()
      );

      redisTemplate.opsForHash().putAll(key, fields);
      redisTemplate.expire(key, Duration.ofSeconds(ttl));

      // Add to active ZSET for stall detection
      redisTemplate.opsForZSet().add(ACTIVE_ZSET_KEY, propertyId.toString(), now);

      log.info("[MediaProgress] Initialized progress for property: {} (total: {}, owner: {})",
            propertyId, totalImages, ownerId);
   }

   @Override
   public boolean markQueued(UUID propertyId, String correlationId) {
      AppProperties.Media.Progress config = appProperties.getMedia().getProgress();
      String now = String.valueOf(Instant.now().toEpochMilli());

      List<String> keys = Arrays.asList(
            progressKey(propertyId),
            progressKey(propertyId) + ":queued_ids",
            ACTIVE_ZSET_KEY
      );
      Long result = redisTemplate.execute(queuedScript, keys,
            correlationId,
            now,
            String.valueOf(config.getUploadWeight()),
            String.valueOf(config.getProcessingWeight()),
            propertyId.toString()
      );

      boolean changed = result != null && result == 1L;
      if (changed) {
         refreshTtl(propertyId);
         publishProgressEvent(propertyId);
         log.debug("[MediaProgress] Marked queued for property: {}, correlationId: {}",
               propertyId, correlationId);
      }
      return changed;
   }

   @Override
   public boolean markProcessed(UUID propertyId, String correlationId) {
      return executeUpdateScript(propertyId, correlationId, "processed", "completed_ids");
   }

   @Override
   public boolean markFailed(UUID propertyId, String correlationId) {
      return executeUpdateScript(propertyId, correlationId, "failed", "failed_ids");
   }

   @Override
   public Optional<MediaProgress> getProgress(UUID propertyId) {
      Map<Object, Object> entries = redisTemplate.opsForHash().entries(progressKey(propertyId));
      if (entries.isEmpty()) {
         return Optional.empty();
      }

      return Optional.of(new MediaProgress(
            parseInt(entries, "total"),
            parseInt(entries, "queued"),
            parseInt(entries, "processed"),
            parseInt(entries, "failed"),
            (String) entries.getOrDefault("status", "PROCESSING"),
            parseInt(entries, "percentage"),
            parseLong(entries, "lastUpdatedAt")
      ));
   }

   @Override
   public boolean verifyOwnership(UUID propertyId, UUID userId) {
      Object ownerId = redisTemplate.opsForHash().get(progressKey(propertyId), "ownerId");
      return userId.toString().equals(ownerId);
   }

   @Override
   public void cleanup(UUID propertyId) {
      String key = progressKey(propertyId);
      redisTemplate.delete(Arrays.asList(
            key,
            key + ":queued_ids",
            key + ":completed_ids",
            key + ":failed_ids"
      ));
      redisTemplate.opsForZSet().remove(ACTIVE_ZSET_KEY, propertyId.toString());
      log.info("[MediaProgress] Cleaned up progress data for property: {}", propertyId);
   }

   private boolean executeUpdateScript(UUID propertyId, String correlationId,
         String fieldName, String setSuffix) {
      AppProperties.Media.Progress config = appProperties.getMedia().getProgress();
      String now = String.valueOf(Instant.now().toEpochMilli());

      List<String> keys = Arrays.asList(
            progressKey(propertyId),
            progressKey(propertyId) + ":" + setSuffix,
            ACTIVE_ZSET_KEY
      );
      Long result = redisTemplate.execute(updateScript, keys,
            correlationId,
            fieldName,
            now,
            String.valueOf(config.getUploadWeight()),
            String.valueOf(config.getProcessingWeight()),
            propertyId.toString()
      );

      boolean changed = result != null && result == 1L;
      if (changed) {
         refreshTtl(propertyId);
         publishProgressEvent(propertyId);
         log.debug("[MediaProgress] Marked {} for property: {}, correlationId: {}",
               fieldName, propertyId, correlationId);
      }
      return changed;
   }

   private void publishProgressEvent(UUID propertyId) {
      getProgress(propertyId).ifPresent(progress -> {
         eventPublisher.publishEvent(
               new MediaProgressUpdatedEvent(this, propertyId, progress));
      });
   }

   private void refreshTtl(UUID propertyId) {
      long ttl = appProperties.getMedia().getProgress().getRetentionTtl();
      String key = progressKey(propertyId);
      redisTemplate.expire(key, Duration.ofSeconds(ttl));
      redisTemplate.expire(key + ":queued_ids", Duration.ofSeconds(ttl));
      redisTemplate.expire(key + ":completed_ids", Duration.ofSeconds(ttl));
      redisTemplate.expire(key + ":failed_ids", Duration.ofSeconds(ttl));
   }

   private String progressKey(UUID propertyId) {
      return PROGRESS_KEY_PREFIX + propertyId;
   }

   private int parseInt(Map<Object, Object> map, String field) {
      Object val = map.get(field);
      if (val == null) return 0;
      try {
         return Integer.parseInt(val.toString());
      } catch (NumberFormatException e) {
         return 0;
      }
   }

   private long parseLong(Map<Object, Object> map, String field) {
      Object val = map.get(field);
      if (val == null) return 0L;
      try {
         return Long.parseLong(val.toString());
      } catch (NumberFormatException e) {
         return 0L;
      }
   }

}
