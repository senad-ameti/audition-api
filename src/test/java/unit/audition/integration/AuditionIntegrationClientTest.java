package unit.audition.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionPost;
import com.audition.model.Comment;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditionIntegrationClient Unit Tests")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AuditionIntegrationClientTest {

    private static final String POSTS_URL = "https://jsonplaceholder.typicode.com/posts";
    private static final String POSTS_BY_ID_URL = "https://jsonplaceholder.typicode.com/posts/{id}";
    private static final String POST_COMMENTS_URL = "https://jsonplaceholder.typicode.com/posts/{postId}/comments";

    @Mock
    private transient RestTemplate restTemplate;

    @Mock
    private transient AuditionLogger auditionLogger;

    private transient AuditionIntegrationClient client;

    @BeforeEach
    void setUp() {
        client = new AuditionIntegrationClient(restTemplate, auditionLogger, POSTS_URL, POSTS_BY_ID_URL,
            POST_COMMENTS_URL);
    }

    @Nested
    @DisplayName("getPosts")
    class GetPostsTests {

        @Test
        @DisplayName("Returns posts when upstream provides data")
        void returnsPosts() {
            final AuditionPost first = buildPost(1, 11, "test-title-1", "body-1");
            final AuditionPost second = buildPost(2, 12, "test-title-2", "body-2");
            when(restTemplate.getForObject(POSTS_URL, AuditionPost[].class)).thenReturn(
                new AuditionPost[]{first, second});

            final List<AuditionPost> result = client.getPosts();

            assertEquals(2, result.size());
            assertEquals(11, result.get(0).getUserId());
            assertEquals("test-title-2", result.get(1).getTitle());
            verify(restTemplate).getForObject(POSTS_URL, AuditionPost[].class);
            verify(auditionLogger, never()).debug(any(Logger.class), anyString());
        }

        @Test
        @DisplayName("Returns empty list and logs when upstream response is empty")
        void handlesEmptyResponse() {
            when(restTemplate.getForObject(POSTS_URL, AuditionPost[].class)).thenReturn(null);

            final List<AuditionPost> result = client.getPosts();

            assertTrue(result.isEmpty());
            verify(auditionLogger).debug(any(Logger.class), eq("No posts found from external API"));
        }

        @Test
        @DisplayName("Wraps HttpClientErrorException into SystemException preserving error details")
        void handlesHttpClientError() {
            final HttpClientErrorException originalException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                "Bad Request");
            when(restTemplate.getForObject(POSTS_URL, AuditionPost[].class))
                .thenThrow(originalException);

            final SystemException exception = assertThrows(SystemException.class, () -> client.getPosts());

            assertTrue(exception.getMessage().contains("400"));
            assertEquals("External API Error", exception.getTitle());
            assertEquals(400, exception.getStatusCode());
            assertEquals(originalException, exception.getCause());
            verify(auditionLogger, never()).debug(any(Logger.class), anyString());
        }
    }

    @Nested
    @DisplayName("getPostById")
    class GetPostByIdTests {

        @Test
        @DisplayName("Returns post when upstream provides data for given id")
        void returnsPost() {
            final String postId = "1";
            final AuditionPost expectedPost = buildPost(1, 10, "test-title", "test-body");
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(expectedPost);

            final AuditionPost result = client.getPostById(postId);

            assertEquals(1, result.getId());
            assertEquals(10, result.getUserId());
            assertEquals("test-title", result.getTitle());
            assertEquals("test-body", result.getBody());
            verify(restTemplate).getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId);
        }

        @Test
        @DisplayName("Returns null when upstream returns null response")
        void returnsNullWhenUpstreamReturnsNull() {
            final String postId = "99";
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(null);

            final AuditionPost result = client.getPostById(postId);

            assertNull(result);
            verify(restTemplate).getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId);
        }

        @Test
        @DisplayName("Throws SystemException with 404 status when post is not found")
        void throwsNotFoundExceptionWhenPostNotFound() {
            final String postId = "999";
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

            final SystemException exception = assertThrows(SystemException.class, () -> client.getPostById(postId));

            assertEquals("Cannot find a Post with id " + postId, exception.getMessage());
            assertEquals("Resource Not Found", exception.getTitle());
            assertEquals(404, exception.getStatusCode());
        }

        @Test
        @DisplayName("Throws SystemException preserving error details for non-404 client errors")
        void throwsSystemExceptionForOtherClientErrors() {
            final String postId = "1";
            final HttpClientErrorException originalException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                "Bad Request");
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId))
                .thenThrow(originalException);

            final SystemException exception = assertThrows(SystemException.class, () -> client.getPostById(postId));

            assertTrue(exception.getMessage().contains("400"));
            assertEquals("External API Error", exception.getTitle());
            assertEquals(400, exception.getStatusCode());
            assertEquals(originalException, exception.getCause());
        }

        @Test
        @DisplayName("Throws SystemException preserving error details for forbidden access error")
        void throwsSystemExceptionForForbiddenError() {
            final String postId = "1";
            final HttpClientErrorException originalException = new HttpClientErrorException(HttpStatus.FORBIDDEN,
                "Forbidden");
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId))
                .thenThrow(originalException);

            final SystemException exception = assertThrows(SystemException.class, () -> client.getPostById(postId));

            assertTrue(exception.getMessage().contains("403"));
            assertEquals("External API Error", exception.getTitle());
            assertEquals(403, exception.getStatusCode());
            assertEquals(originalException, exception.getCause());
        }

        @Test
        @DisplayName("Throws SystemException preserving error details for unauthorized access error")
        void throwsSystemExceptionForUnauthorizedError() {
            final String postId = "1";
            final HttpClientErrorException originalException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED,
                "Unauthorized");
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId))
                .thenThrow(originalException);

            final SystemException exception = assertThrows(SystemException.class, () -> client.getPostById(postId));

            assertTrue(exception.getMessage().contains("401"));
            assertEquals("External API Error", exception.getTitle());
            assertEquals(401, exception.getStatusCode());
            assertEquals(originalException, exception.getCause());
        }

        @Test
        @DisplayName("Handles post with all fields populated correctly")
        void handlesCompletePostData() {
            final String postId = "5";
            final AuditionPost expectedPost = buildPost(5, 100, "Complete Title with Special Characters !@#$%",
                "Long body content\nwith newlines\nand special chars: éàü");
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(expectedPost);

            final AuditionPost result = client.getPostById(postId);

            assertEquals(5, result.getId());
            assertEquals(100, result.getUserId());
            assertEquals("Complete Title with Special Characters !@#$%", result.getTitle());
            assertEquals("Long body content\nwith newlines\nand special chars: éàü", result.getBody());
        }
    }

    @Nested
    @DisplayName("getPostWithComments")
    class GetPostWithCommentsTests {

        @Test
        @DisplayName("Returns post with comments when both post and comments exist")
        void returnsPostWithComments() {
            final String postId = "1";
            final AuditionPost expectedPost = buildPost(1, 10, "test-title", "test-body");
            final Comment comment1 = buildComment(1, 1, "comment-name-1", "email1@test.com", "comment-body-1");
            final Comment comment2 = buildComment(1, 2, "comment-name-2", "email2@test.com", "comment-body-2");

            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(expectedPost);
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenReturn(new Comment[]{comment1, comment2});

            final AuditionPost result = client.getPostWithComments(postId);

            assertEquals(1, result.getId());
            assertEquals("test-title", result.getTitle());
            assertEquals(2, result.getComments().size());
            assertEquals("comment-name-1", result.getComments().get(0).getName());
            assertEquals("email2@test.com", result.getComments().get(1).getEmail());
        }

        @Test
        @DisplayName("Returns post with empty comments list when no comments exist")
        void returnsPostWithEmptyComments() {
            final String postId = "1";
            final AuditionPost expectedPost = buildPost(1, 10, "test-title", "test-body");

            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(expectedPost);
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenReturn(new Comment[0]);

            final AuditionPost result = client.getPostWithComments(postId);

            assertEquals(1, result.getId());
            assertTrue(result.getComments().isEmpty());
            verify(auditionLogger).debug(any(Logger.class), eq("No comments found for post with id " + postId));
        }

        @Test
        @DisplayName("Returns post with empty comments list when comments response is null")
        void returnsPostWithEmptyCommentsWhenNull() {
            final String postId = "1";
            final AuditionPost expectedPost = buildPost(1, 10, "test-title", "test-body");

            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(expectedPost);
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId)).thenReturn(null);

            final AuditionPost result = client.getPostWithComments(postId);

            assertEquals(1, result.getId());
            assertTrue(result.getComments().isEmpty());
            verify(auditionLogger).debug(any(Logger.class), eq("No comments found for post with id " + postId));
        }

        @Test
        @DisplayName("Throws SystemException with 404 status when post is not found")
        void throwsNotFoundExceptionWhenPostNotFound() {
            final String postId = "999";
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

            final SystemException exception = assertThrows(SystemException.class,
                () -> client.getPostWithComments(postId));

            assertEquals("Cannot find a Post with id " + postId, exception.getMessage());
            assertEquals("Resource Not Found", exception.getTitle());
            assertEquals(404, exception.getStatusCode());
        }

        @Test
        @DisplayName("Throws SystemException with 404 status when comments endpoint returns not found")
        void throwsNotFoundExceptionWhenCommentsNotFound() {
            final String postId = "1";
            final AuditionPost expectedPost = buildPost(1, 10, "test-title", "test-body");

            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(expectedPost);
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

            final SystemException exception = assertThrows(SystemException.class,
                () -> client.getPostWithComments(postId));

            assertEquals("Cannot find comments for Post with id " + postId, exception.getMessage());
            assertEquals("Resource Not Found", exception.getTitle());
            assertEquals(404, exception.getStatusCode());
        }

        @Test
        @DisplayName("Throws SystemException preserving error details for bad request error when fetching comments")
        void throwsSystemExceptionForBadRequestOnComments() {
            final String postId = "1";
            final AuditionPost expectedPost = buildPost(1, 10, "test-title", "test-body");
            final HttpClientErrorException originalException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                "Bad Request");

            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(expectedPost);
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenThrow(originalException);

            final SystemException exception = assertThrows(SystemException.class,
                () -> client.getPostWithComments(postId));

            assertTrue(exception.getMessage().contains("400"));
            assertEquals("External API Error", exception.getTitle());
            assertEquals(400, exception.getStatusCode());
            assertEquals(originalException, exception.getCause());
        }

        @Test
        @DisplayName("Throws SystemException preserving error details for unauthorized error when fetching post")
        void throwsSystemExceptionForUnauthorizedOnPost() {
            final String postId = "1";
            final HttpClientErrorException originalException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED,
                "Unauthorized");
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId))
                .thenThrow(originalException);

            final SystemException exception = assertThrows(SystemException.class,
                () -> client.getPostWithComments(postId));

            assertTrue(exception.getMessage().contains("401"));
            assertEquals("External API Error", exception.getTitle());
            assertEquals(401, exception.getStatusCode());
            assertEquals(originalException, exception.getCause());
        }

        @Test
        @DisplayName("Throws SystemException preserving error details for forbidden error when fetching comments")
        void throwsSystemExceptionForForbiddenOnComments() {
            final String postId = "1";
            final AuditionPost expectedPost = buildPost(1, 10, "test-title", "test-body");
            final HttpClientErrorException originalException = new HttpClientErrorException(HttpStatus.FORBIDDEN,
                "Forbidden");

            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(expectedPost);
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenThrow(originalException);

            final SystemException exception = assertThrows(SystemException.class,
                () -> client.getPostWithComments(postId));

            assertTrue(exception.getMessage().contains("403"));
            assertEquals("External API Error", exception.getTitle());
            assertEquals(403, exception.getStatusCode());
            assertEquals(originalException, exception.getCause());
        }

        @Test
        @DisplayName("Returns post with comments containing special characters")
        void handlesCommentsWithSpecialCharacters() {
            final String postId = "1";
            final AuditionPost expectedPost = buildPost(1, 10, "test-title", "test-body");
            final Comment comment = buildComment(1, 1, "Special <name> & 'quotes'",
                "test@émail.com", "Body with unicode: éàü\nand newlines");

            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(expectedPost);
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenReturn(new Comment[]{comment});

            final AuditionPost result = client.getPostWithComments(postId);

            assertEquals(1, result.getComments().size());
            assertEquals("Special <name> & 'quotes'", result.getComments().get(0).getName());
            assertEquals("test@émail.com", result.getComments().get(0).getEmail());
            assertEquals("Body with unicode: éàü\nand newlines", result.getComments().get(0).getBody());
        }

        @Test
        @DisplayName("Returns post with multiple comments correctly ordered")
        void handlesMultipleCommentsInOrder() {
            final String postId = "1";
            final AuditionPost expectedPost = buildPost(1, 10, "test-title", "test-body");
            final Comment comment1 = buildComment(1, 1, "First", "first@test.com", "First body");
            final Comment comment2 = buildComment(1, 2, "Second", "second@test.com", "Second body");
            final Comment comment3 = buildComment(1, 3, "Third", "third@test.com", "Third body");

            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(expectedPost);
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenReturn(new Comment[]{comment1, comment2, comment3});

            final AuditionPost result = client.getPostWithComments(postId);

            assertEquals(3, result.getComments().size());
            assertEquals(1, result.getComments().get(0).getId());
            assertEquals(2, result.getComments().get(1).getId());
            assertEquals(3, result.getComments().get(2).getId());
        }
    }

    @Nested
    @DisplayName("getCommentsForPost")
    class GetCommentsForPostTests {

        @Test
        @DisplayName("Returns comments when upstream provides data for given post id")
        void returnsComments() {
            final String postId = "1";
            final Comment comment1 = buildComment(1, 1, "comment-name-1", "email1@test.com", "comment-body-1");
            final Comment comment2 = buildComment(1, 2, "comment-name-2", "email2@test.com", "comment-body-2");

            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenReturn(new Comment[]{comment1, comment2});

            final List<Comment> result = client.getCommentsForPost(postId);

            assertEquals(2, result.size());
            assertEquals("comment-name-1", result.get(0).getName());
            assertEquals("email2@test.com", result.get(1).getEmail());
            verify(restTemplate).getForObject(POST_COMMENTS_URL, Comment[].class, postId);
        }

        @Test
        @DisplayName("Returns empty list and logs when no comments exist")
        void returnsEmptyListWhenNoComments() {
            final String postId = "1";
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenReturn(new Comment[0]);

            final List<Comment> result = client.getCommentsForPost(postId);

            assertTrue(result.isEmpty());
            verify(auditionLogger).debug(any(Logger.class), eq("No comments found for post with id " + postId));
        }

        @Test
        @DisplayName("Returns empty list and logs when upstream returns null")
        void returnsEmptyListWhenNull() {
            final String postId = "1";
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId)).thenReturn(null);

            final List<Comment> result = client.getCommentsForPost(postId);

            assertTrue(result.isEmpty());
            verify(auditionLogger).debug(any(Logger.class), eq("No comments found for post with id " + postId));
        }

        @Test
        @DisplayName("Throws SystemException with 404 status when comments not found")
        void throwsNotFoundExceptionWhenCommentsNotFound() {
            final String postId = "999";
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

            final SystemException exception = assertThrows(SystemException.class,
                () -> client.getCommentsForPost(postId));

            assertEquals("Cannot find comments for Post with id " + postId, exception.getMessage());
            assertEquals("Resource Not Found", exception.getTitle());
            assertEquals(404, exception.getStatusCode());
        }

        @Test
        @DisplayName("Throws SystemException preserving error details for bad request error")
        void throwsSystemExceptionForBadRequest() {
            final String postId = "1";
            final HttpClientErrorException originalException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                "Bad Request");
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenThrow(originalException);

            final SystemException exception = assertThrows(SystemException.class,
                () -> client.getCommentsForPost(postId));

            assertTrue(exception.getMessage().contains("400"));
            assertEquals("External API Error", exception.getTitle());
            assertEquals(400, exception.getStatusCode());
            assertEquals(originalException, exception.getCause());
        }

        @Test
        @DisplayName("Throws SystemException preserving error details for unauthorized error")
        void throwsSystemExceptionForUnauthorized() {
            final String postId = "1";
            final HttpClientErrorException originalException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED,
                "Unauthorized");
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenThrow(originalException);

            final SystemException exception = assertThrows(SystemException.class,
                () -> client.getCommentsForPost(postId));

            assertTrue(exception.getMessage().contains("401"));
            assertEquals("External API Error", exception.getTitle());
            assertEquals(401, exception.getStatusCode());
            assertEquals(originalException, exception.getCause());
        }

        @Test
        @DisplayName("Throws SystemException preserving error details for forbidden error")
        void throwsSystemExceptionForForbidden() {
            final String postId = "1";
            final HttpClientErrorException originalException = new HttpClientErrorException(HttpStatus.FORBIDDEN,
                "Forbidden");
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenThrow(originalException);

            final SystemException exception = assertThrows(SystemException.class,
                () -> client.getCommentsForPost(postId));

            assertTrue(exception.getMessage().contains("403"));
            assertEquals("External API Error", exception.getTitle());
            assertEquals(403, exception.getStatusCode());
            assertEquals(originalException, exception.getCause());
        }

        @Test
        @DisplayName("Returns comments with special characters correctly")
        void handlesCommentsWithSpecialCharacters() {
            final String postId = "1";
            final Comment comment = buildComment(1, 1, "Special <name> & 'quotes'",
                "test@émail.com", "Body with unicode: éàü\nand newlines");

            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenReturn(new Comment[]{comment});

            final List<Comment> result = client.getCommentsForPost(postId);

            assertEquals(1, result.size());
            assertEquals("Special <name> & 'quotes'", result.get(0).getName());
            assertEquals("test@émail.com", result.get(0).getEmail());
            assertEquals("Body with unicode: éàü\nand newlines", result.get(0).getBody());
        }

        @Test
        @DisplayName("Returns multiple comments in correct order")
        void handlesMultipleCommentsInOrder() {
            final String postId = "1";
            final Comment comment1 = buildComment(1, 1, "First", "first@test.com", "First body");
            final Comment comment2 = buildComment(1, 2, "Second", "second@test.com", "Second body");
            final Comment comment3 = buildComment(1, 3, "Third", "third@test.com", "Third body");

            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenReturn(new Comment[]{comment1, comment2, comment3});

            final List<Comment> result = client.getCommentsForPost(postId);

            assertEquals(3, result.size());
            assertEquals(1, result.get(0).getId());
            assertEquals(2, result.get(1).getId());
            assertEquals(3, result.get(2).getId());
        }

        @Test
        @DisplayName("Throws SystemException preserving error details for internal server error")
        void throwsSystemExceptionForServerError() {
            final String postId = "1";
            final HttpClientErrorException originalException = new HttpClientErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Server Error");
            when(restTemplate.getForObject(POST_COMMENTS_URL, Comment[].class, postId))
                .thenThrow(originalException);

            final SystemException exception = assertThrows(SystemException.class,
                () -> client.getCommentsForPost(postId));

            assertTrue(exception.getMessage().contains("500"));
            assertEquals("External API Error", exception.getTitle());
            assertEquals(500, exception.getStatusCode());
            assertEquals(originalException, exception.getCause());
        }
    }

    private AuditionPost buildPost(final int id, final int userId, final String title, final String body) {
        final AuditionPost post = new AuditionPost();
        post.setId(id);
        post.setUserId(userId);
        post.setTitle(title);
        post.setBody(body);
        return post;
    }

    private Comment buildComment(final int postId, final int id, final String name,
        final String email, final String body) {
        final Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setId(id);
        comment.setName(name);
        comment.setEmail(email);
        comment.setBody(body);
        return comment;
    }
}

