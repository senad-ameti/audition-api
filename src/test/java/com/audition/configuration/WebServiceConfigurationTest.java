package com.audition.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
class WebServiceConfigurationTest {

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void objectMapper_configs() {
        assertThat(objectMapper.getSerializationConfig().getDefaultPropertyInclusion().getValueInclusion())
            .isEqualTo(JsonInclude.Include.NON_EMPTY);
        assertThat(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
        assertThat(objectMapper.isEnabled(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
    }

    @Test
    void restTemplate_hasMappingJackson2HttpMessageConverter() {
        boolean hasConverter = restTemplate.getMessageConverters().stream()
            .anyMatch(c -> c instanceof MappingJackson2HttpMessageConverter);
        assertThat(hasConverter).isTrue();
    }

    @Test
    void restTemplate_hasBufferingFactory() {
        assertThat(restTemplate.getInterceptors()).isNotEmpty();
    }

    @Test
    void loggingInterceptor_logsRequestAndNonEmptyResponse() {
        mockServer.expect(requestTo(Matchers.containsString("example")))
            .andRespond(withSuccess("{\"key\":\"value\"}", MediaType.APPLICATION_JSON));

        restTemplate.getForObject("https://example.com/test", String.class);
        mockServer.verify();
    }

    @Test
    void loggingInterceptor_logsRequestAndEmptyResponse() {
        mockServer.expect(requestTo(Matchers.containsString("example")))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        restTemplate.getForObject("https://example.com/empty", String.class);
        mockServer.verify();
    }
}
