# Deployment and Rollback Runbook - Issue #26

This runbook outlines the deployment procedure, validation steps, rollback strategy, and operational criteria for deploying the security hardening and registration flow improvements introduced in Issue #26.

---

## 1. Pre-Deployment Verification

Before executing the deployment, ensure the following steps have been completed and verified on the staging/development environment:

1. **Automated Integration Tests**: Verify all security and integration tests pass successfully.
   ```bash
   mvn test -Dtest=CsrfIntegrationTest
   ```
2. **Static Code Analysis & Compilation**: Ensure the Java code compiles without warnings and the Next.js Client lints with zero errors:
   - Server: `mvn clean test-compile`
   - Client: `npm run lint`
3. **Environment Property Validation**: Verify that the required environment variables are set.

---

## 2. Deployment Procedure

### Step 1: Database Check

Ensure that the PostgreSQL database is active and the Kafka cluster + Redis instances are healthy. No schema changes are required for this deployment, but database access is necessary for registration validation.

### Step 2: Environment Configuration Setup

Configure the environment variables in your production environment config (e.g., `env/.env.prod` or system properties):

- Set `NEXT_PUBLIC_WEB_URL` to your production frontend URL (e.g., `https://booking.zeion.online`).
- Set `CLIENT_URL` to match `NEXT_PUBLIC_WEB_URL`.
- Set `TRUSTED_HOSTS` to contain your trusted hostnames, separated by commas (e.g., `booking.zeion.online,partner.zeion.online,owner.zeion.online`).

### Step 3: Deploy Backend (Spring Boot Server)

1. Deploy the compiled backend jar/container.
2. Monitor server startup logs to ensure the validation in `CookieDomainInitializer` passes.
   > [!IMPORTANT]
   > If the startup validation fails (e.g., due to local/loopback address in production profile or missing `CLIENT_URL`), the server will throw an `IllegalStateException` and fail to start. This is the expected safety behavior.

### Step 4: Deploy Frontend (Next.js Client)

1. Build and deploy the Next.js frontend package.
2. Verify that the client is pointing to the relative path for SSE subscription (`/api/v1/auth/subscribe/{requestId}`) and status polling (`/api/v1/auth/registration-status/{requestId}`).

---

## 3. Post-Deployment Smoke Tests

Immediately after deploying, perform the following validation steps:

1. **New User Registration**: Register a new user account.
   - Verify the registration response returns a 200 status code and does **NOT** return an `accessToken` in the payload.
   - Verify the user receives the registration confirmation email and that the link points to the production URL, not localhost.
2. **SSE/Polling Validation**:
   - Check developer console network tab to ensure the client successfully established an SSE connection.
   - If SSE fails or is blocked by a proxy, verify that the frontend falls back to Polling with exponential backoff (`3s, 5s, 8s, 13s, 21s`) and successfully completes the registration flow.
3. **CSRF Validation on Protected Endpoints**:
   - Navigate to the user profile page and update the display name. Verify it succeeds and passes CSRF check.
   - Set up and enable 2FA (`/auth/2fa/setup` and `/auth/2fa/enable`). Verify that the CSRF token is verified and the action succeeds.

---

## 4. Rollback Strategy & Trigger Criteria

In the event of an operational regression, a rollback must be initiated promptly to minimize user impact.

### Trigger Criteria

| Severity          | Alert Condition       | Metric / Indicator                                                                                                        | Action                                                         |
| :---------------- | :-------------------- | :------------------------------------------------------------------------------------------------------------------------ | :------------------------------------------------------------- |
| **P0 (Critical)** | Core Flow Outage      | Async registration error rate (`registration_failed_total` / `registration_success_total`) **> 2%** over 15-30 minutes.   | **Initiate Rollback Immediately**                              |
| **P0 (Critical)** | Security Lockout      | CSRF rejection rate spike (`csrf_rejected_total` spikes above normal baseline) causing valid user requests to be blocked. | **Initiate Rollback Immediately**                              |
| **P1 (High)**     | Polling Degradation   | Polling timeout rate (`registration_polling_timeout_total`) **> 5%** of all registrations.                                | **Investigation** (Prepare rollback if not resolved in 1 hour) |
| **P2 (Medium)**   | Observability Failure | Metrics not exporting or JSON logs parsing errors in log collector.                                                       | **Fix-forward** (Do not roll back; patch on the next cycle)    |

### Rollback Steps

1. **Revert Frontend Code**: Roll back the Next.js Client deployment to the previous stable release commit.
2. **Revert Backend Code**: Roll back the Spring Boot Server deployment to the previous stable release version (which defaults to using the older `CustomCsrfFilter` without dynamic Same-Origin/trustedHosts check and retains localhost fallback in configuration).
3. **Restore Environment Properties**: If any environment variable was misconfigured, restore `CLIENT_URL` and CORS properties to the previous known-good state.
4. **Deploy & Validate**:
   - Run the smoke test suite to confirm registration and CSRF endpoints are working.
   - Ensure the rollback is completed within the target window of **5-10 minutes**.
