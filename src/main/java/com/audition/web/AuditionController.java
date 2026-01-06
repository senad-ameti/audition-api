package com.audition.web;

import com.audition.common.exception.SystemException;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import com.audition.service.AuditionService;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditionController {

    @Autowired
    transient AuditionService auditionService;

    // Add a query param that allows data filtering. The intent of the filter is at developers discretion.
    @RequestMapping(value = "/posts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<AuditionPost> getPosts(
        @RequestParam(value = "userId", required = false) final String userId) {
        // Add logic that filters response data based on the query param.
        // Currently, the userId filter has been added.
        // More filters like title/body filtering can be added as needed later.
        // Paging and sorting can also be added as needed later.
        final List<AuditionPost> auditionPosts = auditionService.getPosts();

        // input validation
        if (StringUtils.isEmpty(userId)) {
            return auditionPosts;
        }
        final int userIdInt;
        try {
            userIdInt = Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            throw new SystemException(String.format("Invalid userId parameter %s", userId), "Bad Request",
                HttpStatus.BAD_REQUEST.value());
        }

        return auditionPosts.stream().filter(post -> post.getUserId() == userIdInt).toList();
    }

    @RequestMapping(value = "/posts/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody AuditionPost getPostById(@PathVariable("id") final String postId) {
        // input validation
        final int postIdInt = getIntegerId(postId);

        return auditionService.getPostById(postIdInt);
    }

    // Add additional methods to return comments for each post. Hint: Check https://jsonplaceholder.typicode.com/
    @RequestMapping(value = "/posts/{id}/comments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<AuditionComment> getCommentsForPost(@PathVariable("id") final String postId) {
        // input validation
        final int postIdInt = getIntegerId(postId);

        // assume the comments are returned separately from posts
        // alternatively, comments can be part of the AuditionPost object,
        // using auditionService.getPostWithCommentsById(postIdInt);
        return auditionService.getCommentsByPostId(postIdInt);
    }

    private static int getIntegerId(final String postId) {
        final int idInt;
        try {
            idInt = Integer.parseInt(postId);
        } catch (NumberFormatException e) {
            throw new SystemException(String.format("Invalid postId parameter %s", postId), "Bad Request",
                HttpStatus.BAD_REQUEST.value());
        }
        return idInt;
    }
}
