package com.audition.service;

import com.audition.common.logging.AuditionLogger;
import com.audition.client.AuditionIntegrationClient;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditionServiceTest {

    @Mock
    private AuditionIntegrationClient auditionIntegrationClient;

    @Mock
    private AuditionLogger auditionLogger;

    @InjectMocks
    private AuditionService auditionService;

    private AuditionPost samplePost;
    private AuditionComment sampleComment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        samplePost = new AuditionPost();
        samplePost.setId(1);
        samplePost.setUserId(10);
        samplePost.setTitle("Title");
        samplePost.setBody("Body");

        sampleComment = new AuditionComment();
        sampleComment.setId(100);
        sampleComment.setPostId(1);
        sampleComment.setName("Tester");
        sampleComment.setEmail("test@example.com");
        sampleComment.setBody("Comment body");
    }

    @Test
    void postsShouldDelegateToClient() {
        when(auditionIntegrationClient.getPosts()).thenReturn(List.of(samplePost));

        final List<AuditionPost> result = auditionService.getPosts();

        assertThat(result).hasSize(1).contains(samplePost);
        verify(auditionIntegrationClient).getPosts();
    }

    @Test
    void postByIdShouldDelegateToClient() {
        when(auditionIntegrationClient.getPostById(anyString())).thenReturn(samplePost);

        final AuditionPost result = auditionService.getPostById("1");

        assertThat(result).isEqualTo(samplePost);
        verify(auditionIntegrationClient).getPostById("1");
    }

    @Test
    void commentsForPostShouldDelegateToClient() {
        when(auditionIntegrationClient.getCommentsByPostId(anyString())).thenReturn(List.of(sampleComment));

        final List<AuditionComment> result = auditionService.getCommentsForPost("1");

        assertThat(result).hasSize(1).contains(sampleComment);
        verify(auditionIntegrationClient).getCommentsByPostId("1");
    }

    @Test
    void commentsByPostQueryShouldDelegateToClient() {
        when(auditionIntegrationClient.getCommentsByQuery(anyString())).thenReturn(List.of(sampleComment));

        final List<AuditionComment> result = auditionService.getCommentsByQuery("1");

        assertThat(result).hasSize(1).contains(sampleComment);
        verify(auditionIntegrationClient).getCommentsByQuery("1");
    }
}
