package com.audition.configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.text.SimpleDateFormat;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebServiceConfiguration implements WebMvcConfigurer {

    private static final String YEAR_MONTH_DAY_PATTERN = "yyyy-MM-dd";

    @Value("${spring.profiles.active}")
    private String profile;

    @Bean
    public ObjectMapper objectMapper() {
        SimpleDateFormat df = new SimpleDateFormat(YEAR_MONTH_DAY_PATTERN);

        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setDateFormat(df)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(Include.NON_EMPTY)
            .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }

    @Bean
    public MappingJackson2HttpMessageConverter createMappingJacksonHttpMessageConverter(ObjectMapper objectMapper) {
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    @Bean
    public RestTemplate restTemplate(
        final RestTemplateBuilder builder,
        final MappingJackson2HttpMessageConverter customJackson2HttpMessageConverter,
        final LoggingInterceptor loggingInterceptor
    ) {
        Supplier<ClientHttpRequestFactory> clientHttpRequestFactorySupplier;

        if ("dev".equals(profile)) {
            // setup BufferingClientHttpRequestFactory for dev profile
            clientHttpRequestFactorySupplier = () -> new BufferingClientHttpRequestFactory(createClientFactory());
        } else {
            clientHttpRequestFactorySupplier = this::createClientFactory;
        }

        return builder.requestFactory(clientHttpRequestFactorySupplier)
            .additionalMessageConverters(customJackson2HttpMessageConverter)
            .additionalInterceptors(loggingInterceptor).build();
    }

    private SimpleClientHttpRequestFactory createClientFactory() {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setOutputStreaming(false);
        return requestFactory;
    }
}
