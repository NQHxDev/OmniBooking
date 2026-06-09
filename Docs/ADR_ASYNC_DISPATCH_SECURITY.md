# Architectural Decision Record (ADR) - Spring Security ASYNC & ERROR Dispatcher Configurations

## Context

In Spring Boot 3 & Spring Security 6, the security filter chain intercepts all dispatcher types by default. This includes:

- `REQUEST` (standard HTTP requests)
- `ASYNC` (asynchronous request execution lifecycle, e.g., Server-Sent Events, DeferredResult)
- `ERROR` (internal dispatches to error pages/handlers)

Our application features real-time progress streams (such as property media upload progress tracking via `MediaProgressController`) utilizing Server-Sent Events (`SseEmitter`).

Under the default configuration, the initial connection is handled as a standard `REQUEST` dispatch, which passes through the security filters (such as `JwtAuthenticationFilter`) and is successfully authenticated. However, when the controller returns `SseEmitter` and enters async mode:

1. The container performs a secondary `ASYNC` dispatch.
2. The security filter chain runs again.
3. Because our JWT filter inherits from `OncePerRequestFilter`, it does _not_ re-run during the secondary dispatch, leaving the `SecurityContext` on the Tomcat async handler thread empty (anonymous).
4. The Spring Security `AuthorizationFilter` intercepts the `ASYNC` dispatch, finds no authentication, and throws `AuthorizationDeniedException`.
5. Since the response headers have already been committed to stream events, attempting to handle the exception and write a `403 Forbidden` response fails, resulting in a Tomcat container servlet exception: `Unable to handle the Spring Security Exception because the response is already committed.`

Therefore, the root cause of the error is caused by Spring Security authorization processing (`AuthorizationFilter`) during the secondary ASYNC dispatch, while the JWT authentication filter (`OncePerRequestFilter`) is not re-executed for that dispatch.

### Evaluation of ASYNC/ERROR Scenarios

To strengthen operational reliability, we evaluated the necessity of permitting both `DispatcherType.ASYNC` and `DispatcherType.ERROR` under the following production failure modes:

- **SSE Emitter Failures**: When an SSE emitter fails during event transmission, the container must invoke error handling and resource cleanup. If `DispatcherType.ERROR` is not permitted, this cleanup process triggers security authorization on an empty context, leading to secondary authorization exceptions.
- **Async Request Timeouts**: If a stream exceeds the configured timeout value without client activity, Tomcat triggers a timeout. Spring's `WebAsyncManager` initiates a secondary dispatch to process the timeout. Permitting `DispatcherType.ASYNC` and `DispatcherType.ERROR` ensures that these timeout dispatches do not fail with security denials.
- **Client Disconnects during Streaming**: When a client terminates the connection early, the server encounters a socket termination event (e.g., `ClientAbortException`). Tomcat handles this as an error dispatch. If blocked by security filters, the container throws nested `AuthorizationDeniedException` instances, corrupting logs and masking clean connection closure.
- **Exceptions occurring after the response has already been committed**: If an application exception occurs in an async thread after the response headers are written, the container routes the request to `/error`. By permitting `DispatcherType.ERROR` globally, the container can complete the cycle without attempting to rewrite response headers for a secondary security exception (which causes `IllegalStateException` or `response already committed` errors).
- **Secondary servlet error dispatches triggered by Tomcat/Spring MVC**: Internal dispatches to the error page (like `/error`) must bypass security checks so that the global error handler (`GlobalExceptionHandler`) can correctly serialize formatted `ApiResponse.error()` payloads to the client rather than having the request blocked and returning an empty page.

---

## Options Evaluated

### Option A: Manually Propagate SecurityContext to Async Handlers

We could configure a custom task executor or explicitly configure the `WebAsyncManager` to use `DelegatingSecurityContextAsyncTaskExecutor` to copy the context to async worker threads.

- **Advantages**: Retains granular security context checks on async worker threads.
- **Disadvantages**: Does _not_ prevent the secondary dispatch problem at the servlet container level. The container's `ASYNC` and `ERROR` dispatches still pass through the filter chain where `OncePerRequestFilter` has already run and cleared the context on the handler thread.

### Option B: Permit ASYNC and ERROR Dispatcher Types in Security Filter Chain (Selected)

We configure the `SecurityFilterChain` to explicitly permit all dispatches of type `DispatcherType.ASYNC` and `DispatcherType.ERROR` without running authorization rules.

- **Advantages**:
   - **Clean Resolution**: Prevents `AuthorizationDeniedException` during async finalization and timeouts.
   - **Robust Error Handling**: Prevents nested security errors when client disconnects or timeouts occur on SSE streams.
   - **Safe**: Bypassing security on secondary dispatches is safe because the initial `REQUEST` entry point has already been fully validated, authenticated, and authorized. A client cannot directly trigger a raw `ASYNC` or `ERROR` servlet dispatch from the outside.
- **Disadvantages**: None identified.

---

## Design and Configuration

### Security Filter Chain Configuration

We update [SecurityConfig.java](file:///Users/anhjkr/Documents/My%20Projects/OmniBooking/Server/src/main/java/com/omnibooking/security/SecurityConfig.java) to include:

```java
.authorizeHttpRequests(auth -> auth
      .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
      // other rules...
)
```

### Security Considerations

- **No Bypass for Initial Entries**: All incoming client requests enter as `REQUEST` dispatches and are subject to full JWT validation, CSRF checks, and role/permission checks.
- **Scope Isolation**: Bypassing `ASYNC` and `ERROR` dispatches only allows internal container routing to proceed without re-triggering security filters on the same request context.

---

## Verification and Testing

### Regression Protection

We introduce [SseSecurityIntegrationTest.java](file:///Users/anhjkr/Documents/My%20Projects/OmniBooking/Server/src/test/java/com/omnibooking/security/SseSecurityIntegrationTest.java) to verify:

1. Authenticated partners can initiate and complete SSE streams.
2. Unauthenticated clients are rejected with `401/403` on standard entry.
3. `DispatcherType.ERROR` dispatches bypass security checks.
4. Simulated async timeouts do not trigger authorization denials.
