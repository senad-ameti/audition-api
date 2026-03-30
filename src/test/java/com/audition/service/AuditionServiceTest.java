package com.audition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostFilter;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditionServiceTest {

    @Mock
    private AuditionIntegrationClient client;

    @InjectMocks
    private AuditionService service;

    private final List<AuditionPost> allPosts = List.of(
        new AuditionPost(1, 1, "sunt aut facere", "body1"),
        new AuditionPost(1, 2, "qui est esse", "body2"),
        new AuditionPost(2, 3, "vero aut provident", "body3")
    );

    @ParameterizedTest(name = "userId={0} title={1} expectedCount={2}")
    @MethodSource("filterCases")
    void postsFilteredCorrectly(final String userId, final String title, final int expectedCount) {
        when(client.getPosts(userId)).thenReturn(allPosts);
        final List<AuditionPost> result = service.getPosts(new AuditionPostFilter(userId, title));
        assertEquals(expectedCount, result.size());
    }

    static Stream<Arguments> filterCases() {
        return Stream.of(
            Arguments.of(null, null, 3),
            Arguments.of(null, "  ", 3),
            Arguments.of(null, "vero", 1),
            Arguments.of(null, "VERO", 1),
            Arguments.of("1", null, 3)
        );
    }

    @Test
    void postByIdDelegatesToClient() {
        final AuditionPost post = new AuditionPost(1, 1, "title", "body");
        when(client.getPostById("1")).thenReturn(post);
        assertEquals(post, service.getPostById("1"));
    }

    @Test
    void postWithCommentsDelegatesToClient() {
        final AuditionPost post = new AuditionPost(1, 1, "title", "body");
        when(client.getPostWithComments("1")).thenReturn(post);
        assertEquals(post, service.getPostWithComments("1"));
    }

    @Test
    void commentsDelegatesToClient() {
        final List<AuditionComment> comments = List.of(new AuditionComment(1, 1, "n", "e", "b"));
        when(client.getComments("1")).thenReturn(comments);
        assertEquals(comments, service.getComments("1"));
        verify(client).getComments("1");
    }
}
