package com.audition.configuration;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ResponseHeaderInjector extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {

        // Proceed with the request
        filterChain.doFilter(request, response);

        // Get current span
        final Span currentSpan = Span.current();

        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            response.setHeader("X-Trace-Id", currentSpan.getSpanContext().getTraceId());
            response.setHeader("X-Span-Id", currentSpan.getSpanContext().getSpanId());
        }
    }
}
