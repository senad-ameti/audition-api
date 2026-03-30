package com.audition.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@RequiredArgsConstructor
public class WebServiceConfiguration implements WebMvcConfigurer {

    @Value("${http.client.pool.max-total:100}")
    private int poolMaxTotal;

    @Value("${http.client.pool.max-per-route:20}")
    private int poolMaxPerRoute;

    @Value("${http.client.timeout.connect:5}")
    private int connectTimeoutSeconds;

    @Value("${http.client.timeout.response:10}")
    private int responseTimeoutSeconds;

    @Value("${http.client.timeout.connection-request:3}")
    private int connectionRequestTimeoutSeconds;

    private final ResponseHeaderInjector responseHeaderInjector;
    private final RestTemplateLoggingInterceptor loggingInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(responseHeaderInjector);
    }

    @Bean
    public JsonMapper objectMapper() {
        return JsonMapper.builder()
            .addModule(JavaTimeModuleFactory.create())
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
    public RestTemplate restTemplate(final JsonMapper objectMapper,
        final PoolingHttpClientConnectionManager connectionManager) {
        final RestTemplate restTemplate = new RestTemplate(
            new BufferingClientHttpRequestFactory(createClientFactory(connectionManager)));

        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        restTemplate.setMessageConverters(List.of(converter));
        restTemplate.setInterceptors(List.of(loggingInterceptor));

        return restTemplate;
    }

    @Bean
    public PoolingHttpClientConnectionManager connectionManager() {
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(poolMaxTotal);
        connectionManager.setDefaultMaxPerRoute(poolMaxPerRoute);
        connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(connectTimeoutSeconds))
            .build());
        return connectionManager;
    }

    private HttpComponentsClientHttpRequestFactory createClientFactory(
        final PoolingHttpClientConnectionManager connectionManager) {

        final RequestConfig requestConfig = RequestConfig.custom()
            .setResponseTimeout(Timeout.ofSeconds(responseTimeoutSeconds))
            .setConnectionRequestTimeout(Timeout.ofSeconds(connectionRequestTimeoutSeconds))
            .build();

        final CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
