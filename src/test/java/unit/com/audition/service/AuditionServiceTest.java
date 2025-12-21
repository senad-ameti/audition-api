package unit.com.audition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.audition.common.logging.AuditionLogger;
import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionPost;
import com.audition.model.Comment;
import com.audition.service.AuditionService;
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
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditionService Unit Tests")
class AuditionServiceTest {

    @Mock
    private AuditionIntegrationClient auditionIntegrationClient;

    @Mock
    private AuditionLogger auditionLogger;

    @InjectMocks
    private AuditionService auditionService;

    private List<AuditionPost> mockPosts;

    @BeforeEach
    void setUp() {
        // Create test data with various userId and id combinations
        AuditionPost post1 = new AuditionPost();
        post1.setId(10);
        post1.setUserId(1);
        post1.setTitle("Post 10");
        post1.setBody("Body 10");

        AuditionPost post2 = new AuditionPost();
        post2.setId(20);
        post2.setUserId(1);
        post2.setTitle("Post 20");
        post2.setBody("Body 20");

        AuditionPost post3 = new AuditionPost();
        post3.setId(30);
        post3.setUserId(2);
        post3.setTitle("Post 30");
        post3.setBody("Body 30");

        AuditionPost post4 = new AuditionPost();
        post4.setId(40);
        post4.setUserId(3);
        post4.setTitle("Post 40");
        post4.setBody("Body 40");

        mockPosts = Arrays.asList(post1, post2, post3, post4);

        // Enable debug logging to cover the debug log in getPosts
        ((Logger) LoggerFactory.getLogger(AuditionService.class)).setLevel(Level.DEBUG);
    }

    @Nested
    @DisplayName("getPosts with filtering")
    class GetPostsWithFiltering {

        @Test
        @DisplayName("returns all posts when no filters are provided")
        void returnsAllPostsWithNoFilters() {
            when(auditionIntegrationClient.getPosts()).thenReturn(mockPosts);

            List<AuditionPost> result = auditionService.getPosts(null, null);

            assertEquals(4, result.size(), "Should return all posts when no filters applied");
            assertEquals(10, result.get(0).getId());
            assertEquals(20, result.get(1).getId());
            assertEquals(30, result.get(2).getId());
            assertEquals(40, result.get(3).getId());
        }

        @Test
        @DisplayName("filters posts by userId only")
        void filtersByUserIdOnly() {
            when(auditionIntegrationClient.getPosts()).thenReturn(mockPosts);

            List<AuditionPost> result = auditionService.getPosts(1, null);

            assertEquals(2, result.size(), "Should return only posts with userId=1");
            assertEquals(10, result.get(0).getId());
            assertEquals(20, result.get(1).getId());
            assertEquals(1, result.get(0).getUserId());
            assertEquals(1, result.get(1).getUserId());
        }

        @Test
        @DisplayName("filters posts by id only")
        void filtersByIdOnly() {
            when(auditionIntegrationClient.getPosts()).thenReturn(mockPosts);

            List<AuditionPost> result = auditionService.getPosts(null, 30);

            assertEquals(1, result.size(), "Should return only post with id=30");
            assertEquals(30, result.get(0).getId());
            assertEquals(2, result.get(0).getUserId());
        }

        @Test
        @DisplayName("filters posts by both userId and id with AND logic when both match")
        void filtersByBothUserIdAndIdMatching() {
            when(auditionIntegrationClient.getPosts()).thenReturn(mockPosts);

            List<AuditionPost> result = auditionService.getPosts(1, 10);

            assertEquals(1, result.size(), "Should return post matching both userId=1 AND id=10");
            assertEquals(10, result.get(0).getId());
            assertEquals(1, result.get(0).getUserId());
        }

