package com.omnibooking.controller;

import com.omnibooking.config.AppProperties;
import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.MediaProgress;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.services.media.MediaProgressService;
import com.omnibooking.services.media.SseProgressDispatcher;
import com.omnibooking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/media/progress")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media Progress", description = "Endpoints for tracking media upload progress")
public class MediaProgressController {

   private final MediaProgressService progressService;
   private final SseProgressDispatcher sseDispatcher;
   private final AppProperties appProperties;

   @PostMapping("/{propertyId}/init")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Initialize progress tracking for a property upload batch")
   public ApiResponse<Void> initProgress(
         @PathVariable UUID propertyId,
         @RequestParam int total) {
      UUID ownerId = SecurityUtils.getCurrentUserId();
      progressService.initProgress(propertyId, ownerId, total);

      log.info("[MediaProgress] Initialized tracking for property: {} (total: {}, owner: {})",
            propertyId, total, ownerId);

      return ApiResponse.success(null);
   }

   @GetMapping(value = "/{propertyId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "SSE stream for real-time progress updates (ownership-validated)")
   public SseEmitter streamProgress(@PathVariable UUID propertyId) {
      enforceOwnership(propertyId);

      long timeout = appProperties.getMedia().getProgress().getSseTimeout();
      SseEmitter emitter = new SseEmitter(timeout);

      // Send current state immediately on connect
      progressService.getProgress(propertyId).ifPresent(progress -> {
         try {
            emitter.send(SseEmitter.event()
                  .name("progress")
                  .data(progress));
         } catch (IOException e) {
            log.error("[SSE] Failed to send initial progress for property: {}", propertyId, e);
            emitter.completeWithError(e);
            return;
         }
      });

      // Register for future event-driven updates
      sseDispatcher.register(propertyId, emitter);

      log.debug("[SSE] Client connected to progress stream for property: {}", propertyId);
      return emitter;
   }

   @GetMapping("/{propertyId}")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Get current progress snapshot (REST fallback, ownership-validated)")
   public ApiResponse<MediaProgress> getProgress(@PathVariable UUID propertyId) {
      enforceOwnership(propertyId);

      return progressService.getProgress(propertyId)
            .map(ApiResponse::success)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
   }

   /**
    * Enforce property ownership before returning any progress data.
    * Returns 403 Forbidden if the current user does not own the property.
    */
   private void enforceOwnership(UUID propertyId) {
      UUID currentUserId = SecurityUtils.getCurrentUserId();
      if (!progressService.verifyOwnership(propertyId, currentUserId)) {
         throw new AppException(ErrorCode.NOT_PROPERTY_OWNER);
      }
   }

}
