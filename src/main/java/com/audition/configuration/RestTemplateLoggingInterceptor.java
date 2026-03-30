package com.audition.configuration;

import com.audition.common.logging.AuditionLogger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.GuardLogStatement") // AuditionLogger already checks log level internally
public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor {

    private final AuditionLogger auditionLogger;

    @Override
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
        final ClientHttpRequestExecution execution) throws IOException {
        logRequest(request, body);
        final ClientHttpResponse response = execution.execute(request, body);
        logResponse(response);
        return response;
    }

    private void logRequest(final HttpRequest request, final byte[] body) {
        auditionLogger.info(log, "Request URI: {}", request.getURI());
        auditionLogger.info(log, "Request Method: {}", request.getMethod());
        auditionLogger.info(log, "Request Headers: {}", request.getHeaders());
        auditionLogger.info(log, "Request Body: {}", new String(body, StandardCharsets.UTF_8));
    }

    private void logResponse(final ClientHttpResponse response) throws IOException {
        auditionLogger.info(log, "Response Status: {}", response.getStatusCode());

        final StringBuilder responseBody = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            String line = bufferedReader.readLine();
            while (line != null) {
                responseBody.append(line).append(System.lineSeparator());
                line = bufferedReader.readLine();
            }
        }
        auditionLogger.info(log, "Status code  : {}", response.getStatusCode());
        auditionLogger.info(log, "Headers      : {}", response.getHeaders());
        auditionLogger.debug(log, "Response body: " + responseBody);
    }
}
