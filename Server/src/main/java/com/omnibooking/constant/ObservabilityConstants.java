package com.omnibooking.constant;

public final class ObservabilityConstants {

   private ObservabilityConstants() {
      // Private constructor to prevent instantiation
   }

   public static final String SERVICE_NAME = "omnibooking-server";

   public static final class Modules {
      private Modules() {
      }

      public static final String AUTH = "auth";
      public static final String BOOKING = "booking";
      public static final String PAYMENT = "payment";
      public static final String INFRASTRUCTURE = "infrastructure";
      public static final String MEDIA = "media";
      public static final String SEARCH = "search";
      public static final String NOTIFICATION = "notification";
   }

   public static final class MdcKeys {
      private MdcKeys() {
      }

      public static final String TRACE_ID = "traceId";
      public static final String SPAN_ID = "spanId";
      public static final String REQUEST_ID = "requestId";
      public static final String CORRELATION_ID = "correlationId";
      public static final String USER_ID = "userId";
      public static final String TENANT_ID = "tenantId";
      public static final String SESSION_ID = "sessionId";
      public static final String ENVIRONMENT = "environment";
      public static final String RELEASE = "release";
      public static final String SERVICE_NAME = "serviceName";
      public static final String MODULE = "module";
   }

   public static final class Headers {
      private Headers() {
      }

      public static final String SENTRY_TRACE = "sentry-trace";
      public static final String BAGGAGE = "baggage";
      public static final String REQUEST_ID = "X-Request-ID";
      public static final String CORRELATION_ID = "X-Correlation-ID";
      public static final String USER_ID = "X-User-ID";
      public static final String TENANT_ID = "X-Tenant-ID";
   }

   public static final class Spans {
      private Spans() {
      }

      public static final String ROOT = "root";
      public static final String KAFKA_CONSUMER = "kafka-consumer";
   }

   public static final class Kafka {
      private Kafka() {
      }

      public static final String TOPIC_PREFIX = "omnibooking-";
   }

}