        @Test
        @DisplayName("returns empty list when both userId and id filters don't match any post")
        void returnsEmptyWhenBothFiltersDoNotMatch() {
            when(auditionIntegrationClient.getPosts()).thenReturn(mockPosts);

            // userId=1 has posts with id=10,20 but not id=30
            List<AuditionPost> result = auditionService.getPosts(1, 30);

            assertTrue(result.isEmpty(), "Should return empty list when no post matches both filters");
        }

        @Test
        @DisplayName("returns empty list when userId filter matches no posts")
        void returnsEmptyWhenUserIdDoesNotMatch() {
            when(auditionIntegrationClient.getPosts()).thenReturn(mockPosts);

            List<AuditionPost> result = auditionService.getPosts(999, null);

            assertTrue(result.isEmpty(), "Should return empty list when userId matches no posts");
        }

        @Test
        @DisplayName("returns empty list when id filter matches no posts")
        void returnsEmptyWhenIdDoesNotMatch() {
            when(auditionIntegrationClient.getPosts()).thenReturn(mockPosts);

            List<AuditionPost> result = auditionService.getPosts(null, 999);

            assertTrue(result.isEmpty(), "Should return empty list when id matches no posts");
        }

        @Test
        @DisplayName("returns empty list when integration client returns empty list")
        void returnsEmptyWhenIntegrationReturnsEmpty() {
            when(auditionIntegrationClient.getPosts()).thenReturn(Collections.emptyList());

            List<AuditionPost> result = auditionService.getPosts(1, 10);

            assertTrue(result.isEmpty(), "Should return empty list when no posts exist");
        }

        @Test
        @DisplayName("filters correctly with userId=2")
        void filtersByUserIdTwo() {
            when(auditionIntegrationClient.getPosts()).thenReturn(mockPosts);

            List<AuditionPost> result = auditionService.getPosts(2, null);

            assertEquals(1, result.size(), "Should return only posts with userId=2");
            assertEquals(30, result.get(0).getId());
            assertEquals(2, result.get(0).getUserId());
        }

        @Test
        @DisplayName("filters correctly with id=40")
        void filtersByIdForty() {
            when(auditionIntegrationClient.getPosts()).thenReturn(mockPosts);

            List<AuditionPost> result = auditionService.getPosts(null, 40);

            assertEquals(1, result.size(), "Should return only post with id=40");
            assertEquals(40, result.get(0).getId());
            assertEquals(3, result.get(0).getUserId());
        }
    }

    @Nested
    @DisplayName("getPostById")
    class GetPostByIdTests {

        @Test
        @DisplayName("returns post from client")
        void returnsPost() {
            String postId = "1";
            AuditionPost expected = new AuditionPost();
            expected.setId(1);
            expected.setUserId(1);
            expected.setTitle("Test Post");
            expected.setBody("Test Body");

            when(auditionIntegrationClient.getPostById(postId)).thenReturn(expected);

            AuditionPost result = auditionService.getPostById(postId);

            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("getPostWithComments")
    class GetPostWithCommentsTests {

        @Test
        @DisplayName("returns post with comments from client")
        void returnsPostWithComments() {
            String postId = "1";
            AuditionPost expected = new AuditionPost();
            expected.setId(1);
            expected.setUserId(1);
            expected.setTitle("Test Post");
            expected.setBody("Test Body");

            when(auditionIntegrationClient.getPostWithComments(postId)).thenReturn(expected);

            AuditionPost result = auditionService.getPostWithComments(postId);

            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("getCommentsForPost")
    class GetCommentsForPostTests {

        @Test
        @DisplayName("returns comments from client")
        void returnsComments() {
            String postId = "1";
            Comment comment1 = new Comment();
            comment1.setId(1);
            comment1.setPostId(1);
            comment1.setName("Test");
            comment1.setEmail("test@example.com");
            comment1.setBody("Comment body");

            List<Comment> expected = List.of(comment1);

            when(auditionIntegrationClient.getCommentsForPost(postId)).thenReturn(expected);

            List<Comment> result = auditionService.getCommentsForPost(postId);

            assertEquals(expected, result);
        }
    }
}
