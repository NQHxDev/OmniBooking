# Architectural Decision Record (ADR) - Monorepo Authentication Cookie Ownership

## Context

Following the migration to a Client Monorepo architecture containing three separate Next.js applications:

- **`apps/web`** (Guest Booking Portal) running on port `3000`
- **`apps/partner`** (Hotel Partner Portal) running on port `3002`
- **`apps/owner`** (System Admin Portal) running on port `3005`

These applications communicate with the Spring Boot Backend API (**`Server`**) running on port `8080` (context path `/api/v1`) using Next.js `rewrites` in `next.config.ts` to proxy requests (e.g., `/api/v1/:path*` -> `http://127.0.0.1:8080/api/v1/:path*`).

Authentication uses HTTP cookie-based session state (`access_token`, `session_id`, `refresh_token`, `x_fgp`, and `csrf_token`). We need a formal architectural decision to define who owns the lifecycle (issuance, validation, rotation, and revocation) of these cookies.

---

## Options Evaluated

### Option A: Frontend Proxy (Next.js) Owns & Manages Session Cookies

In this model, the Next.js apps act as a Backend-For-Frontend (BFF). The Next.js server intercepts authentication requests, calls the backend, reads the credentials, manages session cookies internally, and attaches Bearer JWT headers on subsequent requests to the backend.

- **Advantages**:
   - Cookies can be scoped strictly to individual subdomains (e.g. `partner.zeion.online`) without needing wildcard cookies.
   - The backend remains entirely stateless (only parsing `Authorization` header).
- **Disadvantages**:
   - Duplicate code: The refresh logic, cookie parameters, and security attributes must be duplicated across three separate frontend apps.
   - High proxy overhead: Next.js middleware must execute serializing/deserializing logic on every request, adding CPU overhead.
   - Hard to synchronize logouts: Logging out of `apps/partner` does not easily invalidate sessions on `apps/web`.

### Option B: Backend API (Spring Boot) Owns & Manages Session Cookies (Selected)

In this model, the backend Spring Boot Server is the single source of truth for cookie management. It issues cookies (`access_token`, `session_id`, `refresh_token`, `x_fgp`, `csrf_token`) directly to the browser. The frontend applications simply proxy the network requests, allowing the browser to attach the cookies automatically.

- **Advantages**:
   - **Single Source of Truth**: Cookie generation, SameSite/Secure flag handling, and sliding window expiration are implemented once in `CookieUtils` (Spring Boot).
   - **Zero Duplication**: Next.js apps require zero authentication lifecycle code, utilizing simple rewrites.
   - **Instant Global Logout**: Session deletion in Redis instantly invalidates the session across all subdomains.
   - **Zero Next.js Middleware Overhead**: Cookies flow directly to the backend without Next.js intercepting and parsing tokens.
- **Disadvantages**:
   - **Requires Configurable Wildcard Scoping**: In production, the cookie domain must be set to `.yourdomain.com` (via `COOKIE_DOMAIN`) so cookies are shared across `web.yourdomain.com`, `partner.yourdomain.com`, and `owner.yourdomain.com`.

---

## Detailed Design for Option B in OmniBooking

### 1. Local Development Behavior (Ports 3000, 3002, 3005)

By default, RFC 6265 specifies that cookie isolation ignores ports. When `COOKIE_DOMAIN` is empty (default on local), the cookie's domain attribute is omitted.

- Cookies default to the request host (`localhost`).
- The browser automatically shares cookies across all local dev applications running on ports `3000`, `3002`, and `3005`.

### 2. Production Profile Behavior (Wildcard Domains)

When running in production profile (`prod` or `production`), the startup safeguard requires `COOKIE_DOMAIN` to be explicitly declared (e.g., `COOKIE_DOMAIN=.zeion.online` or `.yourdomain.com`).

- This scopes the cookie to the parent domain, allowing cross-subdomain authentication.
- The Next.js apps proxy requests using standard Next.js rewrites, making API requests appear as same-site to the browser, avoiding third-party cookie blocking.

### 3. Session Security Mitigations

Since cookies are shared across subdomains, we enforce three lines of defense:

1. **Fingerprinting (`x_fgp` Cookie)**: Binds the access token to a cryptographically random fingerprint. Even if an XSS vulnerability on one subdomain leaks a JWT token, it cannot be re-used from another browser unless the attacker also steals the HttpOnly `x_fgp` cookie.
2. **Double-Submit CSRF**: `CustomCsrfFilter` enforces strict CSRF verification matching the `csrf_token` cookie against the `X-CSRF-Token` header.
3. **Fail-Closed Session Checks**: The backend verifies token revocations via Redis on every request. Redis outages immediately return HTTP 503 instead of falling back to insecure defaults.

---

## Migration and Rollback Plan

### Migration Path

1. **Remove Next.js Session Handlers**: Delete any custom cookie handling, parsing, or manually managed state logic from the Next.js middlewares.
2. **Configure Domain Properties**: Define the `app.security.cookie-domain` property in Spring Boot to map to `COOKIE_DOMAIN`.
3. **Confirm allowedDevOrigins**: Ensure all next.config.ts files include the shared hostname in `allowedDevOrigins` to prevent dev server proxy blocking.

### Rollback Strategy

If third-party cookie restrictions or future browser updates block cross-subdomain wildcard cookies:

1. **Switch to BFF Routing**: Reintroduce Next.js middleware token routing. Next.js will parse client-scoped cookies and convert them to header-based authorization tokens when forwarding to `Server` on port `8080`.
2. **Remove `COOKIE_DOMAIN`**: Set `COOKIE_DOMAIN` to empty, causing cookies to bind strictly to individual subdomains.
