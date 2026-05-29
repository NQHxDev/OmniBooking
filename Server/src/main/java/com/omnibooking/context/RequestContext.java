package com.omnibooking.context;

import lombok.Builder;
import lombok.Data;

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
   private final java.util.concurrent.atomic.AtomicInteger queryCount = new java.util.concurrent.atomic.AtomicInteger(0);

}


