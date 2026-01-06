package com.audition.configuration;

import com.audition.common.logging.LoggingInterceptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
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
    private final transient LoggingInterceptor loggingInterceptor;

    public WebServiceConfiguration(final LoggingInterceptor loggingInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
    }

    @Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        // 1. allows for date format as yyyy-MM-dd
        mapper.setDateFormat(new SimpleDateFormat(YEAR_MONTH_DAY_PATTERN, Locale.ROOT));
        mapper.registerModule(getTimeModule());
        // 2. Does not fail on unknown properties
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 3. maps to camelCase
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        // 4. Does not include null values or empty values
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        // 5. does not write datas as timestamps.
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return mapper;
    }

    private Module getTimeModule() {
        final JavaTimeModule timeModule = new JavaTimeModule();
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(YEAR_MONTH_DAY_PATTERN);
        timeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

        return timeModule;
    }

    @Bean
    public RestTemplate restTemplate(final ObjectMapper objectMapper) {
        final RestTemplate restTemplate = new RestTemplate(
            new BufferingClientHttpRequestFactory(createClientFactory()));
        // remove duplicate message converters if any
        restTemplate.getMessageConverters().removeIf(
            converter -> converter instanceof MappingJackson2HttpMessageConverter);
        // use object mapper
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter(objectMapper));
        // create a logging interceptor that logs request/response for rest template calls.
        restTemplate.setInterceptors(Collections.singletonList(loggingInterceptor));

        return restTemplate;
    }

    private SimpleClientHttpRequestFactory createClientFactory() {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setOutputStreaming(false);
        return requestFactory;
    }
}
