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
    void preHandle_withActiveSpan_setsTraceHeaders() {
        Span span = mock(Span.class);
        TraceContext ctx = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(ctx);
        when(ctx.traceId()).thenReturn("abc123");
        when(ctx.spanId()).thenReturn("def456");

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean result = injector.preHandle(new MockHttpServletRequest(), response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("abc123");
        assertThat(response.getHeader("X-Span-Id")).isEqualTo("def456");
    }

    @Test
    void preHandle_withNoActiveSpan_setsNoHeaders() {
        when(tracer.currentSpan()).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean result = injector.preHandle(new MockHttpServletRequest(), response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getHeader("X-Trace-Id")).isNull();
        assertThat(response.getHeader("X-Span-Id")).isNull();
    }
}
