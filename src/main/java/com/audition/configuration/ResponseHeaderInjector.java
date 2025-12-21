package com.audition.configuration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Filter that injects trace and span IDs into HTTP response headers. This enables correlation of API responses with
 * distributed tracing systems. Generates unique trace and span IDs for each request.
 */
@Component
@Order(1)
public class ResponseHeaderInjector implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ResponseHeaderInjector.class);

    private final boolean enabled;
    private final String traceIdHeaderName;
    private final String spanIdHeaderName;

    /**
     * Constructs a ResponseHeaderInjector with the specified configuration.
     *
     * @param enabled           whether header injection is enabled
     * @param traceIdHeaderName the name of the header for trace ID
     * @param spanIdHeaderName  the name of the header for span ID
     */
    public ResponseHeaderInjector(
        @Value("${audition.tracing.response-headers.enabled:true}") final boolean enabled,
        @Value("${audition.tracing.response-headers.trace-id-header-name:X-Trace-Id}") final String traceIdHeaderName,
        @Value("${audition.tracing.response-headers.span-id-header-name:X-Span-Id}") final String spanIdHeaderName) {
        this.enabled = enabled;
        this.traceIdHeaderName = traceIdHeaderName;
        this.spanIdHeaderName = spanIdHeaderName;
        logger.info("ResponseHeaderInjector initialized as Filter: enabled={}, traceHeader={}, spanHeader={}",
            enabled, traceIdHeaderName, spanIdHeaderName);
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {

        if (!enabled || !(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Generate trace ID and span ID
        final String traceId = generateTraceId();
        final String spanId = generateSpanId();

        // Add to MDC for logging
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);

        try {
            // Set headers BEFORE processing the request
            httpResponse.setHeader(traceIdHeaderName, traceId);
            httpResponse.setHeader(spanIdHeaderName, spanId);

            logger.debug("Set trace headers - {}={}, {}={} for request: {}",
                traceIdHeaderName, traceId, spanIdHeaderName, spanId, httpRequest.getRequestURI());

            // Continue the filter chain
            chain.doFilter(request, response);

        } finally {
            // Clean up MDC
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }

    /**
     * Generates a unique trace ID using UUID format (32 hex characters).
     *
     * @return a unique trace ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generates a unique span ID using UUID format (16 hex characters).
     *
     * @return a unique span ID
     */
    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
