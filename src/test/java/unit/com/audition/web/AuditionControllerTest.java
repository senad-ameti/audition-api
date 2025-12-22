package unit.com.audition.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.model.AuditionPost;
import com.audition.model.Comment;
import com.audition.service.AuditionService;
import com.audition.web.AuditionController;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    private static final String BODY_TEXT = "body";
    private static final String GET_POSTS_METHOD = "getPosts";
    private static final String POST_ID_EMPTY_MSG = "Post ID cannot be null or empty";
    private static final String POST_ID_INTEGER_MSG = "Post ID must be a valid integer";
    private static final String POST_ID_POSITIVE_MSG = "Post ID must be a positive integer";
    private static final String COMMENT_1 = "Comment 1";

    @Mock
    private transient AuditionService auditionService;

    @InjectMocks
    private transient AuditionController auditionController;

    private transient ExecutableValidator executableValidator;

    @BeforeEach
    void setUp() {
        // Mockito will inject mocks into controller
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            final Validator validator = factory.getValidator();
            executableValidator = validator.forExecutables();
        }
    }

    @Nested
    @DisplayName("GET /posts")
    class GetPosts {

        @Test
        @DisplayName("getPosts returns posts provided by the service when no filters applied")
        void testGetPostsReturnsPostsFromService() {
            final AuditionPost first = new AuditionPost();
            first.setId(1);
            first.setUserId(10);
            first.setTitle("t1");
            first.setBody("b1");

            final AuditionPost second = new AuditionPost();
            second.setId(2);
            second.setUserId(20);
            second.setTitle("t2");
            second.setBody("b2");

            when(auditionService.getPosts(null, null)).thenReturn(Arrays.asList(first, second));

            final List<AuditionPost> result = auditionController.getPosts(null, null);

            assertEquals(2, result.size(), "Controller should return the posts provided by the service");
            assertEquals(10, result.get(0).getUserId());
            assertEquals("t2", result.get(1).getTitle());
        }

        @Test
        @DisplayName("getPosts returns empty list when service returns none")
        void testGetPostsReturnsEmptyWhenServiceEmpty() {
            when(auditionService.getPosts(null, null)).thenReturn(Collections.emptyList());

            final List<AuditionPost> result = auditionController.getPosts(null, null);

            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("getPosts passes userId filter to service")
        void testGetPostsPassesUserIdFilterToService() {
            final AuditionPost post = new AuditionPost();
            post.setId(1);
            post.setUserId(5);
            post.setTitle("User 5 Post");
            post.setBody(BODY_TEXT);

            when(auditionService.getPosts(eq(5), eq(null))).thenReturn(Collections.singletonList(post));

            final List<AuditionPost> result = auditionController.getPosts("5", null);

            assertEquals(1, result.size());
            assertEquals(5, result.get(0).getUserId());
            verify(auditionService).getPosts(eq(5), eq(null));
        }

        @Test
        @DisplayName("getPosts passes id filter to service")
        void testGetPostsPassesIdFilterToService() {
            final AuditionPost post = new AuditionPost();
            post.setId(42);
            post.setUserId(1);
            post.setTitle("Post 42");
            post.setBody(BODY_TEXT);

            when(auditionService.getPosts(eq(null), eq(42))).thenReturn(Collections.singletonList(post));

            final List<AuditionPost> result = auditionController.getPosts(null, "42");

            assertEquals(1, result.size());
            assertEquals(42, result.get(0).getId());
            verify(auditionService).getPosts(eq(null), eq(42));
        }

        @Test
        @DisplayName("getPosts passes both userId and id filters to service")
        void testGetPostsPassesBothFiltersToService() {
            final AuditionPost post = new AuditionPost();
            post.setId(10);
            post.setUserId(3);
            post.setTitle("Post 10");
            post.setBody(BODY_TEXT);

            when(auditionService.getPosts(eq(3), eq(10))).thenReturn(Collections.singletonList(post));

            final List<AuditionPost> result = auditionController.getPosts("3", "10");

            assertEquals(1, result.size());
            assertEquals(3, result.get(0).getUserId());
            assertEquals(10, result.get(0).getId());
            verify(auditionService).getPosts(eq(3), eq(10));
        }

        @Test
        @DisplayName("getPosts returns empty list when filters match nothing")
        void testGetPostsReturnsEmptyWhenFiltersMatchNothing() {
            when(auditionService.getPosts(eq(999), eq(888))).thenReturn(Collections.emptyList());

            final List<AuditionPost> result = auditionController.getPosts("999", "888");

            assertEquals(0, result.size());
            verify(auditionService).getPosts(eq(999), eq(888));
        }
    }

    @Nested
    @DisplayName("GET /posts/{id}")
    class GetPostById {

        @Test
        @DisplayName("getPosts by id returns post from service")
        void testGetPostByIdReturnsPost() {
            final String postId = "1";
            final AuditionPost expected = new AuditionPost();
            expected.setId(1);
            expected.setUserId(5);
            expected.setTitle("title");
            expected.setBody(BODY_TEXT);

            when(auditionService.getPostById(postId)).thenReturn(expected);

            final AuditionPost result = auditionController.getPosts(postId);

            assertEquals(1, result.getId());
            assertEquals(5, result.getUserId());
            assertEquals("title", result.getTitle());
        }

        @Test
        @DisplayName("getPosts by id returns null when service returns null")
        void testGetPostByIdReturnsNullWhenMissing() {
            final String postId = "999";
            when(auditionService.getPostById(postId)).thenReturn(null);

            final AuditionPost result = auditionController.getPosts(postId);

            assertNull(result, "Controller should return null when service returns null");
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is null")
        void throwsExceptionWhenPostIdIsNull() throws Exception {
            final Method method = AuditionController.class.getMethod(GET_POSTS_METHOD, String.class);
            final Object[] parameterValues = {null};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_EMPTY_MSG));
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is empty")
        void throwsExceptionWhenPostIdIsEmpty() throws Exception {
            final Method method = AuditionController.class.getMethod(GET_POSTS_METHOD, String.class);
            final Object[] parameterValues = {""};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_EMPTY_MSG));
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is blank")
        void throwsExceptionWhenPostIdIsBlank() throws Exception {
            final Method method = AuditionController.class.getMethod(GET_POSTS_METHOD, String.class);
            final Object[] parameterValues = {"   "};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_EMPTY_MSG));
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is not a valid integer")
        void throwsExceptionWhenPostIdIsNotInteger() throws Exception {
            final Method method = AuditionController.class.getMethod(GET_POSTS_METHOD, String.class);
            final Object[] parameterValues = {"abc"};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_INTEGER_MSG));
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is a decimal number")
        void throwsExceptionWhenPostIdIsDecimal() throws Exception {
            final Method method = AuditionController.class.getMethod(GET_POSTS_METHOD, String.class);
            final Object[] parameterValues = {"1.5"};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_INTEGER_MSG));
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is zero")
        void throwsExceptionWhenPostIdIsZero() throws Exception {
            final Method method = AuditionController.class.getMethod(GET_POSTS_METHOD, String.class);
            final Object[] parameterValues = {"0"};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_POSITIVE_MSG));
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is negative")
        void throwsExceptionWhenPostIdIsNegative() throws Exception {
            final Method method = AuditionController.class.getMethod(GET_POSTS_METHOD, String.class);
            final Object[] parameterValues = {"-1"};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_POSITIVE_MSG));
        }
    }

    @Nested
    @DisplayName("GET /posts/{id}/comments")
    class GetCommentsForPost {

        @Test
        @DisplayName("getCommentsForPost returns comments from service")
        void testGetCommentsForPostReturnsComments() {
            final String postId = "1";
            final Comment comment1 = new Comment();
            comment1.setId(1);
            comment1.setPostId(1);
            comment1.setName(COMMENT_1);
            comment1.setEmail("user1@test.com");
            comment1.setBody("Comment body 1");

            final Comment comment2 = new Comment();
            comment2.setId(2);
            comment2.setPostId(1);
            comment2.setName("Comment 2");
            comment2.setEmail("user2@test.com");
            comment2.setBody("Comment body 2");

            when(auditionService.getCommentsForPost(postId)).thenReturn(Arrays.asList(comment1, comment2));

            final List<Comment> result = auditionController.getCommentsForPost(postId);

            assertEquals(2, result.size());
            assertEquals(COMMENT_1, result.get(0).getName());
            assertEquals("user2@test.com", result.get(1).getEmail());
            verify(auditionService).getCommentsForPost(postId);
        }

        @Test
        @DisplayName("getCommentsForPost returns empty list when no comments exist")
        void testGetCommentsForPostReturnsEmptyWhenNoComments() {
            final String postId = "1";
            when(auditionService.getCommentsForPost(postId)).thenReturn(Collections.emptyList());

            final List<Comment> result = auditionController.getCommentsForPost(postId);

            assertEquals(0, result.size());
            verify(auditionService).getCommentsForPost(postId);
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is null")
        void throwsExceptionWhenPostIdIsNull() throws Exception {
            final Method method = AuditionController.class.getMethod("getCommentsForPost", String.class);
            final Object[] parameterValues = {null};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_EMPTY_MSG));
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is not a valid integer")
        void throwsExceptionWhenPostIdIsNotInteger() throws Exception {
            final Method method = AuditionController.class.getMethod("getCommentsForPost", String.class);
            final Object[] parameterValues = {"abc"};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_INTEGER_MSG));
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is negative")
        void throwsExceptionWhenPostIdIsNegative() throws Exception {
            final Method method = AuditionController.class.getMethod("getCommentsForPost", String.class);
            final Object[] parameterValues = {"-1"};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_POSITIVE_MSG));
        }
    }

    @Nested
    @DisplayName("GET /posts/{id}?includeComments=true")
    class GetPostWithComments {

        @Test
        @DisplayName("getPostWithComments returns post with comments from service")
        void testGetPostWithCommentsReturnsPostWithComments() {
            final String postId = "1";
            final Comment comment1 = new Comment();
            comment1.setId(1);
            comment1.setPostId(1);
            comment1.setName(COMMENT_1);
            comment1.setEmail("user1@test.com");
            comment1.setBody("Comment body 1");

            final AuditionPost post = new AuditionPost();
            post.setId(1);
            post.setUserId(5);
            post.setTitle("Test Title");
            post.setBody("Test Body");
            post.setComments(Collections.singletonList(comment1));

            when(auditionService.getPostWithComments(postId)).thenReturn(post);

            final AuditionPost result = auditionController.getPostWithComments(postId);

            assertEquals(1, result.getId());
            assertEquals("Test Title", result.getTitle());
            assertEquals(1, result.getComments().size());
            assertEquals(COMMENT_1, result.getComments().get(0).getName());
            verify(auditionService).getPostWithComments(postId);
        }

        @Test
        @DisplayName("getPostWithComments returns post with empty comments list")
        void testGetPostWithCommentsReturnsPostWithEmptyComments() {
            final String postId = "1";
            final AuditionPost post = new AuditionPost();
            post.setId(1);
            post.setUserId(5);
            post.setTitle("Test Title");
            post.setBody("Test Body");
            post.setComments(Collections.emptyList());

            when(auditionService.getPostWithComments(postId)).thenReturn(post);

            final AuditionPost result = auditionController.getPostWithComments(postId);

            assertEquals(1, result.getId());
            assertTrue(result.getComments().isEmpty());
            verify(auditionService).getPostWithComments(postId);
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is null")
        void throwsExceptionWhenPostIdIsNull() throws Exception {
            final Method method = AuditionController.class.getMethod("getPostWithComments", String.class);
            final Object[] parameterValues = {null};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_EMPTY_MSG));
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is not a valid integer")
        void throwsExceptionWhenPostIdIsNotInteger() throws Exception {
            final Method method = AuditionController.class.getMethod("getPostWithComments", String.class);
            final Object[] parameterValues = {"abc"};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_INTEGER_MSG));
        }

        @Test
        @DisplayName("throws ConstraintViolationException when postId is negative")
        void throwsExceptionWhenPostIdIsNegative() throws Exception {
            final Method method = AuditionController.class.getMethod("getPostWithComments", String.class);
            final Object[] parameterValues = {"-1"};
            final Set<ConstraintViolation<AuditionController>> violations =
                executableValidator.validateParameters(auditionController, method, parameterValues);

            assertFalse(violations.isEmpty());
            assertTrue(violations.iterator().next().getMessage().contains(POST_ID_POSITIVE_MSG));
        }
    }
}
