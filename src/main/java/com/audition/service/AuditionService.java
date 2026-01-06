package com.audition.service;

import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditionService {

    @Autowired
    private transient AuditionIntegrationClient auditionIntegrationClient;


    public List<AuditionPost> getPosts() {
        return auditionIntegrationClient.getPosts();
    }

    public AuditionPost getPostById(final int postId) {
        return auditionIntegrationClient.getPostById(postId);
    }

    public List<AuditionComment> getCommentsByPostId(final int postId) {
        return auditionIntegrationClient.getCommentsByPostId(postId);
    }

    public AuditionPost getPostWithCommentsById(final int postId) {
        return auditionIntegrationClient.getPostWithCommentsByPostId(postId);
    }
}
