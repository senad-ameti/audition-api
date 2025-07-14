package com.audition.mapper;

import com.audition.dto.AuditionPostCommentDTO;
import com.audition.dto.AuditionPostDTO;
import com.audition.model.AuditionPost;
import com.audition.model.PostComment;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AuditionPostMapper {

    public AuditionPostDTO map(final AuditionPost post) {
        if (post == null) {
            return null;
        }

        return new AuditionPostDTO(post.userId(), post.id(), post.title(), post.body(), List.of());
    }


    public AuditionPostDTO mapPostAndComments(final AuditionPost post, final List<PostComment> comments) {
        if (post == null) {
            return null;
        }

        List<AuditionPostCommentDTO> commentDTOs = List.of();

        if (comments != null) {
            commentDTOs = comments.stream().map(this::mapComment).toList();
        }

        return new AuditionPostDTO(post.userId(), post.id(), post.title(), post.body(), commentDTOs);
    }

    public AuditionPostCommentDTO mapComment(final PostComment comment) {
        if (comment == null) {
            return null;
        }

        return new AuditionPostCommentDTO(comment.postId(), comment.id(), comment.name(), comment.email(),
            comment.body());
    }
}
