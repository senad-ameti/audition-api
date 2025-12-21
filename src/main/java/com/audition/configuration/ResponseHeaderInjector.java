package com.audition.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that injects trace and span IDs into HTTP response headers. This enables correlation of API responses
 * with distributed tracing systems. Generates unique trace and span IDs for each request.
 */
@Component
public class ResponseHeaderInjector implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ResponseHeaderInjector.class);
    private static final String TRACE_ID_ATTRIBUTE = "X-TRACE-ID";
    private static final String SPAN_ID_ATTRIBUTE = "X-SPAN-ID";

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
        logger.info("ResponseHeaderInjector initialized: enabled={}, traceHeader={}, spanHeader={}",
            enabled, traceIdHeaderName, spanIdHeaderName);
    }

    /**
     * Generates trace and span IDs before request processing.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @param handler  the handler that will process the request
     * @return true to continue processing
     */
    @Override
    public boolean preHandle(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Object handler) {

        if (!enabled) {
            return true;
        }

        // Generate trace ID and span ID
        final String traceId = generateTraceId();
        final String spanId = generateSpanId();

        // Store in request attributes for use in postHandle
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        request.setAttribute(SPAN_ID_ATTRIBUTE, spanId);

        // Also add to MDC for logging
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);

        logger.debug("Generated trace ID: {} and span ID: {} for request: {}", traceId, spanId,
            request.getRequestURI());

        return true;
    }

    /**
     * Injects trace and span IDs into response headers after request processing.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @param handler  the handler that processed the request
     */
    @Override
    public void postHandle(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Object handler,
        final org.springframework.web.servlet.ModelAndView modelAndView) {

        if (!enabled) {
            return;
        }

        final String traceId = (String) request.getAttribute(TRACE_ID_ATTRIBUTE);
        final String spanId = (String) request.getAttribute(SPAN_ID_ATTRIBUTE);

        if (traceId != null && !traceId.isEmpty()) {
            response.setHeader(traceIdHeaderName, traceId);
            logger.debug("Added trace ID header: {}={}", traceIdHeaderName, traceId);
        }

        if (spanId != null && !spanId.isEmpty()) {
            response.setHeader(spanIdHeaderName, spanId);
            logger.debug("Added span ID header: {}={}", spanIdHeaderName, spanId);
        }
    }

    /**
     * Cleans up MDC after request completion.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @param handler  the handler that processed the request
     * @param ex       any exception thrown during processing
     */
    @Override
    public void afterCompletion(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Object handler,
        final Exception ex) {

        // Clean up MDC
        MDC.remove("traceId");
        MDC.remove("spanId");
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
