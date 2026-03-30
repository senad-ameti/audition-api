# Implementation Decisions & Design Notes

This document outlines the main decisions I made while working on the `audition-api`.

---

## Architecture

### Layered design

I’ve kept the application structure simple and split it into three layers:

- **Controller (`AuditionController`)**  
  Handles HTTP requests/responses. It mainly validates inputs and delegates work to the service.

- **Service (`AuditionService`)**  
  Contains the core business logic (e.g. filtering posts). It orchestrates calls to the integration layer.

- **Integration (`AuditionIntegrationClient`)**  
  Responsible for calling external APIs. Also handles things like error mapping and resilience.

This separation keeps responsibilities clear and makes the code easier to test and maintain.

---

## Key Design Decisions

### Java Records for models

I used Java records for `AuditionPost`, `AuditionComment`, and `AuditionPostFilter`.

They felt like a good fit here since the models are simple data holders. Records give immutability out of the box and reduce boilerplate quite a bit.

---

### Input validation at the boundary

Validation is handled at the controller level using annotations like `@NotBlank`, `@Size`, and `@Valid`.

I also used `@Validated` so method-level validation (like on path variables) works as expected.

This keeps validation close to the API boundary and avoids pushing invalid data further into the system.

---

### Circuit breaker on external calls

All external calls in `AuditionIntegrationClient` are protected with a circuit breaker.

I handled fallbacks slightly differently depending on the use case:

- For list endpoints (`getPosts`, `getComments`), returning an empty list felt like a reasonable fallback so the API can still respond.
- For single-resource endpoints (`getPostById`, `getPostWithComments`), returning empty data would be misleading, so I return a `503 Service Unavailable` instead.

---

### Concurrent fetch for post-with-comments

For `getPostWithComments`, I used `CompletableFuture` to fetch the post and its comments in parallel.

This avoids waiting for one call to finish before starting the other and improves overall response time for that endpoint.

---

### RestTemplate with Apache HttpClient 5 connection pool

`RestTemplate` is configured with `HttpComponentsClientHttpRequestFactory` (Apache HttpClient 5) instead of the default one.

The main reason for this is connection pooling. The default client creates a new connection for every request, which isn’t ideal under load. Using Apache HttpClient allows connections to be reused, which is more efficient and behaves much better in a real-world setup.

I also wrapped it with `BufferingClientHttpRequestFactory` so the logging interceptor can read request/response bodies without consuming the stream.

---

### Error handling in the integration layer

All exceptions from external calls are mapped to a custom `SystemException`.

The mapping is roughly:

- 4xx → pass through status and response body
- 5xx → pass through status and response body
- Network/timeout issues → mapped to `503 Service Unavailable`

This keeps error responses consistent and avoids leaking raw Spring exceptions to the API layer.

---

## API Documentation

I used SpringDoc OpenAPI to document the APIs.

- Swagger UI → `http://localhost:8080/swagger-ui.html`
- OpenAPI spec → `http://localhost:8080/api-docs`

Each endpoint is annotated so the documentation includes request params, responses, and possible error codes.

---

## Observability

### Distributed tracing

Micrometer tracing (with Brave) is used for trace propagation.

Trace and span IDs are:
- included in logs
- added to response headers

This makes it easier to trace requests across services.

---

### Logging

Logging is configured to output structured JSON.

Trace-related fields (like `traceId` and `spanId`) are included in each log entry, which helps when debugging or analysing logs.

---

### Metrics

Prometheus metrics are exposed via:

`/actuator/prometheus`

Circuit breaker health indicators are also enabled, so their state can be monitored.

---

## Build & Code Quality

### Dependency cleanup

I removed a few unnecessary dependencies from the initial setup (like JPA, WebFlux, etc.) since they weren’t needed for this service.

This helps keep the application lightweight and avoids pulling in extra complexity.

---

### Static analysis

Checkstyle, PMD, and SpotBugs are configured as part of the build.

Where needed, I used targeted suppressions with comments to explain why.

---

### Testing

I added tests across different layers:

- Controller tests using `@WebMvcTest`
- Unit tests with Mockito
- Integration tests using WireMock to simulate downstream APIs

---

## Improvements / Next Steps

### Security

I didn’t add Spring Security since it wasn’t part of the requirements.

In a real-world setup, I’d add JWT validation and enforce access control (e.g. using `@PreAuthorize`).

---

### Caching

Currently, every request to `getPosts` hits the external API.

Adding a cache (e.g. Redis with `@Cacheable`) would help reduce load and improve response times.

---