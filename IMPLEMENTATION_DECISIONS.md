# Implementation Decisions & Design Notes

This document explains the key decisions made while completing the audition-api assessment.

---

## Architecture

### Layered design
The application follows a strict three-layer architecture:

- **Controller** (`AuditionController`) — HTTP boundary only. Validates input, delegates to service, returns responses.
- **Service** (`AuditionService`) — Business logic (e.g. title filtering). Orchestrates calls to the integration layer.
- **Integration** (`AuditionIntegrationClient`) — All external HTTP calls. Owns retry/circuit-breaker concerns.

This separation keeps each class focused and makes unit testing straightforward — each layer can be tested in isolation with mocked dependencies.

---

## Key Design Decisions

### Java Records for models
`AuditionPost`, `AuditionComment`, and `AuditionPostFilter` are implemented as Java records. Records provide:
- Immutability by default
- Auto-generated `equals`, `hashCode`, and `toString`
- Concise, readable code

`AuditionPost` uses a compact constructor to defensively copy the `comments` list via `List.copyOf()`, preventing external mutation.

### Input validation at the boundary
Validation annotations (`@NotBlank`, `@Size`, `@Valid`) are applied at the controller and model level. `@Validated` on the controller enables method-level constraint checking (`@NotBlank` on path variables).

### Circuit breaker on all external calls
Every method in `AuditionIntegrationClient` is annotated with `@CircuitBreaker`. Fallback strategies differ by use case:
- **List endpoints** (`getPosts`, `getComments`) — return an empty list on open circuit, allowing partial degradation.
- **Single resource endpoints** (`getPostById`, `getPostWithComments`) — throw a `503 Service Unavailable` `SystemException`, because returning an empty response would be misleading to the caller.

### Concurrent fetch for post-with-comments
`getPostWithComments` uses `CompletableFuture` to fetch the post and its comments concurrently, then combines them. This halves the latency of the combined call compared to sequential fetching.

---

## API Documentation

### OpenAPI / Swagger UI
The API is documented using SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`). Every endpoint is annotated with `@Operation`, `@ApiResponses`, and `@Parameter` to produce a complete, machine-readable specification.

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive Swagger UI (redirects to `/swagger-ui/index.html`) |
| `http://localhost:8080/api-docs` | Raw OpenAPI 3.1 JSON spec |

Both paths are exposed via `management.endpoints.web.exposure.include` and configured in `application.yml`:
```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

The spec documents all four endpoints (`GET /posts`, `GET /posts/{id}`, `GET /posts/{id}/comments`, `GET /comments`), their query parameters, path variables, response schemas, and all possible HTTP status codes including `400`, `404`, `500`, and `503` with `ProblemDetail` response bodies.

---

## Observability

### Distributed tracing
`micrometer-tracing-bridge-brave` bridges Micrometer Tracing to Brave, enabling B3 trace context propagation. The `ResponseHeaderInjector` interceptor injects `X-Trace-Id` and `X-Span-Id` into every HTTP response so clients can correlate requests end-to-end.

The logging pattern in `application.yml` includes `traceId` and `spanId` in every log line:
```
%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]
```

### Structured JSON logging
`logback-spring.xml` includes a `LoggingEventCompositeJsonEncoder` appender that writes structured JSON logs. The JSON fields use Micrometer Tracing's MDC keys (`traceId`, `spanId`) — not the legacy Brave/B3 header names (`X-B3-TraceId`) which are no longer populated in MDC by the bridge library.

### Metrics
`micrometer-registry-prometheus` is on the runtime classpath, exposing `/actuator/prometheus` for scraping. Resilience4j circuit breaker instances register health indicators automatically (`register-health-indicator: true`).

---

## Build & Code Quality

### Dependency cleanup
The following dependencies were removed from the original scaffold:

| Dependency | Reason for removal |
|---|---|
| `spring-boot-starter-data-jpa` | No database in this service |
| `spring-boot-starter-webflux` | Servlet stack only; pulling in Reactor would conflict |
| `spring-boot-starter-aop` | No custom aspects; Resilience4j uses its own proxy |
| `micrometer-tracing` (standalone) | Superseded by `micrometer-tracing-bridge-brave` |
| `micrometer-registry-prometheus` (impl) | Moved to `runtimeOnly` — correct scope for a metrics exporter |
| `io.micrometer:context-propagation` | Bundled inside `micrometer-tracing-bridge-brave` |
| `spring-cloud-starter-circuitbreaker-reactor-resilience4j` | Replaced by `resilience4j-spring-boot3` (no Reactor dependency) |
| Explicit `logback-core` / `logback-classic` | Spring Boot manages these via `spring-boot-starter-logging`; pinning them separately risks version skew |
| `reactor-test` | Only needed for WebFlux reactive stream testing |
| `slf4j-api` (explicit) | Managed transitively by `spring-boot-starter-logging` |

The Spring Cloud BOM is declared as `implementation platform(...)` rather than via the legacy `dependencyManagement` block, which is the Gradle-idiomatic approach for Boot 3+.

### Static analysis
Checkstyle (Google Java style), PMD, and SpotBugs are all configured and run as part of the build. `@SuppressFBWarnings` annotations are used where SpotBugs produces false positives, with justification comments explaining the suppression.

### Test coverage
Unit tests cover all layers. `@WebMvcTest` slice tests are used for the controller (fast, no full application context). `@ExtendWith(MockitoExtension.class)` is used for pure unit tests. WireMock-based integration tests validate the full HTTP stack against a stubbed downstream.

---

## Improvements Needed

### Authentication / Authorisation
No Spring Security configuration was added as the test specification does not mention security requirements. In a production deployment, Spring Security should be configured to validate incoming JWT tokens — verifying token expiry, scope, and integrity — and enforce fine-grained access control using `@PreAuthorize` annotations to validate entitlements at the method level.

### Caching
The `getPosts` endpoint hits the external API on every call. In production, a Redis-backed cache using `@Cacheable` should be introduced to reduce downstream load and improve response times.