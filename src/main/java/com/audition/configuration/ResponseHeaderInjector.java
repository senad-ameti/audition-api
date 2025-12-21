package com.audition.configuration;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that injects trace and span IDs into HTTP response headers. This enables correlation of API responses
 * with distributed tracing systems.
 */
@Component
public class ResponseHeaderInjector implements HandlerInterceptor {

    private final Tracer tracer;
    private final boolean enabled;
    private final String traceIdHeaderName;
    private final String spanIdHeaderName;

    /**
     * Constructs a ResponseHeaderInjector with the specified tracer and configuration.
     *
     * @param tracer            the Micrometer tracer for accessing current span context
     * @param enabled           whether header injection is enabled
     * @param traceIdHeaderName the name of the header for trace ID
     * @param spanIdHeaderName  the name of the header for span ID
     */
    public ResponseHeaderInjector(
        final Tracer tracer,
        @Value("${audition.tracing.response-headers.enabled:true}") final boolean enabled,
        @Value("${audition.tracing.response-headers.trace-id-header-name:X-Trace-Id}") final String traceIdHeaderName,
        @Value("${audition.tracing.response-headers.span-id-header-name:X-Span-Id}") final String spanIdHeaderName) {
        this.tracer = tracer;
        this.enabled = enabled;
        this.traceIdHeaderName = traceIdHeaderName;
        this.spanIdHeaderName = spanIdHeaderName;
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

        final var currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return;
        }

        final var spanContext = currentSpan.context();
        if (spanContext == null) {
            return;
        }

        final String traceId = spanContext.traceId();
        if (traceId != null && !traceId.isEmpty()) {
            response.setHeader(traceIdHeaderName, traceId);
        }

        final String spanId = spanContext.spanId();
        if (spanId != null && !spanId.isEmpty()) {
            response.setHeader(spanIdHeaderName, spanId);
        }
    }
}
