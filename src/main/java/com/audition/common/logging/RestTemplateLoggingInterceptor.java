package com.audition.common.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

/**
 * A logging interceptor for RestTemplate that logs HTTP request and response details at DEBUG level. This interceptor
 * captures method, URI, headers (with sensitive data sanitized), body content (for text types), request duration, and
 * trace/span IDs for correlation.
 */
public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateLoggingInterceptor.class);

    private static final int MAX_BODY_SIZE = 10_240; // 10KB
    private static final String REDACTED = "[REDACTED]";
    private static final String TRUNCATED_MARKER = "... [TRUNCATED - showing first 10240 bytes]";
    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    private static final String UNKNOWN = "unknown";

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
        "authorization",
        "cookie",
        "set-cookie",
        "x-api-key"
    );

    private static final Set<String> BINARY_CONTENT_TYPE_PREFIXES = Set.of(
        "image/",
        "audio/",
        "video/"
    );

    private static final Set<String> BINARY_CONTENT_TYPES = Set.of(
        "application/pdf",
        "application/octet-stream",
        "application/zip",
        "application/gzip"
    );

    private final transient AuditionLogger logger;
    private final transient boolean enabled;

    /**
     * Constructs a RestTemplateLoggingInterceptor with the specified logger and enabled flag.
     *
     * @param logger  the AuditionLogger for logging messages
     * @param enabled whether logging is enabled
     */
    public RestTemplateLoggingInterceptor(final AuditionLogger logger, final boolean enabled) {
        this.logger = logger;
        this.enabled = enabled;
    }

    @Override
    @NonNull
    public ClientHttpResponse intercept(@NonNull final HttpRequest request, @NonNull final byte[] body,
        @NonNull final ClientHttpRequestExecution execution) throws IOException {

        if (!enabled || !LOG.isDebugEnabled()) {
            return execution.execute(request, body);
        }

        logRequest(request, body);

        final long startTime = System.currentTimeMillis();
        try {
            final ClientHttpResponse response = execution.execute(request, body);
            final long duration = System.currentTimeMillis() - startTime;
            logResponse(request, response, duration);
            return response;
        } catch (final IOException e) {
            final long duration = System.currentTimeMillis() - startTime;
            logError(request, e, duration);
            throw e;
        }
    }

    /**
     * Logs the HTTP request details including method, URI, headers, and body.
     *
     * @param request the HTTP request
     * @param body    the request body as bytes
     */
    private void logRequest(final HttpRequest request, final byte[] body) {
        if (!LOG.isDebugEnabled()) {
            return;
        }
        try {
            final StringBuilder message = new StringBuilder(512);
            message.append("\n===== HTTP REQUEST =====\nTrace ID: ").append(getTraceId())
                .append("\nSpan ID: ").append(getSpanId())
                .append("\nMethod: ").append(request.getMethod())
                .append("\nURI: ").append(request.getURI())
                .append("\nHeaders: ").append(sanitizeHeaders(request.getHeaders())).append('\n');

            if (body != null && body.length > 0 && isTextContent(request.getHeaders())) {
                final String bodyContent = truncateBody(new String(body, StandardCharsets.UTF_8));
                message.append("Body: ").append(bodyContent).append('\n');
            }

            message.append("========================");
            logger.debug(LOG, message.toString());
        } catch (final RuntimeException e) {
            if (LOG.isDebugEnabled()) {
                logger.debug(LOG, "Error logging request: " + e.getMessage());
            }
        }
    }

    /**
     * Logs the HTTP response details including status, headers, body, and duration.
     *
     * @param request  the original HTTP request
     * @param response the HTTP response
     * @param duration the request duration in milliseconds
     */
    private void logResponse(final HttpRequest request, final ClientHttpResponse response, final long duration) {
        if (!LOG.isDebugEnabled()) {
            return;
        }
        try {
            final StringBuilder message = new StringBuilder(512);
            message.append("\n===== HTTP RESPONSE =====\nTrace ID: ").append(getTraceId())
                .append("\nSpan ID: ").append(getSpanId())
                .append("\nURI: ").append(request.getURI())
                .append("\nStatus: ").append(response.getStatusCode().value()).append(' ')
                .append(response.getStatusText())
                .append("\nDuration: ").append(duration).append("ms\nHeaders: ")
                .append(sanitizeHeaders(response.getHeaders())).append('\n');

            if (isTextContent(response.getHeaders())) {
                final String bodyContent = readResponseBody(response);
                if (!bodyContent.isEmpty()) {
                    message.append("Body: ").append(truncateBody(bodyContent)).append('\n');
                }
            } else {
                message.append("Body: [BINARY CONTENT]\n");
            }

            message.append("=========================");
            logger.debug(LOG, message.toString());
        } catch (final RuntimeException | IOException e) {
            if (LOG.isDebugEnabled()) {
                logger.debug(LOG, "Error logging response: " + e.getMessage());
            }
        }
    }

    /**
     * Logs an error that occurred during the HTTP request.
     *
     * @param request  the HTTP request
     * @param e        the exception that occurred
     * @param duration the request duration in milliseconds
     */
    private void logError(final HttpRequest request, final IOException e, final long duration) {
        if (!LOG.isDebugEnabled()) {
            return;
        }
        try {
            final String message = "\n===== HTTP ERROR =====\n"
                + "Trace ID: " + getTraceId() + "\n"
                + "Span ID: " + getSpanId() + "\n"
                + "URI: " + request.getURI() + "\n"
                + "Duration: " + duration + "ms\n"
                + "Error: " + e.getMessage() + "\n"
                + "======================";
            logger.debug(LOG, message);
        } catch (final RuntimeException ex) {
            if (LOG.isDebugEnabled()) {
                logger.debug(LOG, "Error logging error: " + ex.getMessage());
            }
        }
    }

    /**
     * Sanitizes HTTP headers by redacting sensitive values.
     *
     * @param headers the HTTP headers to sanitize
     * @return a string representation of the sanitized headers
     */
    private String sanitizeHeaders(final HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return "{}";
        }

        return headers.entrySet().stream()
            .map(entry -> {
                final String key = entry.getKey();
                final List<String> values = entry.getValue();
                if (isSensitiveHeader(key)) {
                    return key + "=" + REDACTED;
                }
                return key + "=" + values;
            })
            .collect(Collectors.joining(", ", "{", "}"));
    }

    /**
     * Checks if a header name is considered sensitive and should be redacted.
     *
     * @param headerName the header name to check
     * @return true if the header is sensitive, false otherwise
     */
    private boolean isSensitiveHeader(final String headerName) {
        return SENSITIVE_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    /**
     * Checks if the content type indicates text content that should be logged.
     *
     * @param headers the HTTP headers containing the content type
     * @return true if the content is text-based, false otherwise
     */
    private boolean isTextContent(final HttpHeaders headers) {
        final MediaType contentType = headers.getContentType();
        if (contentType == null) {
            return true; // Assume text if no content type is specified
        }

        final String typeString = contentType.toString().toLowerCase(Locale.ROOT);

        // Check for binary prefixes
        if (BINARY_CONTENT_TYPE_PREFIXES.stream().anyMatch(typeString::startsWith)) {
            return false;
        }

        // Check for exact binary types
        return BINARY_CONTENT_TYPES.stream().noneMatch(typeString::startsWith);
    }

    /**
     * Reads the response body from the ClientHttpResponse.
     *
     * @param response the HTTP response
     * @return the response body as a string
     * @throws IOException if an error occurs reading the response
     */
    private String readResponseBody(final ClientHttpResponse response) throws IOException {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Truncates the body content if it exceeds the maximum size.
     *
     * @param body the body content to truncate
     * @return the truncated body content
     */
    private String truncateBody(final String body) {
        if (body == null) {
            return "";
        }
        if (body.length() <= MAX_BODY_SIZE) {
            return body;
        }
        return body.substring(0, MAX_BODY_SIZE) + TRUNCATED_MARKER;
    }

    /**
     * Gets the trace ID from MDC, or returns "unknown" if not present.
     *
     * @return the trace ID or "unknown"
     */
    private String getTraceId() {
        final String traceId = MDC.get(TRACE_ID_KEY);
        return traceId != null ? traceId : UNKNOWN;
    }

    /**
     * Gets the span ID from MDC, or returns "unknown" if not present.
     *
     * @return the span ID or "unknown"
     */
    private String getSpanId() {
        final String spanId = MDC.get(SPAN_ID_KEY);
        return spanId != null ? spanId : UNKNOWN;
    }
}

