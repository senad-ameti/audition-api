package com.audition.configuration;

import static org.assertj.core.api.Assertions.assertThat;
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

        assertThat(result).isTrue();
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("abc123");
        assertThat(response.getHeader("X-Span-Id")).isEqualTo("def456");
    }

    @Test
    void preHandleWithNoActiveSpanSetsNoHeaders() {
        when(tracer.currentSpan()).thenReturn(null);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final boolean result = injector.preHandle(new MockHttpServletRequest(), response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getHeader("X-Trace-Id")).isNull();
        assertThat(response.getHeader("X-Span-Id")).isNull();
    }
}
