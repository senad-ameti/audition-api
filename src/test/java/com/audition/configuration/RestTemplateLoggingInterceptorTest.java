package com.audition.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.common.logging.AuditionLogger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

@ExtendWith(MockitoExtension.class)
class RestTemplateLoggingInterceptorTest {

    @Mock
    private AuditionLogger auditionLogger;

    @InjectMocks
    private RestTemplateLoggingInterceptor interceptor;

    @Test
    void interceptLogsRequestAndReturnsResponse() throws IOException {
        final HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("https://example.com/posts"));
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());

        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(
            new ByteArrayInputStream("{\"id\":1}".getBytes(StandardCharsets.UTF_8)), HttpStatus.OK));

        try (ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution)) {
            assertNotNull(result);
        }
        verify(auditionLogger, atLeastOnce()).info(any(Logger.class), any(String.class), any());
    }

    @Test
    void interceptLogsRequestBodyWhenPresent() throws IOException {
        final HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("https://example.com/posts"));
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());

        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(
            new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)), HttpStatus.CREATED));

        final byte[] body = "{\"title\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        try (ClientHttpResponse ignored = interceptor.intercept(request, body, execution)) {
            verify(auditionLogger, atLeastOnce()).info(any(Logger.class), eq("Request Body: {}"), any(Object.class));
        }
    }
}
