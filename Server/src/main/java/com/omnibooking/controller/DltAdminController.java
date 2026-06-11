package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.services.user.RegistrationDltReplayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/dlt")
@RequiredArgsConstructor
@Slf4j
public class DltAdminController {

   private final RegistrationDltReplayService replayService;

   @PostMapping("/replay/{requestId}")
   public ResponseEntity<ApiResponse<Boolean>> replayRequest(
         @PathVariable String requestId,
         Principal principal,
         HttpServletRequest httpRequest) {

      String reqIdStr = (String) httpRequest.getAttribute("requestId");
      UUID reqId = UUID.fromString(requestId);
      String initiator = principal != null ? principal.getName() : "SYSTEM_ADMIN";

      boolean success = replayService.replayRequest(reqId, initiator);

      return ResponseEntity.ok(ApiResponse.success(success,
            success ? "DLT request successfully replayed" : "DLT request replay failed", reqIdStr));
   }

   @PostMapping("/replay/batch")
   public ResponseEntity<ApiResponse<Integer>> replayBatch(
         @RequestBody List<String> requestIds,
         Principal principal,
         HttpServletRequest httpRequest) {

      String reqIdStr = (String) httpRequest.getAttribute("requestId");
      String initiator = principal != null ? principal.getName() : "SYSTEM_ADMIN";

      List<UUID> uuids = requestIds.stream()
            .map(UUID::fromString)
            .collect(Collectors.toList());

      int successCount = replayService.replayBatch(uuids, initiator);

      return ResponseEntity.ok(ApiResponse.success(successCount,
            "DLT batch replay completed. Success count: " + successCount, reqIdStr));
   }

   @PostMapping("/replay/partition/{partitionId}")
   public ResponseEntity<ApiResponse<Integer>> replayPartition(
         @PathVariable int partitionId,
         Principal principal,
         HttpServletRequest httpRequest) {

      String reqIdStr = (String) httpRequest.getAttribute("requestId");
      String initiator = principal != null ? principal.getName() : "SYSTEM_ADMIN";

      int successCount = replayService.replayPartition(partitionId, initiator);

      return ResponseEntity.ok(ApiResponse.success(successCount,
            "DLT partition replay completed. Success count: " + successCount, reqIdStr));
   }

   @PostMapping("/replay/all")
   public ResponseEntity<ApiResponse<Integer>> replayAll(
         Principal principal,
         HttpServletRequest httpRequest) {

      String reqIdStr = (String) httpRequest.getAttribute("requestId");
      String initiator = principal != null ? principal.getName() : "SYSTEM_ADMIN";

      int successCount = replayService.replayAll(initiator);

      return ResponseEntity.ok(ApiResponse.success(successCount,
            "DLT replay all completed. Success count: " + successCount, reqIdStr));
   }

}
