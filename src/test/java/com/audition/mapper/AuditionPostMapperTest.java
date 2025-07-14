package com.audition.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.audition.dto.AuditionPostCommentDTO;
import com.audition.dto.AuditionPostDTO;
import com.audition.model.AuditionPost;
import com.audition.model.PostComment;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditionPostMapperTest {

    private final AuditionPostMapper auditionPostMapper = new AuditionPostMapper();

    private final AuditionPost post = new AuditionPost(1, 1, "title1", "body1");

    @Test
    void mapShouldReturnCorrectResult() {
        assertThat(auditionPostMapper.map(null)).isNull();

        assertThat(auditionPostMapper.map(post))
            .isEqualTo(new AuditionPostDTO(1, 1, "title1", "body1", List.of()));
    }

    @Test
    void mapPostAndCommentsShouldReturnCorrectResult() {
        assertThat(auditionPostMapper.mapPostAndComments(null, null)).isNull();

        assertThat(auditionPostMapper.mapPostAndComments(post, null))
            .isEqualTo(new AuditionPostDTO(1, 1, "title1", "body1", List.of()));

        PostComment comment1 = new PostComment(1, 1, "name", "email", "body");
        PostComment comment2 = new PostComment(2, 2, "name2", "email2", "body2");

        assertThat(auditionPostMapper.mapPostAndComments(post, List.of(comment1, comment2)))
            .isEqualTo(new AuditionPostDTO(1, 1, "title1", "body1", List.of(
                new AuditionPostCommentDTO(1, 1, "name", "email", "body"),
                new AuditionPostCommentDTO(2, 2, "name2", "email2", "body2")
            )));
    }

    @Test
    void mapCommentShouldReturnCorrectResult() {
        assertThat(auditionPostMapper.mapComment(null)).isNull();

        PostComment comment = new PostComment(1, 1, "name", "email", "body");
        assertThat(auditionPostMapper.mapComment(comment))
            .isEqualTo(new AuditionPostCommentDTO(1, 1, "name", "email", "body"));
    }

}
