package unit.com.audition.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.model.AuditionPost;
import com.audition.service.AuditionService;
import com.audition.web.AuditionController;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditionController Unit Tests")
class AuditionControllerTest {

    @Mock
    private AuditionService auditionService;

    @InjectMocks
    private AuditionController auditionController;

    @BeforeEach
    void setUp() {
        // Mockito will inject mocks into controller
    }

    @Nested
    @DisplayName("GET /posts")
    class GetPosts {

        @Test
        @DisplayName("getPosts returns posts provided by the service when no filters applied")
        void getPostsReturnsPostsFromService() {
            AuditionPost first = new AuditionPost();
            first.setId(1);
            first.setUserId(10);
            first.setTitle("t1");
            first.setBody("b1");

            AuditionPost second = new AuditionPost();
            second.setId(2);
            second.setUserId(20);
            second.setTitle("t2");
            second.setBody("b2");

            when(auditionService.getPosts(null, null)).thenReturn(Arrays.asList(first, second));

            List<AuditionPost> result = auditionController.getPosts(null, null);

            assertEquals(2, result.size(), "Controller should return the posts provided by the service");
            assertEquals(10, result.get(0).getUserId());
            assertEquals("t2", result.get(1).getTitle());
        }

        @Test
        @DisplayName("getPosts returns empty list when service returns none")
        void getPostsReturnsEmptyWhenServiceEmpty() {
            when(auditionService.getPosts(null, null)).thenReturn(Collections.emptyList());

            List<AuditionPost> result = auditionController.getPosts(null, null);

            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("getPosts passes userId filter to service")
        void getPostsPassesUserIdFilterToService() {
            AuditionPost post = new AuditionPost();
            post.setId(1);
            post.setUserId(5);
            post.setTitle("User 5 Post");
            post.setBody("body");

            when(auditionService.getPosts(eq(5), eq(null))).thenReturn(Collections.singletonList(post));

            List<AuditionPost> result = auditionController.getPosts(5, null);

            assertEquals(1, result.size());
            assertEquals(5, result.get(0).getUserId());
            verify(auditionService).getPosts(eq(5), eq(null));
        }

        @Test
        @DisplayName("getPosts passes id filter to service")
        void getPostsPassesIdFilterToService() {
            AuditionPost post = new AuditionPost();
            post.setId(42);
            post.setUserId(1);
            post.setTitle("Post 42");
            post.setBody("body");

            when(auditionService.getPosts(eq(null), eq(42))).thenReturn(Collections.singletonList(post));

            List<AuditionPost> result = auditionController.getPosts(null, 42);

            assertEquals(1, result.size());
            assertEquals(42, result.get(0).getId());
            verify(auditionService).getPosts(eq(null), eq(42));
        }

        @Test
        @DisplayName("getPosts passes both userId and id filters to service")
        void getPostsPassesBothFiltersToService() {
            AuditionPost post = new AuditionPost();
            post.setId(10);
            post.setUserId(3);
            post.setTitle("Post 10");
            post.setBody("body");

            when(auditionService.getPosts(eq(3), eq(10))).thenReturn(Collections.singletonList(post));

            List<AuditionPost> result = auditionController.getPosts(3, 10);

            assertEquals(1, result.size());
            assertEquals(3, result.get(0).getUserId());
            assertEquals(10, result.get(0).getId());
            verify(auditionService).getPosts(eq(3), eq(10));
        }

        @Test
        @DisplayName("getPosts returns empty list when filters match nothing")
        void getPostsReturnsEmptyWhenFiltersMatchNothing() {
            when(auditionService.getPosts(eq(999), eq(888))).thenReturn(Collections.emptyList());

            List<AuditionPost> result = auditionController.getPosts(999, 888);

            assertEquals(0, result.size());
            verify(auditionService).getPosts(eq(999), eq(888));
        }
    }

    @Nested
    @DisplayName("GET /posts/{id}")
    class GetPostById {

        @Test
        @DisplayName("getPosts by id returns post from service")
        void getPostByIdReturnsPost() {
            final String postId = "1";
            AuditionPost expected = new AuditionPost();
            expected.setId(1);
            expected.setUserId(5);
            expected.setTitle("title");
            expected.setBody("body");

            when(auditionService.getPostById(postId)).thenReturn(expected);

            AuditionPost result = auditionController.getPosts(postId);

            assertEquals(1, result.getId());
            assertEquals(5, result.getUserId());
            assertEquals("title", result.getTitle());
        }

        @Test
        @DisplayName("getPosts by id returns null when service returns null")
        void getPostByIdReturnsNullWhenMissing() {
            final String postId = "999";
            when(auditionService.getPostById(postId)).thenReturn(null);

            AuditionPost result = auditionController.getPosts(postId);

            assertNull(result, "Controller should return null when service returns null");
        }
    }
}
