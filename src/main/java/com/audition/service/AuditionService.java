package com.audition.service;

import com.audition.dto.AuditionPostCommentDTO;
import com.audition.dto.AuditionPostDTO;
import com.audition.integration.AuditionIntegrationClient;
import com.audition.mapper.AuditionPostMapper;
import com.audition.model.AuditionPost;
import com.audition.model.PostComment;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditionService {

    private final AuditionIntegrationClient auditionIntegrationClient;
    private final AuditionPostMapper auditionPostMapper;

    public AuditionService(
        final AuditionIntegrationClient auditionIntegrationClient,
        final AuditionPostMapper auditionPostMapper
    ) {
        this.auditionIntegrationClient = auditionIntegrationClient;
        this.auditionPostMapper = auditionPostMapper;
    }

    public List<AuditionPostDTO> getPosts(final Integer userId) {
        final List<AuditionPost> posts = auditionIntegrationClient.getPosts(userId);
        if (posts == null) {
            return Collections.emptyList();
        }

        return posts
            .stream()
            .map(auditionPostMapper::map)
            .toList();
    }

    public AuditionPostDTO getPostById(final Integer postId) {
        final AuditionPost post = auditionIntegrationClient.getPostById(postId);
        return auditionPostMapper.map(post);
    }

    public AuditionPostDTO getPostWithCommentsById(final Integer postId) {
        final AuditionPost post = auditionIntegrationClient.getPostById(postId);
        if (post == null) {
            return null;
        }

        final List<PostComment> comments = auditionIntegrationClient.getCommentsByPostId(postId);

        return auditionPostMapper.mapPostAndComments(post, comments);
    }

    public List<AuditionPostCommentDTO> getCommentsByPostId(final Integer postId) {
        return auditionIntegrationClient.getCommentsByPostId(postId)
            .stream()
            .map(auditionPostMapper::mapComment)
            .toList();
    }
}
