package com.audition.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.common.logging.AuditionLogger;
import com.audition.common.logging.LoggingInterceptor;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

@ExtendWith(MockitoExtension.class)
public class LoggingInterceptorTest {

    private static final String TEST_REQ_URL = "https://test.com";

    @AfterEach
    void cleanupMdc() {
        MDC.remove("traceId");
        MDC.remove("spanId");
    }

    @Test
    void intercept_validReqResp_successful() throws Exception {
        MDC.put("traceId", "trace-id-123");
        MDC.put("spanId", "span-id-456");

        final AuditionLogger auditionLogger = mock(AuditionLogger.class);
        final LoggingInterceptor interceptor = new LoggingInterceptor(auditionLogger);

        final HttpRequest request = new HttpRequest() {
            @Override
            public HttpMethod getMethod() {
                return HttpMethod.GET;
            }

            @Override
            public URI getURI() {
                return URI.create(TEST_REQ_URL);
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };

        final byte[] body = "request-body".getBytes(StandardCharsets.UTF_8);
        final MockClientHttpResponse response = new MockClientHttpResponse(
            "response-body".getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
        response.getHeaders().add("Content-Type", "application/json");

        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(response);

        final ClientHttpResponse returned = interceptor.intercept(request, body, execution);

        assertThat(returned).isEqualTo(response);
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("application/json");
        assertThat(response.getHeaders().getFirst("X-Trace-Id")).isEqualTo("trace-id-123");
        assertThat(response.getHeaders().getFirst("X-Span-Id")).isEqualTo("span-id-456");

        // verify that logging occurred for both request and response
        verify(auditionLogger, atLeast(2)).info(any(), anyString());
        verify(execution).execute(any(), any());
    }

    @Test
    void intercept_failingRequest_failed() throws Exception {
        final AuditionLogger auditionLogger = mock(AuditionLogger.class);
        final LoggingInterceptor interceptor = new LoggingInterceptor(auditionLogger);

        final HttpRequest failingRequest = new HttpRequest() {
            @Override
            public HttpMethod getMethod() {
                return HttpMethod.GET;
            }

            @Override
            public URI getURI() {
                throw new RuntimeException("get request URI error");
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };

        final byte[] body = "request-body".getBytes(StandardCharsets.UTF_8);
        final MockClientHttpResponse response = new MockClientHttpResponse(
            "response-body".getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
        response.getHeaders().add("Content-Type", "application/json");

        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(response);

        final ClientHttpResponse returned = interceptor.intercept(failingRequest, body, execution);

        assertThat(returned).isEqualTo(response);

        // verify the error logging for request
        verify(auditionLogger).logErrorWithException(any(), eq("Failed to log request"), any());
        verify(execution).execute(any(), any());
    }

    @Test
    void intercept_failingResponse_failed() throws Exception {
        final AuditionLogger auditionLogger = mock(AuditionLogger.class);
        final LoggingInterceptor interceptor = new LoggingInterceptor(auditionLogger);

        final HttpRequest request = new HttpRequest() {
            @Override
            public HttpMethod getMethod() {
                return HttpMethod.GET;
            }

            @Override
            public URI getURI() {
                return URI.create(TEST_REQ_URL);
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };

        final byte[] body = new byte[0];

        final ClientHttpResponse failingResponse = new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() {
                return HttpStatus.OK;
            }

            @Override
            public int getRawStatusCode() {
                return 200;
            }

            @Override
            public String getStatusText() {
                return "OK";
            }

            @Override
            public void close() {
            }

            @Override
            public InputStream getBody() throws IOException {
                throw new IOException("response read error");
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };

        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(failingResponse);

        final ClientHttpResponse returned = interceptor.intercept(request, body, execution);

        assertThat(returned).isEqualTo(failingResponse);

        // verify the error logging for response
        verify(auditionLogger).logErrorWithException(any(), eq("Failed to log response"), any());
        verify(execution).execute(any(), any());
    }
}
