package com.audition.web;

import com.audition.model.AuditionPost;
import com.audition.model.Comment;
import com.audition.service.AuditionService;
import com.audition.web.validation.PositiveInteger;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling audition-related API requests. Provides endpoints for retrieving posts and comments from
 * an external service.
 */
@RestController
@RequiredArgsConstructor
@Validated
public class AuditionController {

    private final AuditionService auditionService;

    /**
     * Retrieves a list of posts, optionally filtered by user ID or post ID.
     *
     * @param userId the user ID to filter posts by (optional)
     * @param id     the post ID to filter by (optional)
     * @return a list of {@link AuditionPost} objects
     */
    @RequestMapping(value = "/posts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<AuditionPost> getPosts(
        @RequestParam(required = false) @PositiveInteger(fieldName = "User ID", optional = true) final String userId,
        @RequestParam(required = false) @PositiveInteger(fieldName = "ID", optional = true) final String id) {
        return auditionService.getPosts(
            userId != null ? Integer.parseInt(userId) : null,
            id != null ? Integer.parseInt(id) : null);
    }

    /**
     * Retrieves a single post by its ID.
     *
     * @param postId the ID of the post to retrieve
     * @return the {@link AuditionPost} object
     */
    @RequestMapping(value = "/posts/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody AuditionPost getPosts(
        @PathVariable("id") @PositiveInteger(fieldName = "Post ID") final String postId) {
        return auditionService.getPostById(postId);
    }

    /**
     * Retrieves comments for a specific post.
     *
     * @param postId the ID of the post
     * @return a list of {@link Comment} objects
     */
    @RequestMapping(value = "/posts/{id}/comments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Comment> getCommentsForPost(
        @PathVariable("id") @PositiveInteger(fieldName = "Post ID") final String postId) {
        return auditionService.getCommentsForPost(postId);
    }

    /**
     * Retrieves a post along with its comments.
     *
     * @param postId the ID of the post
     * @return the {@link AuditionPost} object with comments included
     */
    @RequestMapping(value = "/posts/{id}", method = RequestMethod.GET, params = "includeComments=true", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody AuditionPost getPostWithComments(
        @PathVariable("id") @PositiveInteger(fieldName = "Post ID") final String postId) {
        return auditionService.getPostWithComments(postId);
    }

}
