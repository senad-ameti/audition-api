package com.audition.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.common.logging.AuditionLogger;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class LoggingInterceptorTest {

    private final AuditionLogger auditionLogger = mock(AuditionLogger.class);

    private final LoggingInterceptor interceptor = new LoggingInterceptor(auditionLogger, "dev");

    @Test
    void loggingInterceptorShouldLogRequestAndResponseBody() throws Exception {

        final HttpRequest mockRequest = Mockito.mock(HttpRequest.class);
        final ClientHttpRequestExecution mockExecution = Mockito.mock(ClientHttpRequestExecution.class);
        final ClientHttpResponse mockResponse = Mockito.mock(ClientHttpResponse.class);

        when(mockResponse.getBody()).thenReturn(InputStream.nullInputStream());

        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);

        interceptor.intercept(mockRequest, "userId=1".getBytes(StandardCharsets.UTF_8), mockExecution);

        // Verify that the request body and response body is logged
        verify(auditionLogger).debug(any(), contains("Request body: {}"), eq("userId=1"));
        verify(auditionLogger).debug(any(), contains("Response body: {}"), eq(""));
    }
}
