package unit.audition.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AuditionIntegrationClientTest {

    private static final String POSTS_URL = "https://jsonplaceholder.typicode.com/posts";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuditionLogger auditionLogger;

    @InjectMocks
    private AuditionIntegrationClient client;

    @Test
    @DisplayName("getPosts returns posts when upstream provides data")
    void getPostsReturnsPosts() {
        AuditionPost first = buildPost(1, 11, "test-title-1", "body-1");
        AuditionPost second = buildPost(2, 12, "test-title-2", "body-2");
        when(restTemplate.getForObject(POSTS_URL, AuditionPost[].class)).thenReturn(new AuditionPost[]{first, second});

        List<AuditionPost> result = client.getPosts();

        assertEquals(2, result.size());
        assertEquals(11, result.get(0).getUserId());
        assertEquals("test-title-2", result.get(1).getTitle());
        verify(restTemplate).getForObject(POSTS_URL, AuditionPost[].class);
        verify(auditionLogger, never()).debug(any(Logger.class), anyString());
    }

    @Test
    @DisplayName("getPosts returns empty list and logs when upstream response is empty")
    void getPostsHandlesEmptyResponse() {
        when(restTemplate.getForObject(POSTS_URL, AuditionPost[].class)).thenReturn(null);

        List<AuditionPost> result = client.getPosts();

        assertTrue(result.isEmpty());
        verify(auditionLogger).debug(any(Logger.class), eq("No posts found from external API"));
    }

    @Test
    @DisplayName("getPosts wraps HttpClientErrorException into SystemException")
    void getPostsHandlesHttpClientError() {
        when(restTemplate.getForObject(POSTS_URL, AuditionPost[].class))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

        SystemException exception = assertThrows(SystemException.class, () -> client.getPosts());

        assertEquals("Unknown Error message", exception.getMessage());
        verify(auditionLogger, never()).debug(any(Logger.class), anyString());
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