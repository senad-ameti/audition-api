# Purpose

The note aims to showcase what a brief application document should include. It introduces the usage of the application and the design/feature of `audition-api`, as well as some special notices.

# Get Started

## Run in IDE

The application is able to run in an IDE. Simply import the project and run as an ordinary SpringBoot project.

**Note**: *Open Telemetry observability does not work in this mode since it requires a Java agent which can only run with a Jar.*

## Run as a Jar

Run the following command to run the application. Note that `opentelemetry-javaagent.jar` resides in the root directory of `audition-api`. Please replace the `version` yourself. `-Dotel.traces.exporter` can be customised according to the setup. See [more](https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#exporter-selection).

```bash
java -javaagent:path/to/your/opentelemetry-javaagent.jar \
     -Dotel.service.name=audition-api \
     -Dotel.traces.exporter=console \
     -jar audition-api-<version>.jar
```

# Core Components

- `AuditionApplication`: main entrance of the application
- /web: handling the routing logics of the application
- /service: service layer of the application
- /integration: a delegate client of the service layer, including all the business logics, fetching posts and comments from "https://jsonplaceholder.typicode.com/"
- /model: models of posts and comments
- /configuration: configs of the project
  - `WebServiceConfiguration`: a customised `RestTemplate` with a self-defiend `ObjectMapper`
  - `SecurityConfiguration`: set the `SecurityFilterChain` to filter the specified auctuator requests (health, info)
  - `ResponseHeaderInjector`: injecting the trace id and span id to the client and logging for observability
- /common: logging and exception handling of the application

# Features

The application consists of 3 APIs:

1. /posts

   Get all posts from "https://jsonplaceholder.typicode.com/posts". A query param `userId` can be applied to further filter the posts according to the user id. An empty list will be returned if the user id is not found in posts.

2. /posts/{id}

   Get a post by `id` from "https://jsonplaceholder.typicode.com/posts". `404 Not Found` will be returned if the post id is not found.

3. /posts/{id}/comments

   Get all the comments of a post by `id` form "https://jsonplaceholder.typicode.com/posts". An empty list will be returned if the post id is not found in posts.

# Security

The application is secured by a `SecurityFilterChain` exposing only health and info actuator endpoints, while other actuator endpoints are protected by a authenticator which must contain a role named "ACTUATOR". Other business APIs are currently exposed to public, facing with this, some extra protections like Cognito JWT by OAuth2 can be applied to those.

**Note**: *for assessment and test purpose ONLY, a in-memory basic auth user with its credentials was added:*

- *Username: actuator*
- *Password: actuator*

*The user MUST be removed in production enviroment*. 

# Testing

The line coverage by Jacoco is 83%.

# Special Notice

- For assessment and test purpose ONLY, all the `TODO`s and unused gradle denpendencies are kept as comments. They are expected to be removed in production code base since they are mostly redundant.
- All verifications have passed except for some "problems" found by PMD which are believed to be irrelevant and against to the design. The others are false positives.