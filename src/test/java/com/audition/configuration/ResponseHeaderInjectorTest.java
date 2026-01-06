package com.audition.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.common.logging.AuditionLogger;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class ResponseHeaderInjectorTest {

    private final AuditionLogger auditionLogger = mock(AuditionLogger.class);

    @AfterEach
    void cleanupMdc() {
        MDC.remove("traceId");
        MDC.remove("spanId");
    }

    @Test
    void doFilterInternal_validSpanCtx_successful() throws Exception {
        final Span span = mock(Span.class);
        final SpanContext ctx = mock(SpanContext.class);
        when(ctx.isValid()).thenReturn(true);
        when(ctx.getTraceId()).thenReturn("trace-id-123");
        when(ctx.getSpanId()).thenReturn("span-id-456");
        when(span.getSpanContext()).thenReturn(ctx);

        try (MockedStatic<Span> spanStatic = mockStatic(Span.class)) {
            spanStatic.when(Span::current).thenReturn(span);

            final ResponseHeaderInjector injector = new ResponseHeaderInjector(auditionLogger);
            final MockHttpServletRequest req = new MockHttpServletRequest();
            final MockHttpServletResponse resp = new MockHttpServletResponse();
            final FilterChain chain = mock(FilterChain.class);

            injector.doFilterInternal(req, resp, chain);

            assertThat(resp.getHeader("X-Trace-Id")).isEqualTo("trace-id-123");
            assertThat(resp.getHeader("X-Span-Id")).isEqualTo("span-id-456");
            assertThat(MDC.get("traceId")).isEqualTo("trace-id-123");
            assertThat(MDC.get("spanId")).isEqualTo("span-id-456");

            // verify filter chain proceeded
            verify(chain).doFilter(req, resp);

            // ensure onDestroy clears MDC
            injector.onDestroy();
            assertThat(MDC.get("traceId")).isNull();
            assertThat(MDC.get("spanId")).isNull();
        }
    }

    @Test
    void doFilterInternal_exception_failed() throws Exception {
        try (MockedStatic<Span> spanStatic = mockStatic(Span.class)) {
            final RuntimeException exception = new RuntimeException("otel error");
            spanStatic.when(Span::current).thenThrow(exception);

            final ResponseHeaderInjector injector = new ResponseHeaderInjector(auditionLogger);
            final MockHttpServletRequest req = new MockHttpServletRequest();
            final MockHttpServletResponse resp = new MockHttpServletResponse();
            final FilterChain chain = mock(FilterChain.class);

            injector.doFilterInternal(req, resp, chain);

            // verify logging of the error occurred and the filter chain proceeded
            verify(auditionLogger).logErrorWithException(any(Logger.class),
                eq("Failed to inject OpenTelemetry trace/span IDs into response headers"),
                any(RuntimeException.class));
            verify(chain).doFilter(req, resp);

            // ensure onDestroy clears MDC
            injector.onDestroy();
            assertThat(MDC.get("traceId")).isNull();
            assertThat(MDC.get("spanId")).isNull();
        }
    }
}
