# Operations Handbook & Production Support Guide - Issue #26

This handbook provides guidance for operations and production support teams to monitor, alert on, and troubleshoot the registration flow and CSRF security subsystems.

---

## 1. Operational Observability Architecture

The system implements a multi-dimensional observability stack to monitor high-concurrency authentication and registration flows:

1. **Structured JSON Logs**: Unified logs written in structured JSON formatting, including metadata tags (e.g., `event`, `email`, `timestamp`).
2. **Correlation IDs (MDC)**: The unique `requestId` is propagated across async boundaries (HTTP Controller -> Kafka Consumer Thread -> Redis Pub/Sub -> SSE Broadcast Thread) and indexed as an MDC value.
3. **Application Metrics**: Operational counters exposed via Micrometer to Prometheus and visualized in Grafana.

---

## 2. Monitoring Metrics Reference

The following Micrometer metrics are exposed under `/actuator/prometheus`:

### Registration Metrics

| Metric Name                              | Type    | Description                                                                               |
| :--------------------------------------- | :------ | :---------------------------------------------------------------------------------------- |
| `registration_success_total`             | Counter | Total successful asynchronous registration flow completions.                              |
| `registration_failed_total`              | Counter | Total async registration flow failures (due to duplicate email, validation errors, etc.). |
| `registration_sse_success_total`         | Counter | Number of registration completion broadcasts successfully delivered via SSE.              |
| `registration_polling_fallback_total`    | Counter | Number of times a client falls back to status polling due to SSE failures.                |
| `registration_polling_success_total`     | Counter | Number of times status polling successfully resolves a registration.                      |
| `registration_polling_timeout_total`     | Counter | Number of status polling requests that timed out (reached max backoff retries).           |
| `registration_status_rate_limited_total` | Counter | Number of times a status check polling API request was rate-limited (429).                |

### Security (CSRF) Metrics

| Metric Name                 | Type    | Description                                                                                    |
| :-------------------------- | :------ | :--------------------------------------------------------------------------------------------- |
| `csrf_rejected_total`       | Counter | Total requests blocked by the CSRF protection filter.                                          |
| `csrf_origin_invalid_total` | Counter | Requests rejected specifically because the `Origin` or `Referer` failed the Same-Origin check. |
| `csrf_token_invalid_total`  | Counter | Requests rejected because the client-provided `csrf_token` mismatch or signature is invalid.   |

---

## 3. Recommended Prometheus Alerting Rules

Configure the following alert thresholds in Prometheus:

### Alert: High Registration Failure Rate

- **Expression**: `rate(registration_failed_total[5m]) / (rate(registration_success_total[5m]) + rate(registration_failed_total[5m])) > 0.02`
- **Duration**: `5m`
- **Severity**: `page` (Critical)
- **Description**: Async registration failure rate exceeds 2% over a 5-minute window. Indicates potential issues with Kafka, PostgreSQL, or external mail providers.

### Alert: CSRF Rejection Spike

- **Expression**: `increase(csrf_rejected_total[5m]) > 100`
- **Duration**: `2m`
- **Severity**: `warning`
- **Description**: Spike in CSRF rejections (more than 100 blocks in 5 minutes). Could indicate a credential stuffing/CSRF attack or a misconfigured CORS client origin.

### Alert: High Polling Fallback Usage

- **Expression**: `rate(registration_polling_fallback_total[5m]) / rate(registration_success_total[5m]) > 0.30`
- **Duration**: `10m`
- **Severity**: `warning`
- **Description**: More than 30% of registration flows are falling back to polling instead of SSE. Indicates issues with gateway buffering, proxy configurations, or persistent client SSE connection failures.

### Alert: Excessive Status API Rate Limiting

- **Expression**: `increase(registration_status_rate_limited_total[5m]) > 50`
- **Duration**: `5m`
- **Severity**: `warning`
- **Description**: More than 50 status checks are rate-limited. Indicates a rogue polling client or a potential denial-of-service attempt on the registration status check API.

---

## 4. Grafana Dashboards

Two main dashboards should be built to visualize these operational metrics:

### A. Registration Flow Dashboard

1. **Registration Success Rate**: Graph comparing `registration_success_total` vs `registration_failed_total`.
2. **Channel Efficiency**: Pie chart representing SSE success (`registration_sse_success_total`) vs Polling fallback (`registration_polling_fallback_total`).
3. **Polling Outcomes**: Counter metrics for `registration_polling_success_total` vs `registration_polling_timeout_total`.
4. **Rate Limits**: Graph of `registration_status_rate_limited_total` over time.

### B. Security & CSRF Dashboard

