package com.audition.web;

import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostFilter;
import com.audition.service.AuditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Posts", description = "Retrieve posts and comments from the audition service")
public class AuditionController {

    private static final String STATUS_200 = "200";
    private static final String STATUS_400 = "400";
    private static final String STATUS_404 = "404";
    private static final String STATUS_500 = "500";
    private static final String STATUS_503 = "503";

    private static final String MSG_SUCCESS_POSTS = "Posts retrieved successfully";
    private static final String MSG_SUCCESS_POST = "Post found";
    private static final String MSG_SUCCESS_POST_COMMENTS = "Post with comments found";
    private static final String MSG_SUCCESS_COMMENTS = "Comments retrieved successfully";
    private static final String MSG_INTERNAL_ERROR = "Unexpected internal error";
    private static final String MSG_POST_NOT_FOUND = "Post not found";
    private static final String MSG_POST_ID_BLANK = "Post ID is blank";
    private static final String MSG_CIRCUIT_BREAKER = "Downstream service unavailable (circuit breaker open)";

    private final AuditionService auditionService;

    @Operation(summary = "Get all posts", description = "Returns all posts, optionally filtered by userId and/or title substring (min 3 characters).")
    @ApiResponses({
        @ApiResponse(responseCode = STATUS_200, description = MSG_SUCCESS_POSTS),
        @ApiResponse(responseCode = STATUS_400, description = "title filter is shorter than 3 characters",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = STATUS_500, description = MSG_INTERNAL_ERROR,
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/posts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuditionPost> getPosts(@Valid final AuditionPostFilter auditionPostFilter) {
        return auditionService.getPosts(auditionPostFilter);
    }

    @Operation(summary = "Get post by ID", description = "Returns a single post by its numeric ID.")
    @ApiResponses({
        @ApiResponse(responseCode = STATUS_200, description = MSG_SUCCESS_POST),
        @ApiResponse(responseCode = STATUS_400, description = MSG_POST_ID_BLANK,
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = STATUS_404, description = MSG_POST_NOT_FOUND,
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = STATUS_500, description = MSG_INTERNAL_ERROR,
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = STATUS_503, description = MSG_CIRCUIT_BREAKER,
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/posts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuditionPost getPostById(
        @Parameter(description = "Numeric post ID", example = "1", required = true)
        @PathVariable("id")
        @NotBlank(message = "Post id cannot be blank")
        final String postId) {
        return auditionService.getPostById(postId);
    }

    @Operation(summary = "Get post with comments", description = "Returns a post with its comments embedded, fetched concurrently.")
    @ApiResponses({
        @ApiResponse(responseCode = STATUS_200, description = MSG_SUCCESS_POST_COMMENTS),
        @ApiResponse(responseCode = STATUS_400, description = MSG_POST_ID_BLANK,
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = STATUS_404, description = MSG_POST_NOT_FOUND,
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = STATUS_500, description = MSG_INTERNAL_ERROR,
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = STATUS_503, description = MSG_CIRCUIT_BREAKER,
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/posts/{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuditionPost getPostWithComments(
        @Parameter(description = "Numeric post ID", example = "1", required = true)
        @PathVariable("id")
        @NotBlank(message = "Post id cannot be blank")
        final String postId) {
        return auditionService.getPostWithComments(postId);
    }

    @Operation(summary = "Get comments", description = "Returns all comments, or comments for a specific post when postId is provided.")
    @ApiResponses({
        @ApiResponse(responseCode = STATUS_200, description = MSG_SUCCESS_COMMENTS),
        @ApiResponse(responseCode = STATUS_500, description = MSG_INTERNAL_ERROR,
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuditionComment> getComments(
        @Parameter(description = "Filter comments by post ID", example = "1")
        @RequestParam(value = "postId", required = false) final String postId) {
        return auditionService.getComments(postId);
    }
}
