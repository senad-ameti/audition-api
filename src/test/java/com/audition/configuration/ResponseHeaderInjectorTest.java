package com.audition.configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

class ResponseHeaderInjectorTest {

    private final Tracer tracer = mock(Tracer.class);
    private final ResponseHeaderInjector responseHeaderInjector = new ResponseHeaderInjector(tracer);

    @Test
    void loggingInterceptorShouldLogRequestAndResponseBody() throws Exception {
        final CurrentTraceContext mockContext = mock(CurrentTraceContext.class);
        final TraceContext mockTraceContext = mock(TraceContext.class);
        final ServletRequest mockRequest = mock(ServletRequest.class);
        final FilterChain mockFilterChain = mock(FilterChain.class);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        when(tracer.currentTraceContext()).thenReturn(mockContext);
        when(mockContext.context()).thenReturn(mockTraceContext);
        when(mockTraceContext.traceId()).thenReturn("traceId");
        when(mockTraceContext.spanId()).thenReturn("spanId");

        responseHeaderInjector.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify that traceparent is set in hearder
        verify(mockResponse).setHeader("traceparent", "00-traceId-spanId-00");
    }
}
