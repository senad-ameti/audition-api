# Audition API

The purpose of this Spring Boot application is to test general knowledge of SpringBoot, Java, Gradle etc. It is created for hiring needs of our company but can be used for other purposes.

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
It is  highly recommended that the application be loaded and started up to avoid any issues.

---

## Audition Application information

This section provides information on the application and what the needs to be completed as part of the audition application.

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
- Applicants then need to send the ZIP archive to the email of the recruiting manager. This email be communicated to the applicant during the recruitment process. 

  
---
## Additional Information based on the implementation

# Building:
Check out the code using command:
```
unzip audition-api.zip -d directory-cloned-into
cd directory-cloned-into/audition-api/
```

Navigate to the directory-cloned-into and run command:
```
gradle build
```

If gradle is not installed, please use the following command:
```
./gradlew build    or ./gradlew.bat build (for Windows)
```

This project will be packaged into a runnable jar file.

This will run all the tests and build the application into directory-cloned-into/audition-api/build/libs/audition-api-0.0.1-SNAPSHOT.jar

# Running:
Run command:

```
 java -jar build/libs/audition-api-0.0.1-SNAPSHOT.jar
```

# Apis supported:
Get all the posts:
```
localhost:8080/posts
```

Get posts and filtered by userId
```
localhost:8080/posts?userId=1
```

Get posts by postId
```
localhost:8080/posts/10
```

Get post by id and also all comments of this post
```
localhost:8080/posts/10/comments
```

Get all comments of this post
```
localhost:8080/comments?postId=1
```

# Logging, Tracing and Metrics instrumentation
All the logs contain traceId and spanId.

For dev profile and DEBUG log level, the request body and response body will be logged.

All the response headers contain traceparent and it contains the traceId and spanId.

# Further actions
Add open-api doc for the APIs.

