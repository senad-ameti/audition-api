package com.audition.web;

import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostFilter;
import com.audition.service.AuditionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
    public @ResponseBody List<AuditionPost> getPosts(@Valid AuditionPostFilter auditionPostFilter) {
        return auditionService.getPosts(auditionPostFilter);
    }

    @RequestMapping(value = "/posts/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody AuditionPost getPostById(
        @PathVariable("id")
        @NotBlank(message = "Post id cannot be blank")
        final String postId) {
        return auditionService.getPostById(postId);
    }

    @RequestMapping(value = "/posts/{id}/comments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody AuditionPost getPostWithComments(
        @PathVariable("id")
        @NotBlank(message = "Post id cannot be blank")
        final String postId) {
        return auditionService.getPostWithComments(postId);
    }

    @RequestMapping(value = "/comments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<AuditionComment> getComments(@RequestParam(value = "postId", required = false) final String postId) {
        return auditionService.getComments(postId);
    }
}
