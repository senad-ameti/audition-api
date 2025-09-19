package com.audition.common.interceptor;

import com.audition.common.logging.AuditionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("PMD.GuardLogStatement")
public class LoggingInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingInterceptor.class);

    private final AuditionLogger auditionLogger;

    public LoggingInterceptor(final AuditionLogger auditionLogger) {
        this.auditionLogger = auditionLogger;
    }


    @Override
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
                                        final ClientHttpRequestExecution execution) throws IOException {

        auditionLogger.info(LOGGER, "Request: {} {}", request.getMethod(), request.getURI());
        if (body.length > 0) {
            auditionLogger.debug(LOGGER, "Request body: " + new String(body, StandardCharsets.UTF_8));
        }

        final ClientHttpResponse response = execution.execute(request, body);

        auditionLogger.info(LOGGER, "Response Status: {}", response.getStatusCode());
        final String responseBody = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))
                .lines().reduce("", (a, b) -> a + b);

        if (!responseBody.isEmpty()) {
            auditionLogger.debug(LOGGER, "Response body: " + responseBody);
        }

        return response;
    }
}
