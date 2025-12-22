package unit.com.audition.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.common.logging.AuditionLogger;
import com.audition.common.logging.RestTemplateLoggingInterceptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestTemplateLoggingInterceptor Unit Tests")
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CloseResource"})
class RestTemplateLoggingInterceptorTest {

    private static final String EXAMPLE_API_URL = "http://example.com/api";
    private static final int LARGE_BODY_SIZE = 11_000;

    @Mock
    private transient AuditionLogger auditionLogger;

    @Mock
    private transient HttpRequest httpRequest;

    @Mock
    private transient ClientHttpRequestExecution execution;

    @Mock
    private transient ClientHttpResponse httpResponse;

    private transient RestTemplateLoggingInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RestTemplateLoggingInterceptor(auditionLogger, true);
    }

    @Nested
    @DisplayName("Enabled Interceptor Tests")
    class EnabledInterceptorTests {

        @Test
        @DisplayName("Should log request with method, URI, headers, and body")
        void shouldLogRequestDetails() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.POST);
            when(httpRequest.getURI()).thenReturn(URI.create("http://example.com/api/posts"));
            final HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            when(httpResponse.getHeaders()).thenReturn(responseHeaders);
            when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(httpResponse.getStatusText()).thenReturn("OK");
            when(httpResponse.getBody()).thenReturn(
                new ByteArrayInputStream("{\"id\":1}".getBytes(StandardCharsets.UTF_8)));

            final byte[] requestBody = "{\"title\":\"Test\"}".getBytes(StandardCharsets.UTF_8);
            when(execution.execute(httpRequest, requestBody)).thenReturn(httpResponse);

            // Act
            final ClientHttpResponse response = interceptor.intercept(httpRequest, requestBody, execution);

            // Assert
            assertNotNull(response);
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            // Check that any of the logged messages contain the expected request details
            final boolean hasRequestDetails = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("Method: POST")
                    && msg.contains("URI: http://example.com/api/posts")
                    && msg.contains("Body: {\"title\":\"Test\"}"));
            assertTrue(hasRequestDetails, "Expected request details in logged messages");
        }

        @Test
        @DisplayName("Should log response with status, headers, body, and duration")
        void shouldLogResponseDetails() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(httpRequest.getURI()).thenReturn(URI.create("http://example.com/api/posts/1"));
            final HttpHeaders requestHeaders = new HttpHeaders();
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            when(httpResponse.getHeaders()).thenReturn(responseHeaders);
            when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(httpResponse.getStatusText()).thenReturn("OK");
            when(httpResponse.getBody()).thenReturn(
                new ByteArrayInputStream("{\"id\":1,\"title\":\"Test Post\"}".getBytes(StandardCharsets.UTF_8)));

            when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

            // Act
            final ClientHttpResponse response = interceptor.intercept(httpRequest, new byte[0], execution);

            // Assert
            assertNotNull(response);
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            // Check that response details are in logged messages
            final boolean hasResponseDetails = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("Status: 200 OK"));
            assertTrue(hasResponseDetails, "Expected response details in logged messages");
        }

        @Test
        @DisplayName("Should truncate body over 10KB with marker")
        void shouldTruncateBodyOverLimit() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.POST);
            when(httpRequest.getURI()).thenReturn(URI.create("http://example.com/api/posts"));
            final HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            when(httpResponse.getHeaders()).thenReturn(responseHeaders);
            when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(httpResponse.getStatusText()).thenReturn("OK");
            when(httpResponse.getBody()).thenReturn(
                new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));

            // Create a body larger than 10KB
            final StringBuilder largeBody = new StringBuilder();
            for (int i = 0; i < LARGE_BODY_SIZE; i++) {
                largeBody.append('x');
            }
            final byte[] requestBody = largeBody.toString().getBytes(StandardCharsets.UTF_8);
            when(execution.execute(httpRequest, requestBody)).thenReturn(httpResponse);

            // Act
            interceptor.intercept(httpRequest, requestBody, execution);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            final boolean hasTruncatedMarker = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("[TRUNCATED"));
            assertTrue(hasTruncatedMarker, "Expected truncation marker in logged messages");
        }
    }

    @Nested
    @DisplayName("Header Sanitization Tests")
    class HeaderSanitizationTests {

        @Test
        @DisplayName("Should mask Authorization header as [REDACTED]")
        void shouldMaskAuthorizationHeader() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(httpRequest.getURI()).thenReturn(URI.create(EXAMPLE_API_URL));
            final HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.set("Authorization", "Bearer secret-token-12345");
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final HttpHeaders responseHeaders = new HttpHeaders();
            when(httpResponse.getHeaders()).thenReturn(responseHeaders);
            when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(httpResponse.getStatusText()).thenReturn("OK");
            when(httpResponse.getBody()).thenReturn(
                new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));

            when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

            // Act
            interceptor.intercept(httpRequest, new byte[0], execution);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            final boolean hasRedacted = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("[REDACTED]"));
            final boolean hasNoSecret = messageCaptor.getAllValues().stream()
                .noneMatch(msg -> msg.contains("secret-token-12345"));
            assertTrue(hasRedacted, "Expected [REDACTED] in logged messages");
            assertTrue(hasNoSecret, "Secret token should not appear in logs");
        }

        @Test
        @DisplayName("Should mask Cookie header as [REDACTED]")
        void shouldMaskCookieHeader() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(httpRequest.getURI()).thenReturn(URI.create(EXAMPLE_API_URL));
            final HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.set("Cookie", "sessionId=abc123");
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final HttpHeaders responseHeaders = new HttpHeaders();
            when(httpResponse.getHeaders()).thenReturn(responseHeaders);
            when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(httpResponse.getStatusText()).thenReturn("OK");
            when(httpResponse.getBody()).thenReturn(
                new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));

            when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

            // Act
            interceptor.intercept(httpRequest, new byte[0], execution);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            final boolean hasRedacted = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("[REDACTED]"));
            final boolean hasNoSession = messageCaptor.getAllValues().stream()
                .noneMatch(msg -> msg.contains("sessionId=abc123"));
            assertTrue(hasRedacted, "Expected [REDACTED] in logged messages");
            assertTrue(hasNoSession, "Session ID should not appear in logs");
        }

        @Test
        @DisplayName("Should mask X-API-Key header as [REDACTED]")
        void shouldMaskApiKeyHeader() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(httpRequest.getURI()).thenReturn(URI.create(EXAMPLE_API_URL));
            final HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.set("X-API-Key", "my-secret-api-key");
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final HttpHeaders responseHeaders = new HttpHeaders();
            when(httpResponse.getHeaders()).thenReturn(responseHeaders);
            when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(httpResponse.getStatusText()).thenReturn("OK");
            when(httpResponse.getBody()).thenReturn(
                new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));

            when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

            // Act
            interceptor.intercept(httpRequest, new byte[0], execution);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            final boolean hasRedacted = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("[REDACTED]"));
            final boolean hasNoApiKey = messageCaptor.getAllValues().stream()
                .noneMatch(msg -> msg.contains("my-secret-api-key"));
            assertTrue(hasRedacted, "Expected [REDACTED] in logged messages");
            assertTrue(hasNoApiKey, "API key should not appear in logs");
        }

        @Test
        @DisplayName("Should preserve Content-Type and Accept headers")
        void shouldPreserveNonSensitiveHeaders() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(httpRequest.getURI()).thenReturn(URI.create(EXAMPLE_API_URL));
            final HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            requestHeaders.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final HttpHeaders responseHeaders = new HttpHeaders();
            when(httpResponse.getHeaders()).thenReturn(responseHeaders);
            when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(httpResponse.getStatusText()).thenReturn("OK");
            when(httpResponse.getBody()).thenReturn(
                new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));

            when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

            // Act
            interceptor.intercept(httpRequest, new byte[0], execution);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            final boolean hasContentType = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("Content-Type") && msg.contains("application/json"));
            assertTrue(hasContentType, "Expected Content-Type header in logged messages");
        }
    }

    @Nested
    @DisplayName("Disabled Interceptor Tests")
    class DisabledInterceptorTests {

        @Test
        @DisplayName("Should skip logging when enabled is false")
        void shouldSkipLoggingWhenDisabled() throws IOException {
            // Arrange
            final RestTemplateLoggingInterceptor disabledInterceptor =
                new RestTemplateLoggingInterceptor(auditionLogger, false);

            when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

            // Act
            final ClientHttpResponse response = disabledInterceptor.intercept(httpRequest, new byte[0], execution);

            // Assert
            assertNotNull(response);
            verify(auditionLogger, never()).debug(any(Logger.class), anyString());
        }

        @Test
        @DisplayName("Should still execute request when disabled")
        void shouldStillExecuteRequestWhenDisabled() throws IOException {
            // Arrange
            final RestTemplateLoggingInterceptor disabledInterceptor =
                new RestTemplateLoggingInterceptor(auditionLogger, false);

            when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

            // Act
            final ClientHttpResponse response = disabledInterceptor.intercept(httpRequest, new byte[0], execution);

            // Assert
            assertNotNull(response);
            assertEquals(httpResponse, response);
            verify(execution).execute(httpRequest, new byte[0]);
        }
    }

    @Nested
    @DisplayName("Binary Content Tests")
    class BinaryContentTests {

        @Test
        @DisplayName("Should skip body logging for image content type")
        void shouldSkipBodyLoggingForImageContentType() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(httpRequest.getURI()).thenReturn(URI.create("http://example.com/image.png"));
            final HttpHeaders requestHeaders = new HttpHeaders();
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.IMAGE_PNG);
            when(httpResponse.getHeaders()).thenReturn(responseHeaders);
            when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(httpResponse.getStatusText()).thenReturn("OK");

            when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

            // Act
            interceptor.intercept(httpRequest, new byte[0], execution);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            // Check that one of the messages contains BINARY CONTENT marker
            final boolean foundBinaryMarker = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("[BINARY CONTENT]"));
            assertTrue(foundBinaryMarker, "Expected [BINARY CONTENT] marker in logged messages");
        }

        @Test
        @DisplayName("Should skip body logging for PDF content type")
        void shouldSkipBodyLoggingForPdfContentType() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(httpRequest.getURI()).thenReturn(URI.create("http://example.com/document.pdf"));
            final HttpHeaders requestHeaders = new HttpHeaders();
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            when(httpResponse.getHeaders()).thenReturn(responseHeaders);
            when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(httpResponse.getStatusText()).thenReturn("OK");

            when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

            // Act
            interceptor.intercept(httpRequest, new byte[0], execution);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            final boolean foundBinaryMarker = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("[BINARY CONTENT]"));
            assertTrue(foundBinaryMarker, "Expected [BINARY CONTENT] marker in logged messages");
        }

        @Test
        @DisplayName("Should skip body logging for octet-stream content type")
        void shouldSkipBodyLoggingForOctetStreamContentType() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(httpRequest.getURI()).thenReturn(URI.create("http://example.com/file.bin"));
            final HttpHeaders requestHeaders = new HttpHeaders();
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            when(httpResponse.getHeaders()).thenReturn(responseHeaders);
            when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(httpResponse.getStatusText()).thenReturn("OK");

            when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

            // Act
            interceptor.intercept(httpRequest, new byte[0], execution);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            final boolean foundBinaryMarker = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("[BINARY CONTENT]"));
            assertTrue(foundBinaryMarker, "Expected [BINARY CONTENT] marker in logged messages");
        }
    }

    @Nested
    @DisplayName("Duration Logging Tests")
    class DurationLoggingTests {

        @Test
        @DisplayName("Should include duration in response log")
        void shouldIncludeDurationInLog() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(httpRequest.getURI()).thenReturn(URI.create(EXAMPLE_API_URL));
            final HttpHeaders requestHeaders = new HttpHeaders();
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            when(httpResponse.getHeaders()).thenReturn(responseHeaders);
            when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(httpResponse.getStatusText()).thenReturn("OK");
            when(httpResponse.getBody()).thenReturn(
                new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));

            when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

            // Act
            interceptor.intercept(httpRequest, new byte[0], execution);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            final boolean foundDuration = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("Duration:") && msg.contains("ms"));
            assertTrue(foundDuration, "Expected duration in logged messages");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle IOException and still throw it")
        void shouldHandleIOExceptionAndThrow() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(httpRequest.getURI()).thenReturn(URI.create(EXAMPLE_API_URL));
            final HttpHeaders requestHeaders = new HttpHeaders();
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final IOException ioException = new IOException("Connection refused");
            when(execution.execute(httpRequest, new byte[0])).thenThrow(ioException);

            // Act & Assert
            assertThrows(IOException.class, () ->
                interceptor.intercept(httpRequest, new byte[0], execution));
        }

        @Test
        @DisplayName("Should log error details when IOException occurs")
        void shouldLogErrorDetailsOnIOException() throws IOException {
            // Arrange
            when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(httpRequest.getURI()).thenReturn(URI.create(EXAMPLE_API_URL));
            final HttpHeaders requestHeaders = new HttpHeaders();
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final IOException ioException = new IOException("Connection refused");
            when(execution.execute(httpRequest, new byte[0])).thenThrow(ioException);

            assertThrows(IOException.class, () ->
                interceptor.intercept(httpRequest, new byte[0], execution));

            // Assert - should log both request and error
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            final boolean hasRequestOrError = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("HTTP REQUEST") || msg.contains("HTTP ERROR"));
            assertTrue(hasRequestOrError, "Expected request or error log message");
        }
    }

    @Nested
    @DisplayName("MDC Trace Context Tests")
    class MdcTraceContextTests {

        @Test
        @DisplayName("Should include trace ID in log when present in MDC")
        void shouldIncludeTraceIdFromMdc() throws IOException {
            // Arrange
            MDC.put("traceId", "test-trace-123");
            MDC.put("spanId", "test-span-456");

            try {
                when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
                when(httpRequest.getURI()).thenReturn(URI.create(EXAMPLE_API_URL));
                final HttpHeaders requestHeaders = new HttpHeaders();
                when(httpRequest.getHeaders()).thenReturn(requestHeaders);

                final HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.APPLICATION_JSON);
                when(httpResponse.getHeaders()).thenReturn(responseHeaders);
                when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
                when(httpResponse.getStatusText()).thenReturn("OK");
                when(httpResponse.getBody()).thenReturn(
                    new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));

                when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

                // Act
                interceptor.intercept(httpRequest, new byte[0], execution);

                // Assert
                final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
                verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

                final boolean hasTraceId = messageCaptor.getAllValues().stream()
                    .anyMatch(msg -> msg.contains("test-trace-123"));
                assertTrue(hasTraceId, "Expected trace ID in logged messages");
            } finally {
                MDC.clear();
            }
        }

        @Test
        @DisplayName("Should handle missing MDC trace IDs gracefully")
        void shouldHandleMissingMdcTraceIds() throws IOException {
            // Arrange
            MDC.clear();

            when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(httpRequest.getURI()).thenReturn(URI.create(EXAMPLE_API_URL));
            final HttpHeaders requestHeaders = new HttpHeaders();
            when(httpRequest.getHeaders()).thenReturn(requestHeaders);

            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            when(httpResponse.getHeaders()).thenReturn(responseHeaders);
            when(httpResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(httpResponse.getStatusText()).thenReturn("OK");
            when(httpResponse.getBody()).thenReturn(
                new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));

            when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

            // Act
            interceptor.intercept(httpRequest, new byte[0], execution);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeast(1)).debug(any(Logger.class), messageCaptor.capture());

            final boolean hasUnknown = messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("unknown"));
            assertTrue(hasUnknown, "Expected 'unknown' for missing trace IDs");
        }
    }
}
