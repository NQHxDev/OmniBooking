import * as Sentry from "@sentry/nextjs";

const SENTRY_DSN = process.env.NEXT_PUBLIC_SENTRY_DSN;

if (SENTRY_DSN) {
   Sentry.init({
      dsn: SENTRY_DSN,

      // Adjust this value in production, or use tracesSampler for greater control
      tracesSampleRate: process.env.NODE_ENV === "production" ? 0.2 : 1.0,

      // Setting this option to true will print useful information to the console while you're setting up Sentry.
      debug: false,

      environment: process.env.NODE_ENV || "development",
      release: `omnibooking-client@${process.env.NEXT_PUBLIC_APP_VERSION || "0.1.0"}`,

      // Session Replay
      replaysSessionSampleRate: 0.02, // 2% in normal sessions
      replaysOnErrorSampleRate: 1.0, // 100% on error sessions

      integrations: [
         Sentry.replayIntegration({
            maskAllText: true,
            blockAllMedia: true,
         }),
      ],

      ignoreErrors: [
         "AUTH_006", // Token expired
         "ResizeObserver loop limit exceeded",
         "ResizeObserver loop completed with undelivered notifications",
         "Network Error",
         "Failed to fetch",
      ],

      // PII Sanitization
      beforeSend(event) {
         // Sanitize request/url/user data if any email or passwords exist
         if (event.user && event.user.email) {
            event.user.email = "[MASKED_EMAIL]";
         }

         // Simple regex masking for typical credentials in requests
         if (event.request && event.request.headers) {
            const sensitiveHeaders = ["authorization", "cookie", "x-fgp"];
            event.request.headers = Object.keys(event.request.headers).reduce(
               (acc, key) => {
                  if (sensitiveHeaders.includes(key.toLowerCase())) {
                     acc[key] = "[MASKED_SENSITIVE_HEADER]";
                  } else {
                     acc[key] = event.request!.headers![key];
                  }
                  return acc;
               },
               {} as Record<string, string>
            );
         }

         return event;
      },
   });
}
