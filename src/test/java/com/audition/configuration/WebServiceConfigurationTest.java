package com.audition.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void objectMapperConfigs() {
        assertEquals(JsonInclude.Include.NON_EMPTY,
            objectMapper.getSerializationConfig().getDefaultPropertyInclusion().getValueInclusion());
        assertFalse(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        assertFalse(objectMapper.isEnabled(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    void restTemplateHasMappingJackson2HttpMessageConverter() {
        final boolean hasConverter = restTemplate.getMessageConverters().stream()
            .anyMatch(c -> c instanceof MappingJackson2HttpMessageConverter);
        assertTrue(hasConverter);
    }

    @Test
    void restTemplateHasBufferingFactory() {
        assertFalse(restTemplate.getInterceptors().isEmpty());
    }

    @Test
    void loggingInterceptorLogsRequestAndNonEmptyResponse() {
        mockServer.expect(requestTo(Matchers.containsString("example")))
            .andRespond(withSuccess("{\"key\":\"value\"}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> restTemplate.getForObject("https://example.com/test", String.class));
        mockServer.verify();
    }

    @Test
    void loggingInterceptorLogsRequestAndEmptyResponse() {
        mockServer.expect(requestTo(Matchers.containsString("example")))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> restTemplate.getForObject("https://example.com/empty", String.class));
        mockServer.verify();
    }
}
