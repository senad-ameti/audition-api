package com.audition.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class ResponseHeaderInjectorTest {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    @Mock
    private Tracer tracer;

    @InjectMocks
    private ResponseHeaderInjector injector;

    @Test
    void preHandleWithActiveSpanSetsTraceHeaders() {
        final Span span = mock(Span.class);
        final TraceContext ctx = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(ctx);
        when(ctx.traceId()).thenReturn("abc123");
        when(ctx.spanId()).thenReturn("def456");

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final boolean result = injector.preHandle(new MockHttpServletRequest(), response, new Object());

        assertTrue(result);
        assertEquals("abc123", response.getHeader(TRACE_ID_HEADER));
        assertEquals("def456", response.getHeader(SPAN_ID_HEADER));
    }

    @Test
    void preHandleWithNoActiveSpanSetsNoHeaders() {
        when(tracer.currentSpan()).thenReturn(null);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final boolean result = injector.preHandle(new MockHttpServletRequest(), response, new Object());

        assertTrue(result);
        assertNull(response.getHeader(TRACE_ID_HEADER));
        assertNull(response.getHeader(SPAN_ID_HEADER));
    }
}
