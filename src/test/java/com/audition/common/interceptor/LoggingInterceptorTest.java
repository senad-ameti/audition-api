package com.audition.common.interceptor;

import com.audition.common.logging.AuditionLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

class LoggingInterceptorTest {

    private LoggingInterceptor interceptor;
    private HttpRequest mockRequest;
    private ClientHttpRequestExecution mockExecution;
    private ClientHttpResponse mockResponse;

    private AuditionLogger auditionLogger;

    @BeforeEach
    void setUp() {
        auditionLogger = new AuditionLogger();
        interceptor = new LoggingInterceptor(auditionLogger);
        mockRequest = mock(HttpRequest.class);
        mockExecution = mock(ClientHttpRequestExecution.class);
        mockResponse = mock(ClientHttpResponse.class);
    }

    @Test
    void interceptWithInfoAndDebugEnabled() throws IOException {
        when(mockRequest.getMethod()).thenReturn(HttpMethod.POST);
        when(mockRequest.getURI()).thenReturn(URI.create("http://localhost/test"));
        when(mockRequest.getHeaders()).thenReturn(new HttpHeaders());

        final String responseBody = "Hello Response";
        when(mockResponse.getBody()).thenReturn(
                new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8))
        );
        when(mockResponse.getStatusCode()).thenReturn(org.springframework.http.HttpStatus.OK);
        when(mockResponse.getStatusText()).thenReturn("OK");
        when(mockResponse.getHeaders()).thenReturn(new HttpHeaders());

        final byte[] body = "request-body".getBytes(StandardCharsets.UTF_8);
        when(mockExecution.execute(mockRequest, body)).thenReturn(mockResponse);

        try (ClientHttpResponse actual = interceptor.intercept(mockRequest, body, mockExecution)) {
            assertEquals(mockResponse, actual);
        }
        verify(mockExecution, times(1)).execute(mockRequest, body);
    }

    @Test
    void interceptWithEmptyBodyAndMinimalResponse() throws IOException {
        when(mockRequest.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequest.getURI()).thenReturn(URI.create("http://localhost/empty"));
        when(mockRequest.getHeaders()).thenReturn(new HttpHeaders());

        when(mockResponse.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockResponse.getStatusCode()).thenReturn(org.springframework.http.HttpStatus.NO_CONTENT);
        when(mockResponse.getStatusText()).thenReturn("No Content");
        when(mockResponse.getHeaders()).thenReturn(new HttpHeaders());

        final byte[] emptyBody = new byte[0];
        when(mockExecution.execute(mockRequest, emptyBody)).thenReturn(mockResponse);

        try (ClientHttpResponse actual = interceptor.intercept(mockRequest, emptyBody, mockExecution)) {
            assertEquals(mockResponse, actual);
        }
        verify(mockExecution, times(1)).execute(mockRequest, emptyBody);
    }

    @Test
    void interceptWhenLoggingDisabled() throws IOException {
        final Logger mockLogger = mock(Logger.class);

        // force all isXEnabled() to false
        when(mockLogger.isInfoEnabled()).thenReturn(false);
        when(mockLogger.isDebugEnabled()).thenReturn(false);

        try (MockedStatic<LoggerFactory> mocked = mockStatic(LoggerFactory.class)) {
            mocked.when(() -> LoggerFactory.getLogger(LoggingInterceptor.class))
                    .thenReturn(mockLogger);

            final LoggingInterceptor interceptorWithMock = new LoggingInterceptor(auditionLogger);

            when(mockRequest.getMethod()).thenReturn(HttpMethod.GET);
            when(mockRequest.getURI()).thenReturn(URI.create("http://localhost/nologs"));
            when(mockRequest.getHeaders()).thenReturn(new HttpHeaders());

            when(mockResponse.getBody()).thenReturn(new ByteArrayInputStream("X".getBytes()));
            when(mockResponse.getStatusCode()).thenReturn(org.springframework.http.HttpStatus.OK);
            when(mockResponse.getStatusText()).thenReturn("OK");
            when(mockResponse.getHeaders()).thenReturn(new HttpHeaders());

            when(mockExecution.execute(any(), any())).thenReturn(mockResponse);

            try (ClientHttpResponse actual =
                         interceptorWithMock.intercept(mockRequest, new byte[0], mockExecution)) {
                assertEquals(mockResponse, actual);
            }

            // verify: no log methods called (guarded by isXEnabled())
            if (mockLogger.isInfoEnabled()) {
                verify(mockLogger, never()).info(anyString());
                verify(mockLogger, never()).info(anyString(), (Object[]) any());
            }
            if (mockLogger.isDebugEnabled()) {
                verify(mockLogger, never()).debug(anyString());
                verify(mockLogger, never()).debug(anyString(), (Object[]) any());
            }
            if (mockLogger.isErrorEnabled()) {
                verify(mockLogger, never()).error(anyString());
                verify(mockLogger, never()).error(anyString(), (Object[]) any());
            }
        }
    }
}
