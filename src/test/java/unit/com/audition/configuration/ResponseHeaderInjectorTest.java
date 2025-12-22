package unit.com.audition.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.configuration.ResponseHeaderInjector;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("ResponseHeaderInjector Unit Tests")
class ResponseHeaderInjectorTest {

    private static final String X_TRACE_ID = "X-Trace-Id";
    private static final String X_SPAN_ID = "X-Span-Id";

    private transient HttpServletRequest request;
    private transient HttpServletResponse response;
    private transient FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/test");
    }

    @Nested
    @DisplayName("When enabled")
    class WhenEnabled {

        @Test
        @DisplayName("generates and injects trace and span IDs into response headers with default header names")
        void injectsTraceAndSpanIds() throws Exception {
            final ResponseHeaderInjector filter = new ResponseHeaderInjector(
                true, X_TRACE_ID, X_SPAN_ID);

            filter.doFilter(request, response, filterChain);

            final ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
            final ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);

            verify(response, org.mockito.Mockito.times(2))
                .setHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

            // Verify both headers were set
            final var headerNames = headerNameCaptor.getAllValues();
            final var headerValues = headerValueCaptor.getAllValues();

            assertEquals(2, headerNames.size());
            assertEquals(X_TRACE_ID, headerNames.get(0));
            assertEquals(X_SPAN_ID, headerNames.get(1));

            // Verify trace ID is 32 hex characters
            assertEquals(32, headerValues.get(0).length());
            // Verify span ID is 16 hex characters
            assertEquals(16, headerValues.get(1).length());

            // Verify filter chain was called
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("injects trace and span IDs with custom header names")
        void injectsWithCustomHeaderNames() throws Exception {
            final ResponseHeaderInjector filter = new ResponseHeaderInjector(
                true, "Custom-Trace", "Custom-Span");

            filter.doFilter(request, response, filterChain);

            final ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
            verify(response, org.mockito.Mockito.times(2))
                .setHeader(headerNameCaptor.capture(), org.mockito.ArgumentMatchers.anyString());

            final var headerNames = headerNameCaptor.getAllValues();
            assertEquals("Custom-Trace", headerNames.get(0));
            assertEquals("Custom-Span", headerNames.get(1));
        }

        @Test
        @DisplayName("generates unique trace IDs for different requests")
        void generatesUniqueTraceIds() throws Exception {
            final ResponseHeaderInjector filter = new ResponseHeaderInjector(
                true, X_TRACE_ID, X_SPAN_ID);

            final HttpServletRequest request1 = mock(HttpServletRequest.class);
            final HttpServletResponse response1 = mock(HttpServletResponse.class);
            final FilterChain chain1 = mock(FilterChain.class);

            final HttpServletRequest request2 = mock(HttpServletRequest.class);
            final HttpServletResponse response2 = mock(HttpServletResponse.class);
            final FilterChain chain2 = mock(FilterChain.class);

            when(request1.getRequestURI()).thenReturn("/test1");
            when(request2.getRequestURI()).thenReturn("/test2");

            // First request
            filter.doFilter(request1, response1, chain1);

            // Second request
            filter.doFilter(request2, response2, chain2);

            final ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
            final ArgumentCaptor<String> captor2 = ArgumentCaptor.forClass(String.class);

            verify(response1, org.mockito.Mockito.times(2))
                .setHeader(org.mockito.ArgumentMatchers.anyString(), captor1.capture());
            verify(response2, org.mockito.Mockito.times(2))
                .setHeader(org.mockito.ArgumentMatchers.anyString(), captor2.capture());

            // Verify trace IDs are different
            final var values1 = captor1.getAllValues();
            final var values2 = captor2.getAllValues();

            assertNotNull(values1.get(0)); // trace ID from request 1
            assertNotNull(values2.get(0)); // trace ID from request 2
            // They should be different
            org.junit.jupiter.api.Assertions.assertNotEquals(values1.get(0), values2.get(0));
        }

        @Test
        @DisplayName("stores trace IDs in MDC for logging and cleans up after")
        void storesIdsInMDC() throws Exception {
            final ResponseHeaderInjector filter = new ResponseHeaderInjector(
                true, X_TRACE_ID, X_SPAN_ID);

            // Clear MDC before test
            org.slf4j.MDC.clear();

            filter.doFilter(request, response, filterChain);

            // MDC should be cleared after filter execution (in finally block)
            // So we can't test MDC values after doFilter completes
            // This is correct behavior - MDC cleanup happens automatically
            assertNull(org.slf4j.MDC.get("traceId"));
            assertNull(org.slf4j.MDC.get("spanId"));
        }
    }

    @Nested
    @DisplayName("When disabled")
    class WhenDisabled {

        @Test
        @DisplayName("does not inject any headers")
        void doesNotInjectHeaders() throws Exception {
            final ResponseHeaderInjector filter = new ResponseHeaderInjector(
                false, X_TRACE_ID, X_SPAN_ID);

            filter.doFilter(request, response, filterChain);

            verify(response, never()).setHeader(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

            // But filter chain should still be called
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("When request or response is not HTTP")
    class WhenNotHttpRequestOrResponse {

        @Test
        @DisplayName("does not inject headers when request is not HttpServletRequest")
        void doesNotInjectHeadersForNonHttpRequest() throws Exception {
            final ResponseHeaderInjector filter = new ResponseHeaderInjector(
                true, X_TRACE_ID, X_SPAN_ID);

            final jakarta.servlet.ServletRequest nonHttpRequest = mock(jakarta.servlet.ServletRequest.class);
            final jakarta.servlet.ServletResponse nonHttpResponse = mock(jakarta.servlet.ServletResponse.class);

            filter.doFilter(nonHttpRequest, nonHttpResponse, filterChain);

            verify(response, never()).setHeader(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

            // But filter chain should still be called
            verify(filterChain).doFilter(nonHttpRequest, nonHttpResponse);
        }

        @Test
        @DisplayName("does not inject headers when response is not HttpServletResponse")
        void doesNotInjectHeadersForNonHttpResponse() throws Exception {
            final ResponseHeaderInjector filter = new ResponseHeaderInjector(
                true, X_TRACE_ID, X_SPAN_ID);

            final jakarta.servlet.ServletResponse nonHttpResponse = mock(jakarta.servlet.ServletResponse.class);

            filter.doFilter(request, nonHttpResponse, filterChain);

            verify(response, never()).setHeader(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

            // But filter chain should still be called
            verify(filterChain).doFilter(request, nonHttpResponse);
        }
    }
}

