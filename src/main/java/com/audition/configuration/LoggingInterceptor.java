package com.audition.configuration;

import com.audition.common.logging.AuditionLogger;
import jakarta.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingInterceptor.class);
    private final AuditionLogger auditionLogger;
    private final String profile;

    public LoggingInterceptor(final AuditionLogger auditionLogger, @Value("${spring.profiles.active}") String profile) {
        this.auditionLogger = auditionLogger;
        this.profile = profile;
    }

    @Override
    public @Nonnull ClientHttpResponse intercept(
        @Nonnull final HttpRequest request,
        @Nonnull final byte[] reqBody,
        final ClientHttpRequestExecution ex
    ) throws IOException {

        final ClientHttpResponse response = ex.execute(request, reqBody);

        // loading the entire body data into memory could expose our application to performance issues
        // so only log it for debug logs
        if ("dev".equals(profile)) {
            auditionLogger.debug(LOGGER, "Request body: {}", new String(reqBody, StandardCharsets.UTF_8));

            try (InputStreamReader isr = new InputStreamReader(response.getBody(), StandardCharsets.UTF_8)) {
                try (BufferedReader bufferedReader = new BufferedReader(isr)) {
                    final String body = bufferedReader.lines()
                        .collect(Collectors.joining("\n"));

                    auditionLogger.debug(LOGGER, "Response body: {}", body);
                }
            }
        }

        return response;
    }
}
