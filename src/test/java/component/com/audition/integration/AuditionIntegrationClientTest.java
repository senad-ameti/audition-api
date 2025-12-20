package component.com.audition.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.audition.model.Comment;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
@DisplayName("AuditionIntegrationClient Component Tests")
class AuditionIntegrationClientTest {

    @InjectWireMock
    private WireMockServer wireMockServer;

    @Autowired
    private AuditionIntegrationClient auditionIntegrationClient;

    @SpyBean
    private AuditionLogger auditionLogger;

    @Autowired
    private CacheManager cacheManager;

    @AfterEach
    void resetWireMockInteractions() {
        wireMockServer.resetAll();
        clearCaches();
    }

    private void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Nested
    @DisplayName("getPosts")
    class GetPostsTests {

        @Test
        @DisplayName("Returns posts when upstream responds with content")
        void returnsRemotePayload() {
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
        void returnsEmptyPayload() {
            stubPostsResponse(200, "[]");

            final List<AuditionPost> posts = auditionIntegrationClient.getPosts();

            assertTrue(posts.isEmpty(), "Expected empty response to yield empty list");
            verify(auditionLogger).debug(any(), eq("No posts found from external API"));
        }

        @Test
        @DisplayName("Throws SystemException when upstream responds with a client error")
        void throwsWhenRemoteClientErrors() {
            stubPostsResponse(404, "");

            assertThrows(SystemException.class, () -> auditionIntegrationClient.getPosts());
        }
    }

    @Nested
    @DisplayName("getPostById")
    class GetPostByIdTests {

        @Test
        @DisplayName("Returns post when upstream responds with valid post data for given id")
        void returnsRemotePayload() {
            final String postId = "1";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 1, "title": "Test Title", "body": "Test Body"}
                """);

            final AuditionPost post = auditionIntegrationClient.getPostById(postId);

            assertAll(
                () -> assertEquals(1, post.getId()),
                () -> assertEquals(1, post.getUserId()),
                () -> assertEquals("Test Title", post.getTitle()),
                () -> assertEquals("Test Body", post.getBody())
            );
        }

        @Test
        @DisplayName("Returns post with all fields correctly mapped when upstream responds with complete data")
        void mapsAllFieldsCorrectly() {
            final String postId = "42";
            stubPostByIdResponse(postId, 200, """
                {"userId": 5, "id": 42, "title": "sunt aut facere repellat provident", "body": "quia et suscipit\\nsuscipit recusandae"}
                """);

            final AuditionPost post = auditionIntegrationClient.getPostById(postId);

            assertAll(
                () -> assertEquals(42, post.getId()),
                () -> assertEquals(5, post.getUserId()),
                () -> assertEquals("sunt aut facere repellat provident", post.getTitle()),
                () -> assertEquals("quia et suscipit\nsuscipit recusandae", post.getBody())
            );
        }

        @Test
        @DisplayName("Throws SystemException with 404 status when post is not found")
        void throwsNotFoundExceptionWhenPostNotFound() {
            final String postId = "9999";
            stubPostByIdResponse(postId, 404, "{}");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getPostById(postId));

            assertAll(
                () -> assertEquals("Cannot find a Post with id " + postId, exception.getMessage()),
                () -> assertEquals("Resource Not Found", exception.getTitle()),
                () -> assertEquals(404, exception.getStatusCode())
            );
        }

        @Test
        @DisplayName("Throws SystemException when upstream responds with 400 Bad Request")
        void throwsSystemExceptionForBadRequest() {
            final String postId = "invalid";
            stubPostByIdResponse(postId, 400, "Bad Request");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getPostById(postId));

            assertEquals("Unknown Error message", exception.getMessage());
        }

        @Test
        @DisplayName("Throws SystemException when upstream responds with 401 Unauthorized")
        void throwsSystemExceptionForUnauthorized() {
            final String postId = "1";
            stubPostByIdResponse(postId, 401, "Unauthorized");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getPostById(postId));

            assertEquals("Unknown Error message", exception.getMessage());
        }

        @Test
        @DisplayName("Throws SystemException when upstream responds with 403 Forbidden")
        void throwsSystemExceptionForForbidden() {
            final String postId = "1";
            stubPostByIdResponse(postId, 403, "Forbidden");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getPostById(postId));

            assertEquals("Unknown Error message", exception.getMessage());
        }

        @Test
        @DisplayName("Returns post with empty body when upstream responds with empty body field")
        void handlesEmptyBody() {
            final String postId = "10";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 10, "title": "Title Only", "body": ""}
                """);

