package com.audition.configuration;

import com.audition.common.logging.AuditionLogger;
import com.audition.common.logging.RestTemplateLoggingInterceptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.text.SimpleDateFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebServiceConfiguration implements WebMvcConfigurer {

    private static final String YEAR_MONTH_DAY_PATTERN = "yyyy-MM-dd";

    @Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();

        // 1. allows for date format as yyyy-MM-dd
        objectMapper.setDateFormat(new SimpleDateFormat(YEAR_MONTH_DAY_PATTERN));

        // 2. Does not fail on unknown properties
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 3. maps to camelCase
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

        // 4. Does not include null values or empty values
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        // 5. does not write dates as timestamps
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return objectMapper;
    }

    @Bean
    public RestTemplate restTemplate(final ObjectMapper objectMapper,
        final AuditionLogger auditionLogger,
        @Value("${audition.logging.rest-template.enabled:true}") final boolean loggingEnabled) {
        final RestTemplate restTemplate = new RestTemplate(
            new BufferingClientHttpRequestFactory(createClientFactory()));

        // Remove default Jackson converter and add custom one with our ObjectMapper
        restTemplate.getMessageConverters().removeIf(converter ->
            converter instanceof MappingJackson2HttpMessageConverter);

        final MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setObjectMapper(objectMapper);
        restTemplate.getMessageConverters().add(jacksonConverter);

        // Add logging interceptor for request/response logging
        restTemplate.getInterceptors().add(new RestTemplateLoggingInterceptor(auditionLogger, loggingEnabled));

        return restTemplate;
    }


    private SimpleClientHttpRequestFactory createClientFactory() {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setOutputStreaming(false);
        return requestFactory;
    }
}