1. **Total CSRF Rejections**: Time-series graph of `csrf_rejected_total`.
2. **Rejection Breakdown**: Stacked bar chart showing `csrf_origin_invalid_total` (Origin/Referer failures) vs `csrf_token_invalid_total` (Token discrepancies).
3. **Invalid Origin Source Domains**: Table displaying request referrer domains that triggered `csrf_origin_invalid_total` (extracted from access logs).

---

## 5. Troubleshooting & Incident Response Procedures

When an incident is reported (e.g., users reporting "registration failed" or "session blocked"), follow these diagnostic steps:

### Step 1: Obtain the Request ID

Every registration flow is assigned a unique `requestId` (UUIDv7) at the client.

- Ask the user/client application for the `requestId` from the error toast or application logs.
- If unavailable, retrieve the user's email address.

### Step 2: Query Structured JSON Logs

Search your log aggregation system (Elasticsearch/Kibana, Grafana Loki, etc.) for the specific `requestId` or `email`. Because logs are written in structured JSON, they can be parsed and filtered cleanly:

```bash
# Example query for Kibana/Loki:
{app="omnibooking"} | json | requestId="YOUR_REQUEST_ID_HERE"
```

#### Expected Happy Path Event Timeline:

1. `{"event":"registration_received","email":"...","requestId":"..."}` (API endpoint hit)
2. `{"event":"registration_queued","requestId":"..."}` (Published to Kafka topic)
3. `{"event":"registration_consume_start","requestId":"..."}` (Kafka consumer started processing)
4. `{"event":"registration_saved","requestId":"..."}` (Saved to Database)
5. `{"event":"registration_cache_saved","requestId":"..."}` (Cached in Redis)
6. `{"event":"registration_sse_broadcast_received","requestId":"..."}` (Received over Redis Pub/Sub)
7. `{"event":"registration_sse_delivered","requestId":"..."}` (Sent to client via SSE connection)

#### Identifying Common Failure Patterns:

- **Missing Kafka Event**: If `registration_queued` is logged but `registration_consume_start` is missing, check Kafka cluster health and partition assignments.
- **Duplicate Email Rejection**: Look for `registration_failed` with log details indicating duplicate email constraints.
- **SSE Connection Failure**: If the timeline stops at `registration_cache_saved` and client logs show polling fallback, it means the client is now polling. Look for `registration_status_checked` events.

### Step 3: Troubleshooting CSRF Blocks

If a user is blocked by CSRF (HTTP 403 `SEC_002` or `SEC_001` in the response):

1. **Check the Response Headers & Body**:
   - `SEC_002 (CSRF_ORIGIN_INVALID)`: The request `Origin` or `Referer` did not match the Host header or trusted hosts. Verify if the client is going through a proxy and if the proxy forwarded headers (`X-Forwarded-Host`) match the configured `app.security.trusted-hosts`.
   - `SEC_001 (CSRF_TOKEN_INVALID)`: The double-submit `csrf_token` cookie did not match the `X-CSRF-Token` header. Ensure the browser is not blocking cookies (e.g., third-party cookies or SameSite Lax issues) and the frontend is reading the cookie correctly.
2. **Review Environment Settings**:
   - Verify that `CLIENT_URL` is set to the correct client domain.
   - Verify that `TRUSTED_HOSTS` contains the hostnames of all production web clients accessing the backend.

### Step 4: Troubleshooting SSE & Asynchronous Security Exceptions

#### Symptoms:

- Errors logged in `logs/error.log` with stack trace containing:
  `org.springframework.security.authorization.AuthorizationDeniedException: Access Denied`
- Accompanied by Tomcat root exceptions saying:
  `Unable to handle the Spring Security Exception because the response is already committed.`
- Connections to Server-Sent Event (SSE) progress streams or registration streams terminate unexpectedly with protocol level errors.
- Nested security exceptions thrown during SSE stream timeouts or client disconnects.

#### Root Cause:

- Under Spring Security 6, the filter chain intercepts all dispatch types (`REQUEST`, `ASYNC`, `ERROR`).
- During secondary dispatches (`ASYNC` for streaming and `ERROR` for timeouts/disconnects), the JWT filter (`OncePerRequestFilter`) does not execute again. The `SecurityContext` on the async/error handling threads is empty.
- Spring Security's `AuthorizationFilter` intercepts the secondary dispatch, detects no authentication, and throws `AuthorizationDeniedException`.

#### Resolution:

- Verify that `SecurityConfig.java` has the matching rule:
   ```java
   .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
   ```
- This allows internal container dispatches to complete without triggering security filter authorization.
- Note: This does not weaken request security. The initial client entry point is always a `REQUEST` dispatch type, which undergoes full JWT authentication, role check, and CSRF check.
