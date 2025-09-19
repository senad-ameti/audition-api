package com.audition.web;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import com.audition.service.AuditionService;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/posts")
@SuppressWarnings("PMD.GuardLogStatement")
public class AuditionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditionController.class);

    private final AuditionService auditionService;

    private final AuditionLogger auditionLogger;

    /**
     * Fetch posts with optional filters.
     * Supported filters:
     * - userId (exact match)
     * - id (exact match)
     * - titleContains (substring, case-insensitive)
     * - bodyContains (substring, case-insensitive)
     *
     * @return list of filtered posts (never null).
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<AuditionPost> getPosts(
            @RequestParam(name = "userId", required = false) final Integer userId,
            @RequestParam(name = "id", required = false) final Integer id,
            @RequestParam(name = "titleContains", required = false) final String titleContains,
            @RequestParam(name = "bodyContains", required = false) final String bodyContains) {

        final List<AuditionPost> posts = auditionService.getPosts();

        return posts.stream()
                .filter(post -> userId == null || Objects.equals(post.getUserId(), userId))
                .filter(post -> id == null || Objects.equals(post.getId(), id))
                .filter(post -> titleContains == null || (post.getTitle() != null
                        && post.getTitle().toLowerCase(Locale.ROOT).contains(titleContains.toLowerCase(Locale.ROOT))))
                .filter(post -> bodyContains == null || (post.getBody() != null
                        && post.getBody().toLowerCase(Locale.ROOT).contains(bodyContains.toLowerCase(Locale.ROOT))))
                .collect(Collectors.toList());
    }

    /**
     * Fetch a post by its ID.
     *
     * @param postId must be a numeric string, positive value only
     * @return the matching {@link AuditionPost}
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuditionPost> getPostById(
            @PathVariable("id")
            @Pattern(regexp = "^[0-9]+$", message = "Post ID must be numeric")
            @Min(value = 1, message = "Post ID must be greater than 0") final String postId) {
            auditionLogger.debug(LOGGER, String.format("Received request to fetch post with id={}", postId));
        try {
            final AuditionPost auditionPost = auditionService.getPostById(postId);
            LOGGER.info("Successfully retrieved post with id={}", postId);
            return ResponseEntity.ok(auditionPost);

        } catch (final SystemException ex) {
                auditionLogger.error(LOGGER, String.format("Error fetching post with id={}: {}", postId, ex.getMessage(), ex));
            final HttpStatus status = ex.getStatusCode() != null
                    ? HttpStatus.valueOf(ex.getStatusCode())
                    : HttpStatus.INTERNAL_SERVER_ERROR;

            return ResponseEntity
                    .status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(null);
        }
    }

    @GetMapping(value = "/{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AuditionComment>> getCommentsForPost(
            @PathVariable("id")
            @Pattern(regexp = "^[0-9]+$", message = "Post ID must be numeric")
            @Min(value = 1, message = "Post ID must be greater than 0") final String postId) {
        auditionLogger.debug(LOGGER, String.format("Fetching comments for post id={}", postId));
        return ResponseEntity.ok(auditionService.getCommentsForPost(postId));
    }

    @GetMapping(value = "/{id}/comments/query", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AuditionComment>> getCommentsByPostQuery(
            @PathVariable("id")
            @Pattern(regexp = "^[0-9]+$", message = "Post ID must be numeric")
            @Min(value = 1, message = "Post ID must be greater than 0") final String postId) {
        auditionLogger.debug(LOGGER, String.format("Fetching comments via query param for post id={}", postId));
        return ResponseEntity.ok(auditionService.getCommentsByQuery(postId));
    }
}
