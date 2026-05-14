package com.omnibooking.context;

public class RequestContextHolder {

   private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

   public static void setContext(RequestContext context) {
      CONTEXT.set(context);
   }

   public static RequestContext getContext() {
      return CONTEXT.get();
   }

   public static void clearContext() {
      CONTEXT.remove();
   }

}
