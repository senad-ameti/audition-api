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

@RestController
@RequiredArgsConstructor
@Validated
public class AuditionController {

    private final AuditionService auditionService;

    @RequestMapping(value = "/posts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<AuditionPost> getPosts(
        @RequestParam(required = false) @PositiveInteger(fieldName = "User ID", optional = true) final String userId,
        @RequestParam(required = false) @PositiveInteger(fieldName = "ID", optional = true) final String id) {
        return auditionService.getPosts(
            userId != null ? Integer.parseInt(userId) : null,
            id != null ? Integer.parseInt(id) : null);
    }

    @RequestMapping(value = "/posts/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody AuditionPost getPosts(
        @PathVariable("id") @PositiveInteger(fieldName = "Post ID") final String postId) {
        return auditionService.getPostById(postId);
    }

    @RequestMapping(value = "/posts/{id}/comments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Comment> getCommentsForPost(
        @PathVariable("id") @PositiveInteger(fieldName = "Post ID") final String postId) {
        return auditionService.getCommentsForPost(postId);
    }

    @RequestMapping(value = "/posts/{id}", method = RequestMethod.GET, params = "includeComments=true", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody AuditionPost getPostWithComments(
        @PathVariable("id") @PositiveInteger(fieldName = "Post ID") final String postId) {
        return auditionService.getPostWithComments(postId);
    }

}
