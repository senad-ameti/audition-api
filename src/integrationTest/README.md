# Integration Tests

These tests start the full Spring Boot application on a random port and stub the downstream
API with [WireMock](https://wiremock.org), so no real network calls are made.

## Structure

```
src/integrationTest/
├── java/com/audition/
│   ├── mockservers/
│   │   └── AuditionPostMockServers.java   # WireMock server — started once, stubs all downstream endpoints
│   ├── utils/
│   │   └── FileUtils.java                 
│   └── web/
│       ├── AbstractIntegrationTest.java   
│       ├── PostsIntegrationTest.java      
│       ├── PostByIdIntegrationTest.java   
│       ├── PostWithCommentsIntegrationTest.java  
│       └── CommentsIntegrationTest.java   
└── resources/
    ├── downstream/audition-post/          # Raw fixture data served by WireMock
    │   ├── all-posts.json
    │   └── all-comments.json
    └── response/                          # Expected response bodies asserted in tests
        ├── response-all-posts.json
        ├── response-post-by-userid-1.json
        ├── response-post-by-post-id-1.json
        ├── response-post-by-title-search-vero.json
        ├── response-post-with-comments-by-post-id-1.json
        └── response-comments-for-post-id-1.json
```

## How it works

1. `AbstractIntegrationTest` uses `@DynamicPropertySource` to override `audition_service_url`
   with the WireMock server's random port before the application context starts.
2. `AuditionPostMockServers` is started once and registers stubs for every downstream endpoint used by the application.
3. Each test class extends `AbstractIntegrationTest` and calls the real application endpoints
   via `TestRestTemplate`, then asserts the response body and status code.

The `application-it.yaml` profile is activated via `@ActiveProfiles("it")` on the base class.

## Running the tests

```bash
# Integration tests only
./gradlew integrationTest
```
