package com.omnibooking.config;

import com.omnibooking.constant.ObservabilityConstants;
import com.omnibooking.constant.ObservabilityConstants.Headers;
import com.omnibooking.constant.ObservabilityConstants.MdcKeys;
import com.omnibooking.context.RequestContext;
import com.omnibooking.context.RequestContextHolder;
import com.omnibooking.config.observability.ModuleTagResolver;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.github.f4b6a3.uuid.UuidCreator;
import io.sentry.Sentry;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class RequestIdFilter implements Filter {

   private final Tracer tracer;

   public RequestIdFilter(@Autowired(required = false) Tracer tracer) {
      this.tracer = tracer;
   }

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
         throws IOException, ServletException {

      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      // Request ID & Correlation ID propagation
      String requestId = httpRequest.getHeader(Headers.REQUEST_ID);
      if (requestId == null || requestId.isEmpty()) {
         requestId = UuidCreator.getTimeOrderedEpoch().toString();
      }

      String correlationId = httpRequest.getHeader(Headers.CORRELATION_ID);
      if (correlationId == null || correlationId.isEmpty()) {
         correlationId = requestId;
      }

      // Trace ID and Span ID extraction from Micrometer Tracer or Sentry
      String traceId = null;
      String spanId = null;
      if (tracer != null && tracer.currentSpan() != null) {
         traceId = tracer.currentSpan().context().traceId();
         spanId = tracer.currentSpan().context().spanId();
      } else {
         io.sentry.ISpan sentrySpan = Sentry.getSpan();
         if (sentrySpan != null) {
            traceId = sentrySpan.getSpanContext().getTraceId().toString();
            spanId = sentrySpan.getSpanContext().getSpanId().toString();
         }
      }

      if (traceId == null || traceId.isEmpty()) {
         traceId = requestId;
      }
      if (spanId == null || spanId.isEmpty()) {
         spanId = "root";
      }

      // Extract User Context if available
      String userId = null;
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
         userId = auth.getName();
      }

      String tenantId = httpRequest.getHeader(Headers.TENANT_ID);
      String sessionId = httpRequest.getSession(false) != null ? httpRequest.getSession(false).getId() : null;
      String environment = System.getenv("SENTRY_ENVIRONMENT");
      if (environment == null) {
         environment = "development";
      }
      String release = System.getenv("APP_VERSION");
      if (release == null) {
         release = "0.1.0";
      }

      // Determine Module based on the request URI path
      String requestUri = httpRequest.getRequestURI();
      String module = ModuleTagResolver.resolveModule(requestUri);

      // Add to MDC
      MDC.put(MdcKeys.REQUEST_ID, requestId);
      MDC.put(MdcKeys.CORRELATION_ID, correlationId);
      MDC.put(MdcKeys.TRACE_ID, traceId);
      MDC.put(MdcKeys.SPAN_ID, spanId);
      MDC.put(MdcKeys.USER_ID, userId != null ? userId : "anonymous");
      MDC.put(MdcKeys.TENANT_ID, tenantId != null ? tenantId : "default");
      MDC.put(MdcKeys.SESSION_ID, sessionId != null ? sessionId : "none");
      MDC.put(MdcKeys.ENVIRONMENT, environment);
      MDC.put(MdcKeys.RELEASE, release);
      MDC.put(MdcKeys.SERVICE_NAME, ObservabilityConstants.SERVICE_NAME);
      MDC.put(MdcKeys.MODULE, module);

      // Save to ServletRequest attributes so controllers/filters can access
      request.setAttribute("requestId", requestId);

      // Configure Sentry Scope
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
            io.sentry.protocol.User sentryUser = new io.sentry.protocol.User();
            sentryUser.setId(finalUserId);
            scope.setUser(sentryUser);
         }
      });

      // Add to Response headers
      httpResponse.setHeader(Headers.REQUEST_ID, requestId);
      httpResponse.setHeader(Headers.CORRELATION_ID, correlationId);

      // Store in RequestContext for global thread context propagation
      RequestContext context = RequestContext.builder()
            .requestId(requestId)
            .traceId(traceId)
            .spanId(spanId)
            .correlationId(correlationId)
            .userId(userId)
            .tenantId(tenantId)
            .sessionId(sessionId)
            .environment(environment)
            .release(release)
            .serviceName(ObservabilityConstants.SERVICE_NAME)
            .module(module)
            .build();
      RequestContextHolder.setContext(context);

      try {
         chain.doFilter(request, response);
      } finally {
         MDC.clear();
         RequestContextHolder.clearContext();
      }
   }

}
