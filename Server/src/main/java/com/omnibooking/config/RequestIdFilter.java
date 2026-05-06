package com.omnibooking.config;

import com.omnibooking.context.RequestContext;
import com.omnibooking.context.RequestContextHolder;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class RequestIdFilter implements Filter {

   private static final String REQUEST_ID_HEADER = "X-Request-ID";

   private static final String MDC_KEY = "requestId";

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
         throws IOException, ServletException {

      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      // Get from header or generate new one
      String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
      if (requestId == null || requestId.isEmpty()) {
         requestId = UUID.randomUUID().toString();
      }

      // Add to MDC for logging
      MDC.put(MDC_KEY, requestId);

      // Add to Response header
      httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

      // Store in request attribute for easy access in Controller/ExceptionHandler
      request.setAttribute(MDC_KEY, requestId);

      // Set in RequestContext for global access in Service/Repo layers
      RequestContext context = RequestContext.builder()
            .requestId(requestId)
            .build();
      RequestContextHolder.setContext(context);

      try {
         chain.doFilter(request, response);
      } finally {
         MDC.remove(MDC_KEY);
         RequestContextHolder.clearContext();
      }
   }

}
