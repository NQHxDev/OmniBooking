package com.omnibooking.controller;

import com.omnibooking.context.RequestContextHolder;
import com.omnibooking.dto.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

   @GetMapping
   public ApiResponse<Map<String, String>> check() {
      String requestId = RequestContextHolder.getContext() != null
            ? RequestContextHolder.getContext().getRequestId()
            : "N/A";

      return ApiResponse.success(
            Map.of("status", "UP", "message", "OmniBooking API is running"),
            requestId);
   }

}
