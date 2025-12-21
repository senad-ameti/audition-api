package unit.com.audition.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.configuration.ResponseHeaderInjector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("ResponseHeaderInjector Unit Tests")
class ResponseHeaderInjectorTest {

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/test");
    }

    @Nested
    @DisplayName("When enabled")
    class WhenEnabled {

        @Test
        @DisplayName("generates and injects trace and span IDs into response headers with default header names")
        void injectsTraceAndSpanIds() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                true, "X-Trace-Id", "X-Span-Id");

            // Mock getAttribute to return stored values
            final String[] storedTraceId = new String[1];
            final String[] storedSpanId = new String[1];

            org.mockito.Mockito.doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                String value = invocation.getArgument(1);
                if ("X-TRACE-ID".equals(key)) {
                    storedTraceId[0] = value;
                } else if ("X-SPAN-ID".equals(key)) {
                    storedSpanId[0] = value;
                }
                return null;
            }).when(request).setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());

            when(request.getAttribute("X-TRACE-ID")).thenAnswer(inv -> storedTraceId[0]);
            when(request.getAttribute("X-SPAN-ID")).thenAnswer(inv -> storedSpanId[0]);

            // Simulate request flow: preHandle -> postHandle
            injector.preHandle(request, response, new Object());
            injector.postHandle(request, response, new Object(), null);

            ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);

            verify(response, org.mockito.Mockito.times(2))
                .setHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

            // Verify both headers were set
            final var headerNames = headerNameCaptor.getAllValues();
            final var headerValues = headerValueCaptor.getAllValues();

            assertEquals(2, headerNames.size());
            assertEquals("X-Trace-Id", headerNames.get(0));
            assertEquals("X-Span-Id", headerNames.get(1));

            // Verify trace ID is 32 hex characters
            assertEquals(32, headerValues.get(0).length());
            // Verify span ID is 16 hex characters
            assertEquals(16, headerValues.get(1).length());
        }

        @Test
        @DisplayName("injects trace and span IDs with custom header names")
        void injectsWithCustomHeaderNames() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                true, "Custom-Trace", "Custom-Span");

            final String[] storedTraceId = new String[1];
            final String[] storedSpanId = new String[1];

            org.mockito.Mockito.doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                String value = invocation.getArgument(1);
                if ("X-TRACE-ID".equals(key)) {
                    storedTraceId[0] = value;
                } else if ("X-SPAN-ID".equals(key)) {
                    storedSpanId[0] = value;
                }
                return null;
            }).when(request).setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());

            when(request.getAttribute("X-TRACE-ID")).thenAnswer(inv -> storedTraceId[0]);
            when(request.getAttribute("X-SPAN-ID")).thenAnswer(inv -> storedSpanId[0]);

            injector.preHandle(request, response, new Object());
            injector.postHandle(request, response, new Object(), null);

            ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
            verify(response, org.mockito.Mockito.times(2))
                .setHeader(headerNameCaptor.capture(), org.mockito.ArgumentMatchers.anyString());

            final var headerNames = headerNameCaptor.getAllValues();
            assertEquals("Custom-Trace", headerNames.get(0));
            assertEquals("Custom-Span", headerNames.get(1));
        }

        @Test
        @DisplayName("generates unique trace IDs for different requests")
        void generatesUniqueTraceIds() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                true, "X-Trace-Id", "X-Span-Id");

            final HttpServletRequest request1 = mock(HttpServletRequest.class);
            final HttpServletResponse response1 = mock(HttpServletResponse.class);
            final HttpServletRequest request2 = mock(HttpServletRequest.class);
            final HttpServletResponse response2 = mock(HttpServletResponse.class);

            when(request1.getRequestURI()).thenReturn("/test1");
            when(request2.getRequestURI()).thenReturn("/test2");

            final String[] storedTraceId1 = new String[1];
            final String[] storedSpanId1 = new String[1];
            final String[] storedTraceId2 = new String[1];
            final String[] storedSpanId2 = new String[1];

            // Setup mocks for request1
            org.mockito.Mockito.doAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    String value = invocation.getArgument(1);
                    if ("X-TRACE-ID".equals(key)) {
                        storedTraceId1[0] = value;
                    } else if ("X-SPAN-ID".equals(key)) {
                        storedSpanId1[0] = value;
                    }
                    return null;
                }).when(request1)
                .setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
            when(request1.getAttribute("X-TRACE-ID")).thenAnswer(inv -> storedTraceId1[0]);
            when(request1.getAttribute("X-SPAN-ID")).thenAnswer(inv -> storedSpanId1[0]);

            // Setup mocks for request2
            org.mockito.Mockito.doAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    String value = invocation.getArgument(1);
                    if ("X-TRACE-ID".equals(key)) {
                        storedTraceId2[0] = value;
                    } else if ("X-SPAN-ID".equals(key)) {
                        storedSpanId2[0] = value;
                    }
                    return null;
                }).when(request2)
                .setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
            when(request2.getAttribute("X-TRACE-ID")).thenAnswer(inv -> storedTraceId2[0]);
            when(request2.getAttribute("X-SPAN-ID")).thenAnswer(inv -> storedSpanId2[0]);

            // First request
            injector.preHandle(request1, response1, new Object());
            injector.postHandle(request1, response1, new Object(), null);

            // Second request
            injector.preHandle(request2, response2, new Object());
            injector.postHandle(request2, response2, new Object(), null);

            ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> captor2 = ArgumentCaptor.forClass(String.class);

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
        @DisplayName("stores trace IDs in MDC for logging")
        void storesIdsInMDC() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                true, "X-Trace-Id", "X-Span-Id");

            injector.preHandle(request, response, new Object());

            // MDC values should be set after preHandle
            String traceId = org.slf4j.MDC.get("traceId");
            String spanId = org.slf4j.MDC.get("spanId");

            assertNotNull(traceId);
            assertNotNull(spanId);
            assertEquals(32, traceId.length());
            assertEquals(16, spanId.length());

            // Clean up
            injector.afterCompletion(request, response, new Object(), null);

            // MDC should be cleared after afterCompletion
            assertNull(org.slf4j.MDC.get("traceId"));
            assertNull(org.slf4j.MDC.get("spanId"));
        }
    }

    @Nested
    @DisplayName("When disabled")
    class WhenDisabled {

        @Test
        @DisplayName("does not inject any headers")
        void doesNotInjectHeaders() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                false, "X-Trace-Id", "X-Span-Id");

            injector.preHandle(request, response, new Object());
            injector.postHandle(request, response, new Object(), null);

            verify(response, never()).setHeader(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("does not store values in request attributes when disabled")
        void doesNotStoreAttributesWhenDisabled() {
            final ResponseHeaderInjector injector = new ResponseHeaderInjector(
                false, "X-Trace-Id", "X-Span-Id");

            injector.preHandle(request, response, new Object());

            verify(request, never()).setAttribute(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
        }
    }
}

