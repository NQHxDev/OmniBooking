package com.omnibooking.context;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@Builder
public class RequestContext {

   private String requestId;

   private String traceId;

   private String spanId;

   private String correlationId;

   private String userId;

   private String tenantId;

   private String sessionId;

   private String environment;

   private String release;

   private String serviceName;

   private String module;

   @Builder.Default
   private final AtomicInteger queryCount = new AtomicInteger(0);

}
