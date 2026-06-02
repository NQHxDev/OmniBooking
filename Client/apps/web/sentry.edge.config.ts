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
   });
}
