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

/**
 * Client for integrating with an external API to retrieve posts and comments. This component uses Spring's RestTemplate
 * to make HTTP requests and includes caching for performance optimization.
 */
@Component
public class AuditionIntegrationClient {

    private static final Logger LOG = LoggerFactory.getLogger(AuditionIntegrationClient.class);

    private final transient RestTemplate restTemplate;
    private final transient AuditionLogger logger;
    private final transient String postsUrl;
    private final transient String postsByIdUrl;
    private final transient String postCommentsUrl;

    /**
     * Constructs an AuditionIntegrationClient with the required dependencies.
     *
     * @param restTemplate    the RestTemplate for making HTTP requests
     * @param logger          the custom logger for auditing
     * @param postsUrl        the URL for fetching all posts
     * @param postsByIdUrl    the URL template for fetching a post by ID
     * @param postCommentsUrl the URL template for fetching comments for a post
     */
    public AuditionIntegrationClient(
        @Autowired final RestTemplate restTemplate,
        @Autowired final AuditionLogger logger,
        @Value("${audition.integration.posts-url}") final String postsUrl,
        @Value("${audition.integration.posts-by-id-url}") final String postsByIdUrl,
        @Value("${audition.integration.post-comments-url}") final String postCommentsUrl) {
        this.restTemplate = restTemplate;
        this.logger = logger;
        this.postsUrl = postsUrl;
        this.postsByIdUrl = postsByIdUrl;
        this.postCommentsUrl = postCommentsUrl;
    }

    /**
     * Retrieves a list of all posts from the external API. Results are cached to improve performance.
     *
     * @return a list of AuditionPost objects, or an empty list if none found
     * @throws SystemException if there is an error communicating with the external API
     */
    @Cacheable(value = "posts", unless = "#result == null || #result.isEmpty()")
    public List<AuditionPost> getPosts() {
        logger.logCacheMiss(LOG, "posts", "all");
        AuditionPost[] posts;
        try {
            posts = Optional.ofNullable(restTemplate.getForObject(postsUrl, AuditionPost[].class))
                .orElse(new AuditionPost[0]);
            if (posts.length == 0) {
                logger.debug(LOG, "No posts found from external API");
            }
        } catch (final HttpClientErrorException e) {
            throw new SystemException(e.getMessage(), "External API Error",
                e.getStatusCode().value(), e);
        }

        return Arrays.asList(posts);
    }

    /**
     * Retrieves a specific post by its ID from the external API. Results are cached to improve performance.
     *
     * @param id the ID of the post to retrieve
     * @return the AuditionPost object corresponding to the given ID
     * @throws SystemException if the post is not found or if there is an error communicating with the external API
     */
    @Cacheable(value = "postsById", key = "#id", unless = "#result == null")
    public AuditionPost getPostById(final String id) {
        logger.logCacheMiss(LOG, "postsById", id);
        try {
            return restTemplate.getForObject(postsByIdUrl, AuditionPost.class, id);
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new SystemException("Cannot find a Post with id " + id, "Resource Not Found",
                    404, e);
            } else {
                throw new SystemException(e.getMessage(), "External API Error",
                    e.getStatusCode().value(), e);
            }
        }
    }

    /**
     * Retrieves a specific post by its ID along with its associated comments from the external API. Results are cached
     * to improve performance.
     *
     * @param postId the ID of the post to retrieve with comments
     * @return the AuditionPost object with its comments populated
     * @throws SystemException if the post or comments are not found or if there is an error communicating with the
     *                         external API
     */
    @Cacheable(value = "postWithComments", key = "#postId", unless = "#result == null")
    public AuditionPost getPostWithComments(final String postId) {
        logger.logCacheMiss(LOG, "postWithComments", postId);
        final AuditionPost post = getPostById(postId);
        final List<Comment> comments = getCommentsForPost(postId);
        post.setComments(comments);
        return post;
    }

    /**
     * Retrieves a list of comments for a specific post from the external API. Results are cached to improve
     * performance.
     *
     * @param postId the ID of the post for which to retrieve comments
     * @return a list of Comment objects, or an empty list if none found
     * @throws SystemException if the comments are not found or if there is an error communicating with the external
     *                         API
     */
    @Cacheable(value = "comments", key = "#postId", unless = "#result == null || #result.isEmpty()")
    public List<Comment> getCommentsForPost(final String postId) {
        logger.logCacheMiss(LOG, "comments", postId);
        try {
            final Comment[] comments = Optional.ofNullable(
                    restTemplate.getForObject(postCommentsUrl, Comment[].class, postId))
                .orElse(new Comment[0]);
            if (comments.length == 0) {
                logger.debug(LOG, "No comments found for post with id " + postId);
            }
            return Arrays.asList(comments);
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new SystemException("Cannot find comments for Post with id " + postId,
                    "Resource Not Found", 404, e);
            } else {
                throw new SystemException(e.getMessage(), "External API Error",
                    e.getStatusCode().value(), e);
            }
        }
    }
}