            final AuditionPost post = auditionIntegrationClient.getPostById(postId);

            assertAll(
                () -> assertEquals(10, post.getId()),
                () -> assertEquals("Title Only", post.getTitle()),
                () -> assertEquals("", post.getBody())
            );
        }

        @Test
        @DisplayName("Returns post with null title when upstream responds with null title field")
        void handlesNullTitle() {
            final String postId = "15";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 15, "title": null, "body": "Body content"}
                """);

            final AuditionPost post = auditionIntegrationClient.getPostById(postId);

            assertAll(
                () -> assertEquals(15, post.getId()),
                () -> assertNull(post.getTitle()),
                () -> assertEquals("Body content", post.getBody())
            );
        }

        @Test
        @DisplayName("Returns post with special characters in title and body")
        void handlesSpecialCharacters() {
            final String postId = "20";
            stubPostByIdResponse(postId, 200, """
                {"userId": 2, "id": 20, "title": "Special <chars> & 'quotes'", "body": "Body with unicode: éàü"}
                """);

            final AuditionPost post = auditionIntegrationClient.getPostById(postId);

            assertAll(
                () -> assertEquals(20, post.getId()),
                () -> assertEquals("Special <chars> & 'quotes'", post.getTitle()),
                () -> assertEquals("Body with unicode: éàü", post.getBody())
            );
        }
    }

    @Nested
    @DisplayName("getPostWithComments")
    class GetPostWithCommentsTests {

        @Test
        @DisplayName("Returns post with comments when both exist")
        void returnsPostWithComments() {
            final String postId = "1";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 1, "title": "Test Title", "body": "Test Body"}
                """);
            stubPostCommentsResponse(postId, 200, """
                [
                  {"postId": 1, "id": 1, "name": "Comment 1", "email": "user1@test.com", "body": "Comment body 1"},
                  {"postId": 1, "id": 2, "name": "Comment 2", "email": "user2@test.com", "body": "Comment body 2"}
                ]
                """);

            final AuditionPost post = auditionIntegrationClient.getPostWithComments(postId);

            assertAll(
                () -> assertEquals(1, post.getId()),
                () -> assertEquals("Test Title", post.getTitle()),
                () -> assertEquals(2, post.getComments().size()),
                () -> assertEquals("Comment 1", post.getComments().get(0).getName()),
                () -> assertEquals("user2@test.com", post.getComments().get(1).getEmail())
            );
        }

        @Test
        @DisplayName("Returns post with empty comments list when no comments exist")
        void returnsPostWithEmptyComments() {
            final String postId = "1";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 1, "title": "Test Title", "body": "Test Body"}
                """);
            stubPostCommentsResponse(postId, 200, "[]");

            final AuditionPost post = auditionIntegrationClient.getPostWithComments(postId);

            assertAll(
                () -> assertEquals(1, post.getId()),
                () -> assertTrue(post.getComments().isEmpty())
            );
            verify(auditionLogger).debug(any(), eq("No comments found for post with id " + postId));
        }

        @Test
        @DisplayName("Throws SystemException with 404 when post not found")
        void throwsNotFoundExceptionWhenPostNotFound() {
            final String postId = "9999";
            stubPostByIdResponse(postId, 404, "{}");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getPostWithComments(postId));

            assertAll(
                () -> assertEquals("Cannot find a Post with id " + postId, exception.getMessage()),
                () -> assertEquals("Resource Not Found", exception.getTitle()),
                () -> assertEquals(404, exception.getStatusCode())
            );
        }

        @Test
        @DisplayName("Throws SystemException with 404 when comments endpoint returns not found")
        void throwsNotFoundExceptionWhenCommentsNotFound() {
            final String postId = "1";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 1, "title": "Test Title", "body": "Test Body"}
                """);
            stubPostCommentsResponse(postId, 404, "{}");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getPostWithComments(postId));

            assertAll(
                () -> assertEquals("Cannot find comments for Post with id " + postId, exception.getMessage()),
                () -> assertEquals("Resource Not Found", exception.getTitle()),
                () -> assertEquals(404, exception.getStatusCode())
            );
        }

        @Test
        @DisplayName("Throws SystemException when comments endpoint returns 400 Bad Request")
        void throwsSystemExceptionForBadRequestOnComments() {
            final String postId = "1";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 1, "title": "Test Title", "body": "Test Body"}
                """);
            stubPostCommentsResponse(postId, 400, "Bad Request");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getPostWithComments(postId));

            assertEquals("Unknown Error message", exception.getMessage());
        }

        @Test
        @DisplayName("Throws SystemException when post endpoint returns 401 Unauthorized")
        void throwsSystemExceptionForUnauthorizedOnPost() {
            final String postId = "1";
            stubPostByIdResponse(postId, 401, "Unauthorized");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getPostWithComments(postId));

            assertEquals("Unknown Error message", exception.getMessage());
        }

        @Test
        @DisplayName("Throws SystemException when comments endpoint returns 403 Forbidden")
        void throwsSystemExceptionForForbiddenOnComments() {
            final String postId = "1";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 1, "title": "Test Title", "body": "Test Body"}
                """);
            stubPostCommentsResponse(postId, 403, "Forbidden");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getPostWithComments(postId));

            assertEquals("Unknown Error message", exception.getMessage());
        }

        @Test
        @DisplayName("Returns post with comments containing special characters")
        void handlesCommentsWithSpecialCharacters() {
            final String postId = "1";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 1, "title": "Test Title", "body": "Test Body"}
                """);
            stubPostCommentsResponse(postId, 200, """
                [
                  {"postId": 1, "id": 1, "name": "Special <chars> & 'quotes'", "email": "test@émail.com", "body": "Body with unicode: éàü"}
                ]
                """);

            final AuditionPost post = auditionIntegrationClient.getPostWithComments(postId);

            assertAll(
                () -> assertEquals(1, post.getComments().size()),
                () -> assertEquals("Special <chars> & 'quotes'", post.getComments().get(0).getName()),
                () -> assertEquals("test@émail.com", post.getComments().get(0).getEmail()),
                () -> assertEquals("Body with unicode: éàü", post.getComments().get(0).getBody())
            );
        }

        @Test
        @DisplayName("Returns post with comments containing newlines in body")
        void handlesCommentsWithNewlines() {
            final String postId = "1";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 1, "title": "Test Title", "body": "Test Body"}
                """);
            stubPostCommentsResponse(postId, 200, """
                [
                  {"postId": 1, "id": 1, "name": "Comment", "email": "test@test.com", "body": "Line1\\nLine2\\nLine3"}
                ]
                """);

            final AuditionPost post = auditionIntegrationClient.getPostWithComments(postId);

            assertAll(
                () -> assertEquals(1, post.getComments().size()),
                () -> assertEquals("Line1\nLine2\nLine3", post.getComments().get(0).getBody())
            );
        }

        @Test
        @DisplayName("Returns post with multiple comments correctly mapped")
        void handlesMultipleComments() {
            final String postId = "1";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 1, "title": "Test Title", "body": "Test Body"}
                """);
            stubPostCommentsResponse(postId, 200, """
                [
                  {"postId": 1, "id": 1, "name": "First", "email": "first@test.com", "body": "First body"},
                  {"postId": 1, "id": 2, "name": "Second", "email": "second@test.com", "body": "Second body"},
                  {"postId": 1, "id": 3, "name": "Third", "email": "third@test.com", "body": "Third body"},
                  {"postId": 1, "id": 4, "name": "Fourth", "email": "fourth@test.com", "body": "Fourth body"},
                  {"postId": 1, "id": 5, "name": "Fifth", "email": "fifth@test.com", "body": "Fifth body"}
                ]
                """);

            final AuditionPost post = auditionIntegrationClient.getPostWithComments(postId);

            assertAll(
                () -> assertEquals(5, post.getComments().size()),
                () -> assertEquals(1, post.getComments().get(0).getId()),
                () -> assertEquals(5, post.getComments().get(4).getId()),
                () -> assertEquals("Fifth", post.getComments().get(4).getName())
            );
        }

        @Test
        @DisplayName("Returns post with comment having null name field")
        void handlesCommentWithNullName() {
            final String postId = "1";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 1, "title": "Test Title", "body": "Test Body"}
                """);
            stubPostCommentsResponse(postId, 200, """
                [
                  {"postId": 1, "id": 1, "name": null, "email": "test@test.com", "body": "Body content"}
                ]
                """);

            final AuditionPost post = auditionIntegrationClient.getPostWithComments(postId);

            assertAll(
                () -> assertEquals(1, post.getComments().size()),
                () -> assertNull(post.getComments().get(0).getName()),
                () -> assertEquals("test@test.com", post.getComments().get(0).getEmail())
            );
        }

        @Test
        @DisplayName("Returns post with comment having empty body field")
        void handlesCommentWithEmptyBody() {
            final String postId = "1";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 1, "title": "Test Title", "body": "Test Body"}
                """);
            stubPostCommentsResponse(postId, 200, """
                [
                  {"postId": 1, "id": 1, "name": "Comment Name", "email": "test@test.com", "body": ""}
                ]
                """);

            final AuditionPost post = auditionIntegrationClient.getPostWithComments(postId);

            assertAll(
                () -> assertEquals(1, post.getComments().size()),
                () -> assertEquals("Comment Name", post.getComments().get(0).getName()),
                () -> assertEquals("", post.getComments().get(0).getBody())
            );
        }
    }

    @Nested
    @DisplayName("getCommentsForPost")
    class GetCommentsForPostTests {

        @Test
        @DisplayName("Returns comments when upstream responds with valid data")
        void returnsComments() {
            final String postId = "1";
            stubPostCommentsResponse(postId, 200, """
                [
                  {"postId": 1, "id": 1, "name": "Comment 1", "email": "user1@test.com", "body": "Comment body 1"},
                  {"postId": 1, "id": 2, "name": "Comment 2", "email": "user2@test.com", "body": "Comment body 2"}
                ]
                """);

            final List<Comment> comments = auditionIntegrationClient.getCommentsForPost(postId);

            assertAll(
                () -> assertEquals(2, comments.size()),
                () -> assertEquals("Comment 1", comments.get(0).getName()),
                () -> assertEquals("user2@test.com", comments.get(1).getEmail())
            );
        }

        @Test
        @DisplayName("Returns empty list and logs when no comments exist")
        void returnsEmptyListWhenNoComments() {
            final String postId = "1";
            stubPostCommentsResponse(postId, 200, "[]");

            final List<Comment> comments = auditionIntegrationClient.getCommentsForPost(postId);

            assertTrue(comments.isEmpty());
            verify(auditionLogger).debug(any(), eq("No comments found for post with id " + postId));
        }

        @Test
        @DisplayName("Throws SystemException with 404 when comments not found")
        void throwsNotFoundExceptionWhenCommentsNotFound() {
            final String postId = "999";
            stubPostCommentsResponse(postId, 404, "{}");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getCommentsForPost(postId));

            assertAll(
                () -> assertEquals("Cannot find comments for Post with id " + postId, exception.getMessage()),
                () -> assertEquals("Resource Not Found", exception.getTitle()),
                () -> assertEquals(404, exception.getStatusCode())
            );
        }

        @Test
        @DisplayName("Throws SystemException when upstream responds with 400 Bad Request")
        void throwsSystemExceptionForBadRequest() {
            final String postId = "invalid";
            stubPostCommentsResponse(postId, 400, "Bad Request");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getCommentsForPost(postId));

            assertEquals("Unknown Error message", exception.getMessage());
        }

        @Test
        @DisplayName("Throws SystemException when upstream responds with 401 Unauthorized")
        void throwsSystemExceptionForUnauthorized() {
            final String postId = "1";
            stubPostCommentsResponse(postId, 401, "Unauthorized");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getCommentsForPost(postId));

            assertEquals("Unknown Error message", exception.getMessage());
        }

        @Test
        @DisplayName("Throws SystemException when upstream responds with 403 Forbidden")
        void throwsSystemExceptionForForbidden() {
            final String postId = "1";
            stubPostCommentsResponse(postId, 403, "Forbidden");

            SystemException exception = assertThrows(SystemException.class,
                () -> auditionIntegrationClient.getCommentsForPost(postId));

            assertEquals("Unknown Error message", exception.getMessage());
        }

        @Test
        @DisplayName("Returns comments with special characters correctly")
        void handlesCommentsWithSpecialCharacters() {
            final String postId = "1";
            stubPostCommentsResponse(postId, 200, """
                [
                  {"postId": 1, "id": 1, "name": "Special <chars> & 'quotes'", "email": "test@émail.com", "body": "Body with unicode: éàü"}
                ]
                """);

            final List<Comment> comments = auditionIntegrationClient.getCommentsForPost(postId);

            assertAll(
                () -> assertEquals(1, comments.size()),
                () -> assertEquals("Special <chars> & 'quotes'", comments.get(0).getName()),
                () -> assertEquals("test@émail.com", comments.get(0).getEmail()),
                () -> assertEquals("Body with unicode: éàü", comments.get(0).getBody())
            );
        }

        @Test
        @DisplayName("Returns comments with newlines in body correctly")
        void handlesCommentsWithNewlines() {
            final String postId = "1";
            stubPostCommentsResponse(postId, 200, """
                [
                  {"postId": 1, "id": 1, "name": "Comment", "email": "test@test.com", "body": "Line1\\nLine2\\nLine3"}
                ]
                """);

            final List<Comment> comments = auditionIntegrationClient.getCommentsForPost(postId);

            assertAll(
                () -> assertEquals(1, comments.size()),
                () -> assertEquals("Line1\nLine2\nLine3", comments.get(0).getBody())
            );
        }

        @Test
        @DisplayName("Returns multiple comments in correct order")
        void handlesMultipleComments() {
            final String postId = "1";
            stubPostCommentsResponse(postId, 200, """
                [
                  {"postId": 1, "id": 1, "name": "First", "email": "first@test.com", "body": "First body"},
                  {"postId": 1, "id": 2, "name": "Second", "email": "second@test.com", "body": "Second body"},
                  {"postId": 1, "id": 3, "name": "Third", "email": "third@test.com", "body": "Third body"},
                  {"postId": 1, "id": 4, "name": "Fourth", "email": "fourth@test.com", "body": "Fourth body"},
                  {"postId": 1, "id": 5, "name": "Fifth", "email": "fifth@test.com", "body": "Fifth body"}
                ]
                """);

            final List<Comment> comments = auditionIntegrationClient.getCommentsForPost(postId);

            assertAll(
                () -> assertEquals(5, comments.size()),
                () -> assertEquals(1, comments.get(0).getId()),
                () -> assertEquals(5, comments.get(4).getId()),
                () -> assertEquals("Fifth", comments.get(4).getName())
            );
        }

        @Test
        @DisplayName("Returns comment with null name field")
        void handlesCommentWithNullName() {
            final String postId = "1";
            stubPostCommentsResponse(postId, 200, """
                [
                  {"postId": 1, "id": 1, "name": null, "email": "test@test.com", "body": "Body content"}
                ]
                """);

            final List<Comment> comments = auditionIntegrationClient.getCommentsForPost(postId);

            assertAll(
                () -> assertEquals(1, comments.size()),
                () -> assertNull(comments.get(0).getName()),
                () -> assertEquals("test@test.com", comments.get(0).getEmail())
            );
        }

        @Test
        @DisplayName("Returns comment with empty body field")
        void handlesCommentWithEmptyBody() {
            final String postId = "1";
            stubPostCommentsResponse(postId, 200, """
                [
                  {"postId": 1, "id": 1, "name": "Comment Name", "email": "test@test.com", "body": ""}
                ]
                """);

            final List<Comment> comments = auditionIntegrationClient.getCommentsForPost(postId);

            assertAll(
                () -> assertEquals(1, comments.size()),
                () -> assertEquals("Comment Name", comments.get(0).getName()),
                () -> assertEquals("", comments.get(0).getBody())
            );
        }
    }

    private void stubPostsResponse(final int status, final String responseBody) {
        wireMockServer.stubFor(get(urlEqualTo("/posts"))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody)));
    }

    private void stubPostByIdResponse(final String postId, final int status, final String responseBody) {
        wireMockServer.stubFor(get(urlEqualTo("/posts/" + postId))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody)));
    }

    private void stubPostCommentsResponse(final String postId, final int status, final String responseBody) {
        wireMockServer.stubFor(get(urlEqualTo("/posts/" + postId + "/comments"))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody)));
    }

    @Nested
    @DisplayName("Cache Behavior Tests")
    class CacheBehaviorTests {

        @BeforeEach
        void setUpCacheTests() {
            clearCaches();
        }

        @Test
        @DisplayName("getPosts should cache results and avoid duplicate API calls")
        void testGetPostsCaching() {
            // Given
            stubPostsResponse(200, """
                [
                  {"userId": 1, "id": 1, "title": "Cached Post", "body": "Test body"}
                ]
                """);

            // When - First call
            final List<AuditionPost> firstResult = auditionIntegrationClient.getPosts();

            // When - Second call (should hit cache)
            final List<AuditionPost> secondResult = auditionIntegrationClient.getPosts();

            // Then
            assertEquals(1, firstResult.size());
            assertEquals(1, secondResult.size());
            assertEquals("Cached Post", firstResult.get(0).getTitle());
            assertEquals("Cached Post", secondResult.get(0).getTitle());

            // Verify WireMock was called only once
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts")));
        }

        @Test
        @DisplayName("getPostById should cache results per post ID")
        void testGetPostByIdCaching() {
            // Given
            final String postId = "123";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 123, "title": "Cached Post", "body": "Test body"}
                """);

            // When - First call
            final AuditionPost firstResult = auditionIntegrationClient.getPostById(postId);

            // When - Second call (should hit cache)
            final AuditionPost secondResult = auditionIntegrationClient.getPostById(postId);

            // Then
            assertEquals(123, firstResult.getId());
            assertEquals(123, secondResult.getId());
            assertEquals("Cached Post", firstResult.getTitle());
            assertEquals("Cached Post", secondResult.getTitle());

            // Verify WireMock was called only once
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts/" + postId)));
        }

        @Test
        @DisplayName("getCommentsForPost should cache results per post ID")
        void testGetCommentsForPostCaching() {
            // Given
            final String postId = "123";
            stubPostCommentsResponse(postId, 200, """
                [
                  {"id": 1, "postId": 123, "name": "Cached Comment", "email": "test@example.com", "body": "Comment body"}
                ]
                """);

            // When - First call
            final List<Comment> firstResult = auditionIntegrationClient.getCommentsForPost(postId);

            // When - Second call (should hit cache)
            final List<Comment> secondResult = auditionIntegrationClient.getCommentsForPost(postId);

            // Then
            assertEquals(1, firstResult.size());
            assertEquals(1, secondResult.size());
            assertEquals("Cached Comment", firstResult.get(0).getName());
            assertEquals("Cached Comment", secondResult.get(0).getName());

            // Verify WireMock was called only once
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts/" + postId + "/comments")));
        }

        @Test
        @DisplayName("getPostWithComments should cache complete result and avoid duplicate external calls")
        void testGetPostWithCommentsCaching() {
            // Given
            final String postId = "123";
            stubPostByIdResponse(postId, 200, """
                {"userId": 1, "id": 123, "title": "Test Post", "body": "Test body"}
                """);
            stubPostCommentsResponse(postId, 200, """
                [
                  {"id": 1, "postId": 123, "name": "Test Comment", "email": "test@example.com", "body": "Comment body"}
                ]
                """);

            // When - First call
            final AuditionPost firstResult = auditionIntegrationClient.getPostWithComments(postId);

            // When - Second call (should hit cache for the complete method)
            final AuditionPost secondResult = auditionIntegrationClient.getPostWithComments(postId);

            // Then
            assertEquals(123, firstResult.getId());
            assertEquals(123, secondResult.getId());
            assertEquals("Test Post", firstResult.getTitle());
            assertEquals("Test Post", secondResult.getTitle());
            assertEquals(1, firstResult.getComments().size());
            assertEquals(1, secondResult.getComments().size());
            assertEquals("Test Comment", firstResult.getComments().get(0).getName());
            assertEquals("Test Comment", secondResult.getComments().get(0).getName());

            // Verify WireMock calls - should be called once each for the first invocation only
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts/" + postId)));
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts/" + postId + "/comments")));
        }

        @Test
        @DisplayName("Different post IDs should have separate cache entries")
        void testSeparateCacheEntriesForDifferentPostIds() {
            // Given
            final String postId1 = "1";
            final String postId2 = "2";

            stubPostByIdResponse(postId1, 200, """
                {"userId": 1, "id": 1, "title": "Post 1", "body": "Body 1"}
                """);
            stubPostByIdResponse(postId2, 200, """
                {"userId": 1, "id": 2, "title": "Post 2", "body": "Body 2"}
                """);

            // When
            final AuditionPost result1 = auditionIntegrationClient.getPostById(postId1);
            final AuditionPost result2 = auditionIntegrationClient.getPostById(postId2);

            // Call again to test caching
            final AuditionPost cachedResult1 = auditionIntegrationClient.getPostById(postId1);
            final AuditionPost cachedResult2 = auditionIntegrationClient.getPostById(postId2);

            // Then
            assertEquals("Post 1", result1.getTitle());
            assertEquals("Post 2", result2.getTitle());
            assertEquals("Post 1", cachedResult1.getTitle());
            assertEquals("Post 2", cachedResult2.getTitle());

            // Verify each endpoint was called only once
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts/" + postId1)));
            wireMockServer.verify(1, getRequestedFor(urlEqualTo("/posts/" + postId2)));
        }
    }
}

