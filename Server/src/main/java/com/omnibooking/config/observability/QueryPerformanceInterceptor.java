package com.omnibooking.config.observability;

import com.omnibooking.context.RequestContext;
import com.omnibooking.context.RequestContextHolder;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryPerformanceInterceptor implements StatementInspector {

   private static final int N_PLUS_ONE_THRESHOLD = 30; // Alert if a single request executes > 30 queries

   @Override
   public String inspect(String sql) {
      RequestContext context = RequestContextHolder.getContext();
      if (context != null) {
         int count = context.getQueryCount().incrementAndGet();
         if (count > N_PLUS_ONE_THRESHOLD) {
            log.warn("[N+1 Warning] Request {} has executed {} queries. Potential N+1 query issue! Last SQL: {}",
                  context.getRequestId(), count, sql);
         }
         if (context.getRequestId() != null) {
            return "/* requestId: " + context.getRequestId() + " */ " + sql;
         }
      }
      return sql;
   }
}
