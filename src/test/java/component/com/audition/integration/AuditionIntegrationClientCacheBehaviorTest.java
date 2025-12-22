package component.com.audition.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.audition.AuditionApplication;
import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionPost;
import com.audition.model.Comment;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AuditionApplication.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableWireMock
@TestPropertySource(properties = {"audition.cache.enabled=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("AuditionIntegrationClient Cache Behavior Tests")
@SuppressWarnings({"PMD.JUnit5TestShouldBePackagePrivate", "PMD.AvoidDuplicateLiterals",
    "PMD.JUnitTestContainsTooManyAsserts"})
class AuditionIntegrationClientCacheBehaviorTest {

    @InjectWireMock
    private transient WireMockServer wireMockServer;

    @Autowired
    private transient AuditionIntegrationClient auditionIntegrationClient;

    @Autowired(required = false)
    private transient CacheManager cacheManager;

    private void clearCaches() {
        if (cacheManager == null) {
            return;
        }
        cacheManager.getCacheNames().forEach(name -> {
            final Cache c = cacheManager.getCache(name);
            if (c != null) {
                c.clear();
            }
        });
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        clearCaches();
    }

    @Test
    @DisplayName("getPosts should cache results and avoid duplicate API calls")
    void testGetPostsCaching() {
        // stub defined in original test util - reuse helper via classpath method; but keep simple here
        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/posts"))
            .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                .withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("[{\"userId\":1,\"id\":1,\"title\":\"Cached Post\",\"body\":\"Test body\"}]")));

        final List<AuditionPost> firstResult = auditionIntegrationClient.getPosts();
        final List<AuditionPost> secondResult = auditionIntegrationClient.getPosts();

        assertEquals(1, firstResult.size());
        assertEquals(1, secondResult.size());
        assertEquals("Cached Post", firstResult.get(0).getTitle());
        assertEquals("Cached Post", secondResult.get(0).getTitle());

        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts")));
    }

    @Test
    @DisplayName("getPostById should cache results per post ID")
    void testGetPostByIdCaching() {
        final String postId = "123";
        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/posts/" + postId))
            .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                .withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("{\"userId\":1,\"id\":123,\"title\":\"Cached Post\",\"body\":\"Test body\"}")));

        final AuditionPost firstResult = auditionIntegrationClient.getPostById(postId);
        final AuditionPost secondResult = auditionIntegrationClient.getPostById(postId);

        assertEquals(123, firstResult.getId());
        assertEquals(123, secondResult.getId());
        assertEquals("Cached Post", firstResult.getTitle());
        assertEquals("Cached Post", secondResult.getTitle());

        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts/" + postId)));
    }

    @Test
    @DisplayName("getCommentsForPost should cache results per post ID")
    void testGetCommentsForPostCaching() {
        final String postId = "123";
        wireMockServer.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/posts/" + postId + "/comments"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                    .withStatus(200).withHeader("Content-Type", "application/json")
                    .withBody(
                        "[{\"id\":1,\"postId\":123,\"name\":\"Cached Comment\",\"email\":\"test@example.com\",\"body\":\"Comment body\"}]")
                ));

        final List<Comment> firstResult = auditionIntegrationClient.getCommentsForPost(postId);
        final List<Comment> secondResult = auditionIntegrationClient.getCommentsForPost(postId);

        assertEquals(1, firstResult.size());
        assertEquals(1, secondResult.size());
        assertEquals("Cached Comment", firstResult.get(0).getName());
        assertEquals("Cached Comment", secondResult.get(0).getName());

        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts/" + postId + "/comments")));
    }

    @Test
    @DisplayName("getPostWithComments should cache complete result and avoid duplicate external calls")
    void testGetPostWithCommentsCaching() {
        final String postId = "123";
        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/posts/" + postId))
            .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                .withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("{\"userId\":1,\"id\":123,\"title\":\"Test Post\",\"body\":\"Test body\"}")));
        wireMockServer.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/posts/" + postId + "/comments"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                    .withStatus(200).withHeader("Content-Type", "application/json")
                    .withBody(
                        "[{\"id\":1,\"postId\":123,\"name\":\"Test Comment\",\"email\":\"test@example.com\",\"body\":\"Comment body\"}]")
                ));

        final AuditionPost firstResult = auditionIntegrationClient.getPostWithComments(postId);
        final AuditionPost secondResult = auditionIntegrationClient.getPostWithComments(postId);

        assertEquals(123, firstResult.getId());
        assertEquals(123, secondResult.getId());
        assertEquals("Test Post", firstResult.getTitle());
        assertEquals("Test Post", secondResult.getTitle());
        assertEquals(1, firstResult.getComments().size());
        assertEquals(1, secondResult.getComments().size());
        assertEquals("Test Comment", firstResult.getComments().get(0).getName());
        assertEquals("Test Comment", secondResult.getComments().get(0).getName());

        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts/" + postId)));
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts/" + postId + "/comments")));
    }

    @Test
    @DisplayName("Different post IDs should have separate cache entries")
    void testSeparateCacheEntriesForDifferentPostIds() {
        final String postId1 = "1";
        final String postId2 = "2";

        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/posts/" + postId1))
            .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                .withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("{\"userId\":1,\"id\":1,\"title\":\"Post 1\",\"body\":\"Body 1\"}")));
        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/posts/" + postId2))
            .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                .withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("{\"userId\":1,\"id\":2,\"title\":\"Post 2\",\"body\":\"Body 2\"}")));

        final AuditionPost result1 = auditionIntegrationClient.getPostById(postId1);
        final AuditionPost result2 = auditionIntegrationClient.getPostById(postId2);

        final AuditionPost cachedResult1 = auditionIntegrationClient.getPostById(postId1);
        final AuditionPost cachedResult2 = auditionIntegrationClient.getPostById(postId2);

        assertEquals("Post 1", result1.getTitle());
        assertEquals("Post 2", result2.getTitle());
        assertEquals("Post 1", cachedResult1.getTitle());
        assertEquals("Post 2", cachedResult2.getTitle());

        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts/" + postId1)));
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts/" + postId2)));
    }
}
