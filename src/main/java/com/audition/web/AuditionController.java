package com.audition.web;

import com.audition.dto.AuditionPostCommentDTO;
import com.audition.dto.AuditionPostDTO;
import com.audition.service.AuditionService;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Observed(name = "auditionController")
@RestController
@Validated
public class AuditionController {

    private final AuditionService auditionService;

    public AuditionController(final AuditionService auditionService) {
        this.auditionService = auditionService;
    }

    @GetMapping(value = "/posts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuditionPostDTO> getPosts(
        @Valid @PositiveOrZero(message = "userId must be greater than or equal to 0") @RequestParam(value = "userId", required = false) final Integer userId) {
        return auditionService.getPosts(userId);
    }

    @GetMapping(value = "/posts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuditionPostDTO getPostByPostId(
        @Valid @PositiveOrZero(message = "postId must be greater than or equal to 0") @PathVariable("id") final Integer postId) {
        return auditionService.getPostById(postId);
    }

    @GetMapping(value = "/posts/{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuditionPostDTO getPostWithComments(
        @Valid @PositiveOrZero(message = "postId must be greater than or equal to 0") @PathVariable("id") final Integer postId) {
        return auditionService.getPostWithCommentsById(postId);
    }

    @GetMapping(value = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuditionPostCommentDTO> getCommentsForPosts(
        @Valid @PositiveOrZero(message = "postId must be greater than or equal to 0") @RequestParam("postId") final Integer postId) {
        return auditionService.getCommentsByPostId(postId);
    }
}
