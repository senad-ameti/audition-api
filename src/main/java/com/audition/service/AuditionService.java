package com.audition.service;

import com.audition.common.logging.AuditionLogger;
import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionPost;
import com.audition.model.Comment;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class for handling audition-related business logic. Provides methods to retrieve posts and comments from an
 * external integration client.
 */
@Service
public class AuditionService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditionService.class);

    @Autowired
    private transient AuditionIntegrationClient auditionIntegrationClient;

    @Autowired
    private transient AuditionLogger logger;

    /**
     * Retrieves a list of posts, optionally filtered by user ID or post ID.
     *
     * @param userId the user ID to filter posts by (optional, can be null)
     * @param id     the post ID to filter by (optional, can be null)
     * @return a list of {@link AuditionPost} objects matching the filters
     */
    public List<AuditionPost> getPosts(final Integer userId, final Integer id) {
        if (LOG.isDebugEnabled()) {
            logger.debug(LOG, String.format("Filtering posts with userId=%s, id=%s", userId, id));
        }

        final List<AuditionPost> allPosts = auditionIntegrationClient.getPosts();

        return allPosts.stream()
            .filter(post -> userId == null || post.getUserId() == userId)
            .filter(post -> id == null || post.getId() == id)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a single post by its ID.
     *
     * @param postId the ID of the post to retrieve
     * @return the {@link AuditionPost} object
     */
    public AuditionPost getPostById(final String postId) {
        return auditionIntegrationClient.getPostById(postId);
    }

    /**
     * Retrieves a post along with its comments.
     *
     * @param postId the ID of the post
     * @return the {@link AuditionPost} object with comments included
     */
    public AuditionPost getPostWithComments(final String postId) {
        return auditionIntegrationClient.getPostWithComments(postId);
    }

    /**
     * Retrieves comments for a specific post.
     *
     * @param postId the ID of the post
     * @return a list of {@link Comment} objects
     */
    public List<Comment> getCommentsForPost(final String postId) {
        return auditionIntegrationClient.getCommentsForPost(postId);
    }

}
