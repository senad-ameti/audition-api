package com.audition.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.audition.common.exception.SystemException;
import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class AuditionControllerTest {

    private final List<AuditionPost> allPosts = List.of(new AuditionPost(1, 1, "Title 1", "Body 1", List.of()),
        new AuditionPost(1, 2, "Title 2", "Body 2", List.of()), new AuditionPost(2, 3, "Title 3", "Body 3", List.of()));
    private final List<AuditionComment> allComments = List.of(new AuditionComment(1, 1, "Name 1", "Email 1", "Body 1"),
        new AuditionComment(1, 2, "Name 2", "Email 2", "Body 2"),
        new AuditionComment(2, 3, "Name 3", "Email 3", "Body 3"),
        new AuditionComment(2, 4, "Name 4", "Email 4", "Body 4"));


    @Autowired
    transient AuditionController auditionController;

    @MockBean
    transient AuditionIntegrationClient auditionIntegrationClient;

    @BeforeEach
    public void setup() {
        // Mock the auditionService to return allPosts when getPosts is called
        openMocks(this);
        when(auditionIntegrationClient.getPosts()).thenReturn(allPosts);
        when(auditionIntegrationClient.getPostById(1)).thenReturn(allPosts.get(0));
        when(auditionIntegrationClient.getCommentsByPostId(1)).thenReturn(
            allComments.stream().filter(comment -> comment.getPostId() == 1).toList());
    }

    @Test
    public void getPosts_nullUserId_successful() {
        List<AuditionPost> actual = auditionController.getPosts(null);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(allPosts);
    }

    @Test
    public void getPosts_emptyUserId_successful() {
        List<AuditionPost> actual = auditionController.getPosts("");

        assertThat(actual).containsExactlyInAnyOrderElementsOf(allPosts);
    }

    @Test
    public void getPosts_invalidUserId_failed() {
        assertThrows(SystemException.class, () -> auditionController.getPosts("abc"));
    }

    @Test
    public void getPosts_validUserId_successful() {
        List<AuditionPost> actual = auditionController.getPosts("1");
        List<AuditionPost> expect = allPosts.stream().filter(post -> post.getUserId() == 1).toList();
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expect);
    }

    @Test
    public void getPostById_nullPostId_failed() {
        assertThrows(SystemException.class, () -> auditionController.getPostById(null));
    }

    @Test
    public void getPostById_emptyPostId_failed() {
        assertThrows(SystemException.class, () -> auditionController.getPostById(""));
    }

    @Test
    public void getPostById_invalidPostId_failed() {
        assertThrows(SystemException.class, () -> auditionController.getPostById("abc"));
    }

    @Test
    public void getPostById_validPostId_successful() {
        AuditionPost actual = auditionController.getPostById("1");
        AuditionPost expect = allPosts.get(0);

        assertThat(actual).isEqualTo(expect);
    }

    @Test
    public void getCommentsForPost_nullPostId_failed() {
        assertThrows(SystemException.class, () -> auditionController.getCommentsForPost(null));
    }

    @Test
    public void getCommentsForPost_emptyPostId_failed() {
        assertThrows(SystemException.class, () -> auditionController.getCommentsForPost(""));
    }

    @Test
    public void getCommentsForPost_invalidPostId_failed() {
        assertThrows(SystemException.class, () -> auditionController.getCommentsForPost("abc"));
    }

    @Test
    public void getCommentsForPost_validPostId_successful() {
        List<AuditionComment> actual = auditionController.getCommentsForPost("1");
        List<AuditionComment> expect = allComments.stream().filter(comment -> comment.getPostId() == 1).toList();

        assertThat(actual).isEqualTo(expect);
    }
}
