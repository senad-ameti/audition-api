# Audition API

The purpose of this Spring Boot application is to test general knowledge of SpringBoot, Java, Gradle etc. It is created
for hiring needs of our company but can be used for other purposes.

## Overarching expectations & Assessment areas

<pre>
This is not a university test. 
This is meant to be used for job applications and MUST showcase your full skillset. 
<b>As such, PRODUCTION-READY code must be written and submitted. </b> 
</pre>

- clean, easy to understand code
- good code structures
- Proper code encapsulation
- unit tests with minimum 80% coverage.
- A Working application to be submitted.
- Observability. Does the application contain Logging, Tracing and Metrics instrumentation?
- Input validation.
- Proper error handling.
- Ability to use and configure rest template. We allow for half-setup object mapper and rest template
- Not all information in the Application is perfect. It is expected that a person would figure these out and correct.

## Getting Started

### Prerequisite tooling

- Any Springboot/Java IDE. Ideally IntelliJIdea.
- Java 17
- Gradle 8

### Prerequisite knowledge

- Java
- SpringBoot
- Gradle
- Junit

### Importing Google Java codestyle into INtelliJ

```
- Go to IntelliJ Settings
- Search for "Code Style"
- Click on the "Settings" icon next to the Scheme dropdown
- Choose "Import -> IntelliJ Idea code style XML
- Pick the file "google_java_code_style.xml" from root directory of the application
__Optional__
- Search for "Actions on Save"
    - Check "Reformat Code" and "Organise Imports"
```

---
**NOTE** -
It is highly recommended that the application be loaded and started up to avoid any issues.

---

## Audition Application information

This section provides information on the application and what the needs to be completed as part of the audition
application.

The audition consists of multiple TODO statements scattered throughout the codebase. The applicants are expected to:

- Complete all the TODO statements.
- Add unit tests where applicants believe it to be necessary.
- Make sure that all code quality check are completed.
- Gradle build completes sucessfully.
- Make sure the application if functional.

## Submission process

Applicants need to do the following to submit their work:

- Clone this repository
- Complete their work and zip up the working application.
- Applicants then need to send the ZIP archive to the email of the recruiting manager. This email be communicated to the
  applicant during the recruitment process.

  
---

## Additional Information based on the implementation

This section MUST be completed by applicants. It allows applicants to showcase their view on how an application
can/should be documented.
Applicants can choose to do this in a separate markdown file that needs to be included when the code is committed.

---

## Implementation Documentation

### Architecture Overview

The Audition API is a Spring Boot application that provides a RESTful interface to retrieve posts and comments from
the [JSONPlaceholder](https://jsonplaceholder.typicode.com/) external API. The application follows a layered
architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                      Controller Layer                        │
│                   (AuditionController)                       │
├─────────────────────────────────────────────────────────────┤
│                       Service Layer                          │
│                    (AuditionService)                         │
├─────────────────────────────────────────────────────────────┤
│                    Integration Layer                         │
│               (AuditionIntegrationClient)                    │
├─────────────────────────────────────────────────────────────┤
│                     External API                             │
│                (JSONPlaceholder API)                         │
└─────────────────────────────────────────────────────────────┘
```

### API Endpoints

| Method | Endpoint                           | Description                           | Parameters                           |
|--------|------------------------------------|---------------------------------------|--------------------------------------|
| GET    | `/posts`                           | Retrieve all posts                    | `userId` (optional), `id` (optional) |
| GET    | `/posts/{id}`                      | Retrieve a specific post by ID        | `id` (path)                          |
| GET    | `/posts/{id}?includeComments=true` | Retrieve a post with its comments     | `id` (path)                          |
| GET    | `/posts/{id}/comments`             | Retrieve comments for a specific post | `id` (path)                          |

### Running the Application

```bash
# Build the application
./gradlew clean build

# Run the application
./gradlew bootRun

# Run with dev profile (enables request/response logging)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The application runs on port 8080 by default.

### Configuration

Key configuration properties in `application.yml`:

| Property                                    | Description                | Default             |
|---------------------------------------------|----------------------------|---------------------|
| `audition.integration.posts-url`            | URL for fetching all posts | JSONPlaceholder API |
| `audition.cache.enabled`                    | Enable/disable caching     | `true`              |
| `audition.cache.ttl.minutes`                | Cache TTL in minutes       | `15`                |
| `audition.tracing.response-headers.enabled` | Enable trace headers       | `true`              |
| `audition.logging.rest-template.enabled`    | Enable HTTP logging        | `false`             |

### Observability

#### Logging

- **Structured Logging**: Uses Logback with configurable patterns including trace/span IDs
- **Request/Response Logging**: `RestTemplateLoggingInterceptor` provides detailed HTTP logging at DEBUG level
- **Sensitive Data Redaction**: Authorization headers and API keys are automatically redacted in logs

#### Tracing

- **Trace ID/Span ID**: Generated for each request via `ResponseHeaderInjector`
- **Response Headers**: `X-Trace-Id` and `X-Span-Id` headers added to all responses
- **MDC Integration**: Trace context available in all log messages

#### Metrics

- **Actuator Endpoints**: Exposed at `/actuator/*`
- **Prometheus Integration**: Metrics available via Micrometer registry
- **Health Checks**: Available at `/actuator/health`

### Caching

The application uses Caffeine as the caching provider with the following caches:

| Cache Name         | Purpose                      |
|--------------------|------------------------------|
| `posts`            | All posts from the API       |
| `postsById`        | Individual posts by ID       |
| `comments`         | Comments for each post       |
| `postWithComments` | Posts with embedded comments |

### Security

- **Public Endpoints**: `/posts/**`, `/actuator/health`, `/actuator/info`
- **Secured Endpoints**: All other `/actuator/**` endpoints require API key
- **Authentication**: API key authentication via `X-API-Key` header
- **Configuration**: Set `ACTUATOR_API_KEY` environment variable

### Input Validation

- Custom `@PositiveInteger` annotation for path and query parameters
- Validates that IDs are positive integers
- Returns standardized error responses via `ConstraintViolationException` handler

### Error Handling

Centralized exception handling via `ExceptionControllerAdvice`:

| Exception Type                 | HTTP Status | Response Format          |
|--------------------------------|-------------|--------------------------|
| `SystemException`              | Varies      | ProblemDetail (RFC 7807) |
| `ConstraintViolationException` | 400         | ProblemDetail            |
| `HttpClientErrorException`     | Varies      | ProblemDetail            |
| Other exceptions               | 500         | ProblemDetail            |

### Testing

```bash
# Run all tests
./gradlew test

# Run with coverage report
./gradlew test jacocoTestReport

# Verify coverage meets 80% threshold
./gradlew jacocoTestCoverageVerification
```

**Test Structure:**

- `unit/` - Unit tests with mocked dependencies
- `component/` - Integration tests with WireMock for external API simulation

### Code Quality

```bash
# Run all checks
./gradlew check

# Individual checks
./gradlew checkstyleMain    # Code style
./gradlew pmdMain           # Static analysis
./gradlew spotbugsMain      # Bug detection
```

### Dependencies

Key dependencies used:

- **Spring Boot 3.x** - Core framework
- **Spring Security** - API key authentication
- **Caffeine** - High-performance caching
- **Micrometer + Prometheus** - Metrics
- **WireMock** - API mocking for tests
- **Lombok** - Boilerplate reduction 

