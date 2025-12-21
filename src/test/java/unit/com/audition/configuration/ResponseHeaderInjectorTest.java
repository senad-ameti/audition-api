package unit.com.audition.configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.configuration.ResponseHeaderInjector;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ResponseHeaderInjector Unit Tests")
class ResponseHeaderInjectorTest {

    private Tracer tracer;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Span span;
    private TraceContext traceContext;

    @BeforeEach
    void setUp() {
        tracer = mock(Tracer.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        span = mock(Span.class);
        traceContext = mock(TraceContext.class);
    }

    @Nested
    @DisplayName("When enabled")
    class WhenEnabled {

        @Test
        @DisplayName("injects trace and span IDs into response headers with default header names")
        void injectsTraceAndSpanIds() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                tracer, true, "X-Trace-Id", "X-Span-Id");

            when(tracer.currentSpan()).thenReturn(span);
            when(span.context()).thenReturn(traceContext);
            when(traceContext.traceId()).thenReturn("test-trace-id-123");
            when(traceContext.spanId()).thenReturn("test-span-id-456");

            injector.postHandle(request, response, new Object(), null);

            verify(response).setHeader("X-Trace-Id", "test-trace-id-123");
            verify(response).setHeader("X-Span-Id", "test-span-id-456");
        }

        @Test
        @DisplayName("injects trace and span IDs with custom header names")
        void injectsWithCustomHeaderNames() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                tracer, true, "Custom-Trace", "Custom-Span");

            when(tracer.currentSpan()).thenReturn(span);
            when(span.context()).thenReturn(traceContext);
            when(traceContext.traceId()).thenReturn("abc123");
            when(traceContext.spanId()).thenReturn("def456");

            injector.postHandle(request, response, new Object(), null);

            verify(response).setHeader("Custom-Trace", "abc123");
            verify(response).setHeader("Custom-Span", "def456");
        }

        @Test
        @DisplayName("does not inject headers when current span is null")
        void doesNotInjectWhenSpanIsNull() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                tracer, true, "X-Trace-Id", "X-Span-Id");

            when(tracer.currentSpan()).thenReturn(null);

            injector.postHandle(request, response, new Object(), null);

            verify(response, never()).setHeader(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("does not inject headers when span context is null")
        void doesNotInjectWhenContextIsNull() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                tracer, true, "X-Trace-Id", "X-Span-Id");

            when(tracer.currentSpan()).thenReturn(span);
            when(span.context()).thenReturn(null);

            injector.postHandle(request, response, new Object(), null);

            verify(response, never()).setHeader(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("does not inject trace ID when it is null")
        void doesNotInjectNullTraceId() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                tracer, true, "X-Trace-Id", "X-Span-Id");

            when(tracer.currentSpan()).thenReturn(span);
            when(span.context()).thenReturn(traceContext);
            when(traceContext.traceId()).thenReturn(null);
            when(traceContext.spanId()).thenReturn("span-456");

            injector.postHandle(request, response, new Object(), null);

            verify(response, never()).setHeader(org.mockito.ArgumentMatchers.eq("X-Trace-Id"),
                org.mockito.ArgumentMatchers.anyString());
            verify(response).setHeader("X-Span-Id", "span-456");
        }

        @Test
        @DisplayName("does not inject trace ID when it is empty")
        void doesNotInjectEmptyTraceId() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                tracer, true, "X-Trace-Id", "X-Span-Id");

            when(tracer.currentSpan()).thenReturn(span);
            when(span.context()).thenReturn(traceContext);
            when(traceContext.traceId()).thenReturn("");
            when(traceContext.spanId()).thenReturn("span-789");

            injector.postHandle(request, response, new Object(), null);

            verify(response, never()).setHeader(org.mockito.ArgumentMatchers.eq("X-Trace-Id"),
                org.mockito.ArgumentMatchers.anyString());
            verify(response).setHeader("X-Span-Id", "span-789");
        }

        @Test
        @DisplayName("does not inject span ID when it is null")
        void doesNotInjectNullSpanId() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                tracer, true, "X-Trace-Id", "X-Span-Id");

            when(tracer.currentSpan()).thenReturn(span);
            when(span.context()).thenReturn(traceContext);
            when(traceContext.traceId()).thenReturn("trace-123");
            when(traceContext.spanId()).thenReturn(null);

            injector.postHandle(request, response, new Object(), null);

            verify(response).setHeader("X-Trace-Id", "trace-123");
            verify(response, never()).setHeader(org.mockito.ArgumentMatchers.eq("X-Span-Id"),
                org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("does not inject span ID when it is empty")
        void doesNotInjectEmptySpanId() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                tracer, true, "X-Trace-Id", "X-Span-Id");

            when(tracer.currentSpan()).thenReturn(span);
            when(span.context()).thenReturn(traceContext);
            when(traceContext.traceId()).thenReturn("trace-456");
            when(traceContext.spanId()).thenReturn("");

            injector.postHandle(request, response, new Object(), null);

            verify(response).setHeader("X-Trace-Id", "trace-456");
            verify(response, never()).setHeader(org.mockito.ArgumentMatchers.eq("X-Span-Id"),
                org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Nested
    @DisplayName("When disabled")
    class WhenDisabled {

        @Test
        @DisplayName("does not inject any headers")
        void doesNotInjectHeaders() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                tracer, false, "X-Trace-Id", "X-Span-Id");

            when(tracer.currentSpan()).thenReturn(span);
            when(span.context()).thenReturn(traceContext);
            when(traceContext.traceId()).thenReturn("trace-id");
            when(traceContext.spanId()).thenReturn("span-id");

            injector.postHandle(request, response, new Object(), null);

            verify(response, never()).setHeader(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("does not call tracer when disabled")
        void doesNotCallTracer() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                tracer, false, "X-Trace-Id", "X-Span-Id");

            injector.postHandle(request, response, new Object(), null);

            verify(tracer, never()).currentSpan();
        }
    }
}

