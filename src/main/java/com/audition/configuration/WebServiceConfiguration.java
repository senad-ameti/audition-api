package com.audition.configuration;


import com.audition.common.logging.AuditionLogger;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebServiceConfiguration implements WebMvcConfigurer {

    private static final String YEAR_MONTH_DAY_PATTERN = "yyyy-MM-dd";
    private final ResponseHeaderInjector responseHeaderInjector;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(responseHeaderInjector);
    }

    @Bean
    public JsonMapper objectMapper() {
        return JsonMapper.builder()
            // Allows for date format as yyyy-MM-dd
            .defaultDateFormat(new SimpleDateFormat(YEAR_MONTH_DAY_PATTERN))
            // Does not fail on unknown properties
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // Maps to camelCase
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            // Does not include null values or empty values
            .serializationInclusion(JsonInclude.Include.NON_EMPTY)
            // Does not write dates as timestamps
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    }

    @Bean
    public RestTemplate restTemplate(JsonMapper objectMapper, AuditionLogger auditionLogger) {
        final RestTemplate restTemplate = new RestTemplate(
            new BufferingClientHttpRequestFactory(createClientFactory()));

        // Use object mapper
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        restTemplate.setMessageConverters(List.of(converter));

        // Logging interceptor using AuditionLogger with tracing support
        restTemplate.setInterceptors(List.of(loggingInterceptor(auditionLogger)));

        return restTemplate;
    }

    private SimpleClientHttpRequestFactory createClientFactory() {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setOutputStreaming(false);
        return requestFactory;
    }

    private ClientHttpRequestInterceptor loggingInterceptor(AuditionLogger auditionLogger) {
        return (request, body, execution) -> {
            logRequest(request, body, auditionLogger);
            ClientHttpResponse response = execution.execute(request, body);
            logResponse(response, auditionLogger);
            return response;
        };
    }

    private void logRequest(HttpRequest request, byte[] body, AuditionLogger auditionLogger) {
        auditionLogger.info(log, "Request URI: {}", request.getURI());
        auditionLogger.info(log, "Request Method: {}", request.getMethod());
        auditionLogger.info(log, "Request Headers: {}", request.getHeaders());
        auditionLogger.info(log, "Request Body: {}", new String(body, StandardCharsets.UTF_8));
    }

    private void logResponse(ClientHttpResponse response, AuditionLogger auditionLogger) throws IOException {
        auditionLogger.info(log, "Response Status: {}", response.getStatusCode());

        StringBuilder responseBody = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                responseBody.append(line).append(System.lineSeparator());
            }
        }
        auditionLogger.info(log, "Status code  : {}" + response.getStatusCode());
        auditionLogger.info(log, "Headers      : {}" + response.getHeaders());
        auditionLogger.debug(log, "Response body: {}" + responseBody);
    }
}
