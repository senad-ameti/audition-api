package component.com.audition.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.audition.AuditionApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AuditionApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableWireMock
@DisplayName("Actuator Security Tests")
class ActuatorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${audition.security.actuator-api-key}")
    private String apiKey;

    @Nested
    @DisplayName("Public Actuator Endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("/actuator/health is accessible without authentication")
        void healthEndpointIsPublic() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("/actuator/info is accessible without authentication")
        void infoEndpointIsPublic() throws Exception {
            mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Secured Actuator Endpoints")
    class SecuredEndpoints {

        @Test
        @DisplayName("/actuator/metrics returns 403 without API key")
        void metricsEndpointReturns403WithoutApiKey() throws Exception {
            mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("/actuator/metrics returns 200 with valid API key")
        void metricsEndpointReturns200WithValidApiKey() throws Exception {
            mockMvc.perform(get("/actuator/metrics")
                    .header("X-API-Key", apiKey))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("/actuator/metrics returns 403 with invalid API key")
        void metricsEndpointReturns403WithInvalidApiKey() throws Exception {
            mockMvc.perform(get("/actuator/metrics")
                    .header("X-API-Key", "invalid-key"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("/actuator/env returns 403 without API key")
        void envEndpointReturns403WithoutApiKey() throws Exception {
            mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("/actuator/env returns 200 with valid API key")
        void envEndpointReturns200WithValidApiKey() throws Exception {
            mockMvc.perform(get("/actuator/env")
                    .header("X-API-Key", apiKey))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Application Endpoints")
    class ApplicationEndpoints {

        @Test
        @DisplayName("/posts endpoint remains publicly accessible")
        void postsEndpointIsPublic() throws Exception {
            // This test verifies that our security configuration doesn't interfere
            // with application endpoints. The actual response may vary based on
            // whether external service is available, but it should not return 401.
            mockMvc.perform(get("/posts"))
                .andExpect(result -> {
                    final int status = result.getResponse().getStatus();
                    // Should be anything except 401 Unauthorized
                    if (status == 401) {
                        throw new AssertionError("Expected /posts to be publicly accessible but got 401");
                    }
                });
        }
    }
}

