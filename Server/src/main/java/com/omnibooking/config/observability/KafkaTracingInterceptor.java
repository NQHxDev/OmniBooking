package com.omnibooking.config.observability;

import com.omnibooking.constant.ObservabilityConstants.Headers;
import com.omnibooking.context.RequestContext;
import com.omnibooking.context.RequestContextHolder;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class KafkaTracingInterceptor implements ProducerInterceptor<String, Object> {

   @Override
   public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
      RequestContext context = RequestContextHolder.getContext();
      if (context != null) {
         if (context.getRequestId() != null) {
            record.headers().add(new RecordHeader(Headers.REQUEST_ID, context.getRequestId().getBytes(StandardCharsets.UTF_8)));
         }
         if (context.getCorrelationId() != null) {
            record.headers().add(new RecordHeader(Headers.CORRELATION_ID, context.getCorrelationId().getBytes(StandardCharsets.UTF_8)));
         }
         if (context.getTraceId() != null) {
            record.headers().add(new RecordHeader(Headers.SENTRY_TRACE, context.getTraceId().getBytes(StandardCharsets.UTF_8)));
         }
         if (context.getUserId() != null) {
            record.headers().add(new RecordHeader(Headers.USER_ID, context.getUserId().getBytes(StandardCharsets.UTF_8)));
         }
         if (context.getTenantId() != null) {
            record.headers().add(new RecordHeader(Headers.TENANT_ID, context.getTenantId().getBytes(StandardCharsets.UTF_8)));
         }
      }
      return record;
   }

   @Override
   public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
   }

   @Override
   public void close() {
   }

   @Override
   public void configure(Map<String, ?> configs) {
   }
}
