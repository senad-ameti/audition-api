package com.audition.web;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import com.audition.service.AuditionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AuditionControllerTest {

    @Mock
    private AuditionService auditionService;

    @InjectMocks
    private AuditionController controller;

    private AuditionPost samplePost;

    @Mock
    private AuditionLogger auditionLogger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        samplePost = new AuditionPost();
        samplePost.setId(1);
        samplePost.setUserId(10);
        samplePost.setTitle("Sample Title");
        samplePost.setBody("Sample Body Content");
    }

    // -------- getPosts --------
    @Test
    void postsWithoutFiltersShouldReturnAll() {
        when(auditionService.getPosts()).thenReturn(List.of(samplePost));

        final List<AuditionPost> result = controller.getPosts(null, null, null, null);
        assertThat(result).hasSize(1);
    }

    @Test
    void postsWithFiltersShouldMatch() {
        when(auditionService.getPosts()).thenReturn(List.of(samplePost));

        final List<AuditionPost> result = controller.getPosts(10, 1, "sample", "body");
        assertThat(result).hasSize(1);
    }

    @Test
    void postsWithFiltersShouldNotMatch() {
        when(auditionService.getPosts()).thenReturn(List.of(samplePost));

        final List<AuditionPost> result = controller.getPosts(99, 2, "nomatch", "other");
        assertThat(result).isEmpty();
    }

    @Test
    void postsShouldReturnEmptyList() {
        when(auditionService.getPosts()).thenReturn(List.of());

        final List<AuditionPost> result = controller.getPosts(null, null, null, null);
        assertThat(result).isEmpty();
    }

    // -------- getPostById --------
    @Test
    void postByIdShouldReturnSuccess() {
        when(auditionService.getPostById(anyString())).thenReturn(samplePost);

        final ResponseEntity<AuditionPost> response = controller.getPostById("1");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(samplePost);
    }

    @Test
    void postByIdShouldHandleSystemExceptionWithStatus() {
        when(auditionService.getPostById(anyString()))
                .thenThrow(new SystemException("error", "fail", 404, new RuntimeException()));

        final ResponseEntity<AuditionPost> response = controller.getPostById("99");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void postByIdShouldHandleSystemExceptionWithoutStatus() {
        when(auditionService.getPostById(anyString()))
                .thenThrow(new SystemException("error", "fail", null, new RuntimeException()));

        final ResponseEntity<AuditionPost> response = controller.getPostById("99");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }

    // -------- getCommentsForPost --------
    @Test
    void commentsForPostShouldReturnSuccess() {
        final List<AuditionComment> comments = List.of(new AuditionComment());
        when(auditionService.getCommentsForPost(anyString())).thenReturn(comments);

        final ResponseEntity<List<AuditionComment>> response = controller.getCommentsForPost("1");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    // -------- getCommentsByPostQuery --------
    @Test
    void commentsByPostQueryShouldReturnSuccess() {
        final List<AuditionComment> comments = List.of(new AuditionComment());
        when(auditionService.getCommentsByQuery(anyString())).thenReturn(comments);

        final ResponseEntity<List<AuditionComment>> response = controller.getCommentsByPostQuery("1");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}
