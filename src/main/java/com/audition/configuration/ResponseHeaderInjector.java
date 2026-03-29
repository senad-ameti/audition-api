package com.audition.configuration;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ResponseHeaderInjector implements HandlerInterceptor {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";
    private final Tracer tracer;

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response,
        final Object handler) {
        final var currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            response.setHeader(TRACE_ID_HEADER, currentSpan.context().traceId());
            response.setHeader(SPAN_ID_HEADER, currentSpan.context().spanId());
        }
        return true;
    }
}
