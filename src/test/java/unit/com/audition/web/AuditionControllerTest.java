package unit.com.audition.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.audition.model.AuditionPost;
import com.audition.service.AuditionService;
import com.audition.web.AuditionController;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @Test
    @DisplayName("getPosts returns posts provided by the service")
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

        when(auditionService.getPosts()).thenReturn(Arrays.asList(first, second));

        List<AuditionPost> result = auditionController.getPosts();

        assertEquals(2, result.size(), "Controller should return the posts provided by the service");
        assertEquals(10, result.get(0).getUserId());
        assertEquals("t2", result.get(1).getTitle());
    }

    @Test
    @DisplayName("getPosts returns empty list when service returns none")
    void getPostsReturnsEmptyWhenServiceEmpty() {
        when(auditionService.getPosts()).thenReturn(Collections.emptyList());

        List<AuditionPost> result = auditionController.getPosts();

        assertEquals(0, result.size());
    }

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
