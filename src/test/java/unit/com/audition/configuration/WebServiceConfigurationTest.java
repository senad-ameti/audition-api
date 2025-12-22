package unit.com.audition.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.audition.common.logging.AuditionLogger;
import com.audition.common.logging.RestTemplateLoggingInterceptor;
import com.audition.configuration.WebServiceConfiguration;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.text.SimpleDateFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@DisplayName("WebServiceConfiguration Unit Tests")
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
class WebServiceConfigurationTest {

    private transient WebServiceConfiguration configuration;
    private transient AuditionLogger auditionLogger;

    @BeforeEach
    void setUp() {
        configuration = new WebServiceConfiguration();
        auditionLogger = new AuditionLogger();
    }

    @Nested
    @DisplayName("objectMapper")
    class ObjectMapperTests {

        @Test
        @DisplayName("Creates ObjectMapper with correct date format")
        void createsObjectMapperWithDateFormat() {
            final ObjectMapper objectMapper = configuration.objectMapper();

            assertNotNull(objectMapper);
            final SimpleDateFormat dateFormat = (SimpleDateFormat) objectMapper.getDateFormat();
            assertEquals("yyyy-MM-dd", dateFormat.toPattern());
        }

        @Test
        @DisplayName("Configures ObjectMapper to not fail on unknown properties")
        void configuresFailOnUnknownProperties() {
            final ObjectMapper objectMapper = configuration.objectMapper();

            assertFalse(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        }

        @Test
        @DisplayName("Configures ObjectMapper with lower camel case naming strategy")
        void configuresPropertyNamingStrategy() {
            final ObjectMapper objectMapper = configuration.objectMapper();

            assertEquals(PropertyNamingStrategies.LOWER_CAMEL_CASE,
                objectMapper.getPropertyNamingStrategy());
        }

        @Test
        @DisplayName("Configures ObjectMapper to exclude empty values from serialization")
        void configuresSerializationInclusion() {
            final ObjectMapper objectMapper = configuration.objectMapper();

            assertEquals(JsonInclude.Include.NON_EMPTY,
                objectMapper.getSerializationConfig().getDefaultPropertyInclusion().getValueInclusion());
        }

        @Test
        @DisplayName("Configures ObjectMapper to not write dates as timestamps")
        void configuresDateSerialization() {
            final ObjectMapper objectMapper = configuration.objectMapper();

            assertFalse(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        }
    }

    @Nested
    @DisplayName("restTemplate")
    class RestTemplateTests {

        @Test
        @DisplayName("Creates RestTemplate with custom ObjectMapper")
        void createsRestTemplateWithCustomObjectMapper() {
            final ObjectMapper customObjectMapper = configuration.objectMapper();
            final RestTemplate restTemplate = configuration.restTemplate(customObjectMapper, auditionLogger, true);

            assertNotNull(restTemplate);
            assertFalse(restTemplate.getMessageConverters().isEmpty());
        }

        @Test
        @DisplayName("RestTemplate uses MappingJackson2HttpMessageConverter with custom ObjectMapper")
        void usesMappingJackson2HttpMessageConverter() {
            final ObjectMapper customObjectMapper = configuration.objectMapper();
            final RestTemplate restTemplate = configuration.restTemplate(customObjectMapper, auditionLogger, true);

            MappingJackson2HttpMessageConverter jacksonConverter = null;

            for (final HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
                if (converter instanceof MappingJackson2HttpMessageConverter) {
                    jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
                    break;
                }
            }

            assertNotNull(jacksonConverter, "RestTemplate should have MappingJackson2HttpMessageConverter");
            assertEquals(customObjectMapper, jacksonConverter.getObjectMapper());
        }

        @Test
        @DisplayName("RestTemplate's Jackson converter has correct date format configuration")
        void jacksonConverterHasCorrectDateFormat() {
            final ObjectMapper customObjectMapper = configuration.objectMapper();
            final RestTemplate restTemplate = configuration.restTemplate(customObjectMapper, auditionLogger, true);

            MappingJackson2HttpMessageConverter jacksonConverter = null;
            for (final HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
                if (converter instanceof MappingJackson2HttpMessageConverter) {
                    jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
                    break;
                }
            }

            assertNotNull(jacksonConverter);
            final ObjectMapper objectMapper = jacksonConverter.getObjectMapper();
            final SimpleDateFormat dateFormat = (SimpleDateFormat) objectMapper.getDateFormat();
            assertEquals("yyyy-MM-dd", dateFormat.toPattern());
        }

        @Test
        @DisplayName("RestTemplate's Jackson converter has correct property naming strategy")
        void jacksonConverterHasCorrectNamingStrategy() {
            final ObjectMapper customObjectMapper = configuration.objectMapper();
            final RestTemplate restTemplate = configuration.restTemplate(customObjectMapper, auditionLogger, true);

            MappingJackson2HttpMessageConverter jacksonConverter = null;
            for (final HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
                if (converter instanceof MappingJackson2HttpMessageConverter) {
                    jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
                    break;
                }
            }

            assertNotNull(jacksonConverter);
            final ObjectMapper objectMapper = jacksonConverter.getObjectMapper();
            assertEquals(PropertyNamingStrategies.LOWER_CAMEL_CASE,
                objectMapper.getPropertyNamingStrategy());
        }

        @Test
        @DisplayName("RestTemplate's Jackson converter does not write dates as timestamps")
        void jacksonConverterDoesNotWriteDatesAsTimestamps() {
            final ObjectMapper customObjectMapper = configuration.objectMapper();
            final RestTemplate restTemplate = configuration.restTemplate(customObjectMapper, auditionLogger, true);

            MappingJackson2HttpMessageConverter jacksonConverter = null;
            for (final HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
                if (converter instanceof MappingJackson2HttpMessageConverter) {
                    jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
                    break;
                }
            }

            assertNotNull(jacksonConverter);
            final ObjectMapper objectMapper = jacksonConverter.getObjectMapper();
            assertFalse(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        }
    }

    @Nested
    @DisplayName("restTemplate interceptor")
    class RestTemplateInterceptorTests {

        @Test
        @DisplayName("RestTemplate has exactly one interceptor when logging is enabled")
        void hasOneInterceptorWhenEnabled() {
            final ObjectMapper customObjectMapper = configuration.objectMapper();
            final RestTemplate restTemplate = configuration.restTemplate(customObjectMapper, auditionLogger, true);

            assertEquals(1, restTemplate.getInterceptors().size());
        }

        @Test
        @DisplayName("RestTemplate interceptor is RestTemplateLoggingInterceptor")
        void interceptorIsRestTemplateLoggingInterceptor() {
            final ObjectMapper customObjectMapper = configuration.objectMapper();
            final RestTemplate restTemplate = configuration.restTemplate(customObjectMapper, auditionLogger, true);

            assertTrue(restTemplate.getInterceptors().get(0) instanceof RestTemplateLoggingInterceptor);
        }

        @Test
        @DisplayName("RestTemplate has interceptor even when logging is disabled")
        void hasInterceptorWhenDisabled() {
            final ObjectMapper customObjectMapper = configuration.objectMapper();
            final RestTemplate restTemplate = configuration.restTemplate(customObjectMapper, auditionLogger, false);

            assertEquals(1, restTemplate.getInterceptors().size());
            assertTrue(restTemplate.getInterceptors().get(0) instanceof RestTemplateLoggingInterceptor);
        }
    }
}

