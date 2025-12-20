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

    @BeforeEach
    void resetWireMock() {
        if (wireMockServer != null) {
            wireMockServer.resetAll();
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
                    .withBody("[{"
                        + "\"userId\":1,\"id\":10,\"title\":\"alpha\",\"body\":\"lorem\"},"
                        + "{\"userId\":2,\"id\":20,\"title\":\"beta\",\"body\":\"ipsum\"}]")));

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

        @Test
        @DisplayName("filters posts by userId query parameter")
        void filtersPostsByUserId() throws Exception {
            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/posts"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[{"
                        + "\"userId\":1,\"id\":10,\"title\":\"alpha\",\"body\":\"lorem\"},"
                        + "{\"userId\":2,\"id\":20,\"title\":\"beta\",\"body\":\"ipsum\"},"
                        + "{\"userId\":1,\"id\":30,\"title\":\"gamma\",\"body\":\"dolor\"}]")));

            mockMvc.perform(get("/posts?userId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId", is(1)))
                .andExpect(jsonPath("$[0].id", is(10)))
                .andExpect(jsonPath("$[1].userId", is(1)))
                .andExpect(jsonPath("$[1].id", is(30)));
        }

        @Test
        @DisplayName("filters posts by id query parameter")
        void filtersPostsById() throws Exception {
            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/posts"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[{"
                        + "\"userId\":1,\"id\":10,\"title\":\"alpha\",\"body\":\"lorem\"},"
                        + "{\"userId\":2,\"id\":20,\"title\":\"beta\",\"body\":\"ipsum\"},"
                        + "{\"userId\":3,\"id\":30,\"title\":\"gamma\",\"body\":\"dolor\"}]")));

            mockMvc.perform(get("/posts?id=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(20)))
                .andExpect(jsonPath("$[0].userId", is(2)))
                .andExpect(jsonPath("$[0].title", is("beta")));
        }

        @Test
        @DisplayName("filters posts by both userId and id with AND logic")
        void filtersPostsByUserIdAndId() throws Exception {
            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/posts"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[{"
                        + "\"userId\":1,\"id\":10,\"title\":\"alpha\",\"body\":\"lorem\"},"
                        + "{\"userId\":2,\"id\":20,\"title\":\"beta\",\"body\":\"ipsum\"},"
                        + "{\"userId\":1,\"id\":30,\"title\":\"gamma\",\"body\":\"dolor\"}]")));

            mockMvc.perform(get("/posts?userId=1&id=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(1)))
                .andExpect(jsonPath("$[0].id", is(10)))
                .andExpect(jsonPath("$[0].title", is("alpha")));
        }

        @Test
        @DisplayName("returns empty array when filters don't match any posts")
        void returnsEmptyWhenFiltersDoNotMatch() throws Exception {
            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/posts"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[{"
                        + "\"userId\":1,\"id\":10,\"title\":\"alpha\",\"body\":\"lorem\"},"
                        + "{\"userId\":2,\"id\":20,\"title\":\"beta\",\"body\":\"ipsum\"}]")));

            mockMvc.perform(get("/posts?userId=999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("returns empty array when both userId and id filters don't match together")
        void returnsEmptyWhenBothFiltersDoNotMatchTogether() throws Exception {
            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/posts"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[{"
                        + "\"userId\":1,\"id\":10,\"title\":\"alpha\",\"body\":\"lorem\"},"
                        + "{\"userId\":2,\"id\":20,\"title\":\"beta\",\"body\":\"ipsum\"}]")));

            // userId=1 exists but not with id=20 (id=20 belongs to userId=2)
            mockMvc.perform(get("/posts?userId=1&id=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("returns 400 Bad Request when userId is not a valid integer")
        void returnsBadRequestWhenUserIdIsNotInteger() throws Exception {
            mockMvc.perform(get("/posts?userId=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request")))
                .andExpect(jsonPath("$.detail", is("User ID must be a valid integer")));
        }

        @Test
        @DisplayName("returns 400 Bad Request when userId is zero")
        void returnsBadRequestWhenUserIdIsZero() throws Exception {
            mockMvc.perform(get("/posts?userId=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request")))
                .andExpect(jsonPath("$.detail", is("User ID must be a positive integer")));
        }

        @Test
        @DisplayName("returns 400 Bad Request when userId is negative")
        void returnsBadRequestWhenUserIdIsNegative() throws Exception {
            mockMvc.perform(get("/posts?userId=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request")))
                .andExpect(jsonPath("$.detail", is("User ID must be a positive integer")));
        }

        @Test
        @DisplayName("returns 400 Bad Request when id is not a valid integer")
        void returnsBadRequestWhenIdIsNotInteger() throws Exception {
            mockMvc.perform(get("/posts?id=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request")))
                .andExpect(jsonPath("$.detail", is("ID must be a valid integer")));
        }

        @Test
        @DisplayName("returns 400 Bad Request when id is zero")
        void returnsBadRequestWhenIdIsZero() throws Exception {
            mockMvc.perform(get("/posts?id=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request")))
                .andExpect(jsonPath("$.detail", is("ID must be a positive integer")));
        }

        @Test
        @DisplayName("returns 400 Bad Request when id is negative")
        void returnsBadRequestWhenIdIsNegative() throws Exception {
            mockMvc.perform(get("/posts?id=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request")))
                .andExpect(jsonPath("$.detail", is("ID must be a positive integer")));
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

        @Test
        @DisplayName("returns 400 Bad Request when postId is empty")
        void returnsBadRequestWhenPostIdIsEmpty() throws Exception {
            mockMvc.perform(get("/posts/ "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request")))
                .andExpect(jsonPath("$.detail", is("Post ID cannot be null or empty")));
        }

        @Test
        @DisplayName("returns 400 Bad Request when postId is not a valid integer")
        void returnsBadRequestWhenPostIdIsNotInteger() throws Exception {
            mockMvc.perform(get("/posts/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request")))
                .andExpect(jsonPath("$.detail", is("Post ID must be a valid integer")));
        }

        @Test
        @DisplayName("returns 400 Bad Request when postId is a decimal")
        void returnsBadRequestWhenPostIdIsDecimal() throws Exception {
            mockMvc.perform(get("/posts/1.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request")))
                .andExpect(jsonPath("$.detail", is("Post ID must be a valid integer")));
        }

        @Test
        @DisplayName("returns 400 Bad Request when postId is zero")
        void returnsBadRequestWhenPostIdIsZero() throws Exception {
            mockMvc.perform(get("/posts/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request")))
                .andExpect(jsonPath("$.detail", is("Post ID must be a positive integer")));
        }

        @Test
        @DisplayName("returns 400 Bad Request when postId is negative")
        void returnsBadRequestWhenPostIdIsNegative() throws Exception {
            mockMvc.perform(get("/posts/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request")))
                .andExpect(jsonPath("$.detail", is("Post ID must be a positive integer")));
        }
    }
}
