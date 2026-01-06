package com.audition.configuration;

import com.audition.common.logging.AuditionLogger;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ResponseHeaderInjector extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseHeaderInjector.class);
    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    private final transient AuditionLogger auditionLogger;

    public ResponseHeaderInjector(final AuditionLogger auditionLogger) {
        super();
        this.auditionLogger = auditionLogger;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
        final FilterChain filterChain) throws ServletException, IOException {
        try {
            final Span currentSpan = Span.current();
            final SpanContext ctxSpan = currentSpan.getSpanContext();
            if (ctxSpan != null && ctxSpan.isValid()) {
                response.setHeader("X-Trace-Id", ctxSpan.getTraceId());
                response.setHeader("X-Span-Id", ctxSpan.getSpanId());

                MDC.put(TRACE_ID_KEY, ctxSpan.getTraceId());
                MDC.put(SPAN_ID_KEY, ctxSpan.getSpanId());
            }
        } catch (Exception e) {
            // Log the exception if a OpenTelemetry has error occurs
            auditionLogger.logErrorWithException(LOGGER,
                "Failed to inject OpenTelemetry trace/span IDs into response headers", e);
        }
        filterChain.doFilter(request, response);
    }

    @PreDestroy
    public void onDestroy() {
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
    }
}
