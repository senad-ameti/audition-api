package component.com.audition.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.audition.AuditionApplication;
import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionPost;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = AuditionApplication.class
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@MockitoSettings(strictness = Strictness.LENIENT)
@EnableWireMock
class AuditionIntegrationClientTest {

    @InjectWireMock
    private WireMockServer wireMockServer;

    @Autowired
    private AuditionIntegrationClient auditionIntegrationClient;

    @SpyBean
    private AuditionLogger auditionLogger;

    @AfterEach
    void resetWireMockInteractions() {
        wireMockServer.resetAll();
    }


    @Test
    @DisplayName("Returns posts when upstream responds with content")
    void getPostsReturnsRemotePayload() {
        stubPostsResponse(200, """
            [
              {"userId": 1, "id": 10, "title": "alpha", "body": "lorem"},
              {"userId": 2, "id": 20, "title": "beta", "body": "ipsum"}
            ]
            """);

        final List<AuditionPost> posts = auditionIntegrationClient.getPosts();

        assertAll(
            () -> assertEquals(2, posts.size(), "Expected the remote payload to be mapped"),
            () -> assertEquals(10, posts.get(0).getId()),
            () -> assertEquals("alpha", posts.get(0).getTitle()),
            () -> assertEquals(20, posts.get(1).getId()),
            () -> assertEquals("beta", posts.get(1).getTitle())
        );
    }

    @Test
    @DisplayName("Returns an empty list and logs when upstream has no posts")
    void getPostsReturnsEmptyPayload() {
        stubPostsResponse(200, "[]");

        final List<AuditionPost> posts = auditionIntegrationClient.getPosts();

        assertTrue(posts.isEmpty(), "Expected empty response to yield empty list");
        verify(auditionLogger).debug(any(), eq("No posts found from external API"));
    }

    @Test
    @DisplayName("Throws SystemException when upstream responds with a client error")
    void getPostsThrowsWhenRemoteClientErrors() {
        stubPostsResponse(404, "");

        assertThrows(SystemException.class, () -> auditionIntegrationClient.getPosts());
    }

    private void stubPostsResponse(final int status, final String responseBody) {
        wireMockServer.stubFor(get(urlEqualTo("/posts"))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody)));
    }
}