package com.audition.integration;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionPost;
import com.audition.model.Comment;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class AuditionIntegrationClient {

    private static final Logger LOG = LoggerFactory.getLogger(AuditionIntegrationClient.class);

    private final RestTemplate restTemplate;
    private final AuditionLogger logger;
    private final String postsUrl;
    private final String postsByIdUrl;
    private final String postCommentsUrl;

    public AuditionIntegrationClient(
        @Autowired RestTemplate restTemplate,
        @Autowired AuditionLogger logger,
        @Value("${audition.integration.posts-url}") String postsUrl,
        @Value("${audition.integration.posts-by-id-url}") String postsByIdUrl,
        @Value("${audition.integration.post-comments-url}") String postCommentsUrl) {
        this.restTemplate = restTemplate;
        this.logger = logger;
        this.postsUrl = postsUrl;
        this.postsByIdUrl = postsByIdUrl;
        this.postCommentsUrl = postCommentsUrl;
    }

    @Cacheable(value = "posts", unless = "#result == null || #result.isEmpty()")
    public List<AuditionPost> getPosts() {
        AuditionPost[] posts;
        try {
            posts = Optional.ofNullable(restTemplate.getForObject(postsUrl, AuditionPost[].class))
                .orElse(new AuditionPost[0]);
            if (posts.length == 0) {
                logger.debug(LOG, "No posts found from external API");
            }
        } catch (final HttpClientErrorException e) {
            // TODO Handle exceptions appropriately. Feel free to change this function as needed.
            throw new SystemException("Unknown Error message");
        }

        return Arrays.asList(posts);
    }

    @Cacheable(value = "postsById", key = "#id", unless = "#result == null")
    public AuditionPost getPostById(final String id) {
        try {
            return restTemplate.getForObject(postsByIdUrl, AuditionPost.class, id);
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new SystemException("Cannot find a Post with id " + id, "Resource Not Found",
                    404);
            } else {
                // TODO Find a better way to handle the exception so that the original error message is not lost. Feel free to change this function.
                throw new SystemException("Unknown Error message");
            }
        }
    }

    @Cacheable(value = "postWithComments", key = "#postId", unless = "#result == null")
    public AuditionPost getPostWithComments(final String postId) {
        AuditionPost post = getPostById(postId);
        List<Comment> comments = getCommentsForPost(postId);
        post.setComments(comments);
        return post;
    }

    @Cacheable(value = "comments", key = "#postId", unless = "#result == null || #result.isEmpty()")
    public List<Comment> getCommentsForPost(final String postId) {
        try {
            Comment[] comments = Optional.ofNullable(
                    restTemplate.getForObject(postCommentsUrl, Comment[].class, postId))
                .orElse(new Comment[0]);
            if (comments.length == 0) {
                logger.debug(LOG, "No comments found for post with id " + postId);
            }
            return Arrays.asList(comments);
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new SystemException("Cannot find comments for Post with id " + postId,
                    "Resource Not Found", 404);
            } else {
                throw new SystemException("Unknown Error message");
            }
        }
    }
}
