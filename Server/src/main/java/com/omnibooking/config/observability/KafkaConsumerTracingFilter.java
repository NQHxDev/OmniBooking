package com.omnibooking.config.observability;

import com.omnibooking.constant.ObservabilityConstants;
import com.omnibooking.constant.ObservabilityConstants.Headers;
import com.omnibooking.constant.ObservabilityConstants.MdcKeys;
import com.omnibooking.context.RequestContext;
import com.omnibooking.context.RequestContextHolder;

import io.sentry.Sentry;
import io.sentry.protocol.User;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class KafkaConsumerTracingFilter implements RecordInterceptor<String, Object> {

   @Override
   public ConsumerRecord<String, Object> intercept(ConsumerRecord<String, Object> record,
         Consumer<String, Object> consumer) {
      String requestId = getHeaderValue(record, Headers.REQUEST_ID);
      String correlationId = getHeaderValue(record, Headers.CORRELATION_ID);
      String traceId = getHeaderValue(record, Headers.SENTRY_TRACE);
      String userId = getHeaderValue(record, Headers.USER_ID);
      String tenantId = getHeaderValue(record, Headers.TENANT_ID);

      if (requestId == null || requestId.isEmpty()) {
         requestId = java.util.UUID.randomUUID().toString();
      }
      if (correlationId == null || correlationId.isEmpty()) {
         correlationId = requestId;
      }
      if (traceId == null || traceId.isEmpty()) {
         traceId = requestId;
      }

      String environment = System.getenv("SENTRY_ENVIRONMENT");
      if (environment == null) {
         environment = "development";
      }
      String release = System.getenv("APP_VERSION");
      if (release == null) {
         release = "0.1.0";
      }

      // Determine Module based on Topic Name
      String topicName = record.topic();
      String module = ModuleTagResolver.resolveModule(topicName);

      // Set MDC
      MDC.put(MdcKeys.REQUEST_ID, requestId);
      MDC.put(MdcKeys.CORRELATION_ID, correlationId);
      MDC.put(MdcKeys.TRACE_ID, traceId);
      MDC.put(MdcKeys.SPAN_ID, "kafka-consumer");
      MDC.put(MdcKeys.USER_ID, userId != null ? userId : "anonymous");
      MDC.put(MdcKeys.TENANT_ID, tenantId != null ? tenantId : "default");
      MDC.put(MdcKeys.ENVIRONMENT, environment);
      MDC.put(MdcKeys.RELEASE, release);
      MDC.put(MdcKeys.SERVICE_NAME, ObservabilityConstants.SERVICE_NAME);
      MDC.put(MdcKeys.MODULE, module);

      // Restore Sentry Scope
      final String finalUserId = userId;
      final String finalRequestId = requestId;
      final String finalCorrelationId = correlationId;
      final String finalTenantId = tenantId;
      final String finalModule = module;
      Sentry.configureScope(scope -> {
         scope.setTag(MdcKeys.REQUEST_ID, finalRequestId);
         scope.setTag(MdcKeys.CORRELATION_ID, finalCorrelationId);
         scope.setTag(MdcKeys.MODULE, finalModule);
         if (finalTenantId != null) {
            scope.setTag(MdcKeys.TENANT_ID, finalTenantId);
         }
         if (finalUserId != null) {
            User sentryUser = new User();
            sentryUser.setId(finalUserId);
            scope.setUser(sentryUser);
         }
      });

      // Set in RequestContextHolder
      RequestContext context = RequestContext.builder()
            .requestId(requestId)
            .traceId(traceId)
            .spanId("kafka-consumer")
            .correlationId(correlationId)
            .userId(userId)
            .tenantId(tenantId)
            .environment(environment)
            .release(release)
            .serviceName(ObservabilityConstants.SERVICE_NAME)
            .module(module)
            .build();
      RequestContextHolder.setContext(context);

      return record;
   }

   @Override
   public void afterRecord(ConsumerRecord<String, Object> record, Consumer<String, Object> consumer) {
      MDC.clear();
      RequestContextHolder.clearContext();
   }

   private String getHeaderValue(ConsumerRecord<?, ?> record, String headerName) {
      Header header = record.headers().lastHeader(headerName);
      return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
   }

}
