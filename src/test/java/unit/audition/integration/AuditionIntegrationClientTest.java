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
class AuditionIntegrationClientTest {

    private static final String POSTS_URL = "https://jsonplaceholder.typicode.com/posts";
    private static final String POSTS_BY_ID_URL = "https://jsonplaceholder.typicode.com/posts/{id}";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuditionLogger auditionLogger;

    private AuditionIntegrationClient client;

    @BeforeEach
    void setUp() {
        client = new AuditionIntegrationClient(restTemplate, auditionLogger, POSTS_URL, POSTS_BY_ID_URL);
    }

    @Nested
    @DisplayName("getPosts")
    class GetPostsTests {

        @Test
        @DisplayName("Returns posts when upstream provides data")
        void returnsPosts() {
            AuditionPost first = buildPost(1, 11, "test-title-1", "body-1");
            AuditionPost second = buildPost(2, 12, "test-title-2", "body-2");
            when(restTemplate.getForObject(POSTS_URL, AuditionPost[].class)).thenReturn(
                new AuditionPost[]{first, second});

            List<AuditionPost> result = client.getPosts();

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

            List<AuditionPost> result = client.getPosts();

            assertTrue(result.isEmpty());
            verify(auditionLogger).debug(any(Logger.class), eq("No posts found from external API"));
        }

        @Test
        @DisplayName("Wraps HttpClientErrorException into SystemException")
        void handlesHttpClientError() {
            when(restTemplate.getForObject(POSTS_URL, AuditionPost[].class))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

            SystemException exception = assertThrows(SystemException.class, () -> client.getPosts());

            assertEquals("Unknown Error message", exception.getMessage());
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
            AuditionPost expectedPost = buildPost(1, 10, "test-title", "test-body");
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(expectedPost);

            AuditionPost result = client.getPostById(postId);

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

            AuditionPost result = client.getPostById(postId);

            assertNull(result);
            verify(restTemplate).getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId);
        }

        @Test
        @DisplayName("Throws SystemException with 404 status when post is not found")
        void throwsNotFoundExceptionWhenPostNotFound() {
            final String postId = "999";
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

            SystemException exception = assertThrows(SystemException.class, () -> client.getPostById(postId));

            assertEquals("Cannot find a Post with id " + postId, exception.getMessage());
            assertEquals("Resource Not Found", exception.getTitle());
            assertEquals(404, exception.getStatusCode());
        }

        @Test
        @DisplayName("Throws SystemException with unknown error message for non-404 client errors")
        void throwsSystemExceptionForOtherClientErrors() {
            final String postId = "1";
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

            SystemException exception = assertThrows(SystemException.class, () -> client.getPostById(postId));

            assertEquals("Unknown Error message", exception.getMessage());
        }

        @Test
        @DisplayName("Throws SystemException for forbidden access error")
        void throwsSystemExceptionForForbiddenError() {
            final String postId = "1";
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN, "Forbidden"));

            SystemException exception = assertThrows(SystemException.class, () -> client.getPostById(postId));

            assertEquals("Unknown Error message", exception.getMessage());
        }

        @Test
        @DisplayName("Throws SystemException for unauthorized access error")
        void throwsSystemExceptionForUnauthorizedError() {
            final String postId = "1";
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

            SystemException exception = assertThrows(SystemException.class, () -> client.getPostById(postId));

            assertEquals("Unknown Error message", exception.getMessage());
        }

        @Test
        @DisplayName("Handles post with all fields populated correctly")
        void handlesCompletePostData() {
            final String postId = "5";
            AuditionPost expectedPost = buildPost(5, 100, "Complete Title with Special Characters !@#$%",
                "Long body content\nwith newlines\nand special chars: éàü");
            when(restTemplate.getForObject(POSTS_BY_ID_URL, AuditionPost.class, postId)).thenReturn(expectedPost);

            AuditionPost result = client.getPostById(postId);

            assertEquals(5, result.getId());
            assertEquals(100, result.getUserId());
            assertEquals("Complete Title with Special Characters !@#$%", result.getTitle());
            assertEquals("Long body content\nwith newlines\nand special chars: éàü", result.getBody());
        }
    }

    private AuditionPost buildPost(final int id, final int userId, final String title, final String body) {
        AuditionPost post = new AuditionPost();
        post.setId(id);
        post.setUserId(userId);
        post.setTitle(title);
        post.setBody(body);
        return post;
    }
}

