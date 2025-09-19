package com.audition.configuration;

import com.audition.common.interceptor.LoggingInterceptor;
import com.audition.common.logging.AuditionLogger;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;


@Configuration
public class WebServiceConfiguration implements WebMvcConfigurer {
    private final AuditionLogger auditionLogger;

    private static final String YEAR_MONTH_DAY_PATTERN = "yyyy-MM-dd";

    public WebServiceConfiguration(final AuditionLogger auditionLogger) {
        this.auditionLogger = auditionLogger;
    }

    @Bean
    public ObjectMapper objectMapper() {

        final ObjectMapper mapper = new ObjectMapper();
        // Register JavaTimeModule for LocalDate, LocalDateTime, etc.
        mapper.registerModule(new JavaTimeModule());

        // 1. Use yyyy-MM-dd date format (for java.util.Date, Calendar)
        mapper.setDateFormat(new SimpleDateFormat(YEAR_MONTH_DAY_PATTERN, Locale.ROOT));
        // 2. Do not fail on unknown properties
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 3. Explicitly enforce camelCase naming
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

        // 4. Exclude null and empty values
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        // 5. Do not write dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }

    @Bean
    public RestTemplate restTemplate(final ObjectMapper objectMapper) {
        final RestTemplate restTemplate = new RestTemplate(
                new BufferingClientHttpRequestFactory(createClientFactory()));
        // Replace default Jackson converter with custom ObjectMapper
        final List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper));

        // Add logging interceptor
        restTemplate.getInterceptors().add(new LoggingInterceptor(auditionLogger));

        return restTemplate;
    }

    private SimpleClientHttpRequestFactory createClientFactory() {
        return new SimpleClientHttpRequestFactory();
        //setOutputStreaming(false) on SimpleClientHttpRequestFactory has been deprecated
        // since Spring 6.0 because output streaming is now handled automatically.
    }
}
