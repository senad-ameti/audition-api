package com.audition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.audition.dto.AuditionPostCommentDTO;
import com.audition.dto.AuditionPostDTO;
import com.audition.integration.AuditionIntegrationClient;
import com.audition.mapper.AuditionPostMapper;
import com.audition.model.AuditionPost;
import com.audition.model.PostComment;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditionServiceTest {

    @Mock
    private AuditionIntegrationClient auditionIntegrationClient;

    @Mock
    private AuditionPostMapper auditionPostMapper;

    @InjectMocks
    private AuditionService service;

    private final Integer userId1 = 1;
    private final AuditionPost post1 = new AuditionPost(userId1, 1, "title1", "body1");
    private final AuditionPost post2 = new AuditionPost(userId1, 2, "title2", "body2");

    @Test
    void getPostsShouldReturnCorrectResult() {

        when(auditionIntegrationClient.getPosts(userId1)).thenReturn(List.of(post1, post2));

        final AuditionPostDTO expectedDTO1 = new AuditionPostDTO(userId1, 1, "title1", "body1", List.of());
        final AuditionPostDTO expectedDTO2 = new AuditionPostDTO(userId1, 2, "title2", "body2", List.of());

        when(auditionPostMapper.map(post1)).thenReturn(expectedDTO1);
        when(auditionPostMapper.map(post2)).thenReturn(expectedDTO2);

        assertThat(service.getPosts(userId1))
            .hasSize(2)
            .containsExactly(expectedDTO1, expectedDTO2);
    }

    @Test
    void givenInvalidIdThenGetPostByIdShouldReturnNull() {
        final Integer notExistedId = 500;
        when(auditionIntegrationClient.getPostById(notExistedId)).thenReturn(null);
        when(auditionPostMapper.map(null)).thenReturn(null);

        assertThat(service.getPostById(notExistedId)).isNull();
    }

    @Test
    void getPostByIdShouldReturnCorrectResult() {
        when(auditionIntegrationClient.getPostById(1)).thenReturn(post1);
        final AuditionPostDTO expectedDTO1 = new AuditionPostDTO(userId1, 1, "title1", "body1", List.of());

        when(auditionPostMapper.map(post1)).thenReturn(expectedDTO1);

        assertThat(service.getPostById(1)).isEqualTo(expectedDTO1);
    }


    @Test
    void getPostWithCommentsByIdShouldReturnCorrectResult() {
        final int postId = post1.id();
        when(auditionIntegrationClient.getPostById(postId)).thenReturn(post1);

        final List<PostComment> comments = List.of(new PostComment(postId, 1, "name", "email", "body"));
        when(auditionIntegrationClient.getCommentsByPostId(postId)).thenReturn(comments);

        final AuditionPostDTO expectedAuditionPostDTO = new AuditionPostDTO(userId1, 1, "title1", "body1",
            List.of(new AuditionPostCommentDTO(postId, 1, "name", "email", "body")));

        when(auditionPostMapper.mapPostAndComments(post1, comments)).thenReturn(expectedAuditionPostDTO);

        assertThat(service.getPostWithCommentsById(1)).isEqualTo(expectedAuditionPostDTO);
    }

    @Test
    void getCommentsByPostIdShouldReturnEmptyListWhenNoCommentsFound() {
        final int postId = post1.id();

        when(auditionIntegrationClient.getCommentsByPostId(postId)).thenReturn(List.of());

        assertThat(service.getCommentsByPostId(postId)).isEmpty();
    }

    @Test
    void getCommentsByPostIdShouldReturnCorrectResult() {
        final int postId = post1.id();
        final PostComment comment1 = new PostComment(postId, 1, "name", "email", "body");
        final PostComment comment2 = new PostComment(postId, 2, "name2", "email2", "body2");

        when(auditionIntegrationClient.getCommentsByPostId(postId)).thenReturn(List.of(comment1, comment2));

        List<AuditionPostCommentDTO> expectedCommentDTOs = List.of(
            new AuditionPostCommentDTO(postId, 1, "name", "email", "body"),
            new AuditionPostCommentDTO(postId, 2, "name2", "email2", "body2")
        );

        when(auditionPostMapper.mapComment(comment1)).thenReturn(expectedCommentDTOs.get(0));
        when(auditionPostMapper.mapComment(comment2)).thenReturn(expectedCommentDTOs.get(1));

        assertThat(service.getCommentsByPostId(postId)).isEqualTo(expectedCommentDTOs);
    }
}
