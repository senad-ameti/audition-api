package component.com.audition.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.audition.AuditionApplication;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AuditionApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableWireMock
@DisplayName("AuditionController Component Tests")
class AuditionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @InjectWireMock
    private WireMockServer wireMockServer;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetWireMock() {
        if (wireMockServer != null) {
            wireMockServer.resetAll();
        }
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(name -> {
                Cache c = cacheManager.getCache(name);
                if (c != null) {
                    c.clear();
                }
            });
        }
    }

    @Nested
    @DisplayName("GET /posts")
    class GetPosts {

        @Test
        @DisplayName("returns posts from remote API mapped to JSON list")
        void returnsPosts() throws Exception {
            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/posts"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[{" +
                        "\"userId\":1,\"id\":10,\"title\":\"alpha\",\"body\":\"lorem\"}," +
                        "{\"userId\":2,\"id\":20,\"title\":\"beta\",\"body\":\"ipsum\"}]")));

            mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(10)))
                .andExpect(jsonPath("$[0].title", is("alpha")))
                .andExpect(jsonPath("$[1].id", is(20)))
                .andExpect(jsonPath("$[1].title", is("beta")));
        }

        @Test
        @DisplayName("returns empty array when upstream has no posts")
        void returnsEmptyArray() throws Exception {
            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/posts"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[]")));

            mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /posts/{id}")
    class GetPostById {

        @Test
        @DisplayName("returns a single post when found")
        void returnsPost() throws Exception {
            final String postId = "1";
            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/posts/" + postId))
                .willReturn(WireMock.aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"userId\":1,\"id\":1,\"title\":\"Test Title\",\"body\":\"Test Body\"}")));

            mockMvc.perform(get("/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Title")));
        }
    }
}
