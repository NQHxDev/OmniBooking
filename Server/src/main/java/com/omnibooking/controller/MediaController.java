package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.event.MediaUploadEvent;
import com.omnibooking.services.media.MediaProducer;
import com.omnibooking.services.media.MediaProgressService;
import com.omnibooking.constant.MediaConstants;
import com.omnibooking.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import com.github.f4b6a3.uuid.UuidCreator;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media Management", description = "Endpoints for testing and managing media uploads")
public class MediaController {

   private final MediaProducer mediaProducer;

   private final MediaProgressService mediaProgressService;

   private final AppProperties appProperties;

   @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER) and !hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).ADMIN)")
   @Operation(summary = "Upload media for property (Partner Only)")
   public ApiResponse<String> upload(@RequestParam("file") MultipartFile file,
         @RequestParam("entityId") String entityId,
         @RequestParam("entityType") String entityType,
         @RequestParam(value = "isMain", defaultValue = "false") boolean isMain) throws IOException {
      String correlationId = UuidCreator.getTimeOrderedEpoch().toString();

      log.info("[Media Controller] Received upload request for entity: {} ({}). CorrelationId: {}",
            entityId, entityType, correlationId);

      MediaUploadEvent event = MediaUploadEvent.builder()
            .correlationId(correlationId)
            .fileBytes(file.getBytes())
            .folder(MediaConstants.getPropertyFolder(appProperties.getCloudinary().getPropertiesBaseFolder(), entityId))
            .fileName(file.getOriginalFilename())
            .entityId(entityId)
            .entityType(entityType)
            .isMain(isMain)
            .build();

      mediaProducer.sendUploadEvent(event);

      // Track queued progress (idempotent via Lua script)
      try {
         mediaProgressService.markQueued(
               java.util.UUID.fromString(event.getEntityId()),
               event.getCorrelationId());
      } catch (Exception e) {
         log.warn("[Media Controller] Failed to track queued progress for entity: {}",
               event.getEntityId(), e);
      }

      return ApiResponse.success("Media upload started! Processing in background with ID: " + correlationId);
   }

}
