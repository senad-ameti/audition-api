package com.audition.client;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement")
public class AuditionIntegrationClient {

    private static final Logger LOG = LoggerFactory.getLogger(AuditionIntegrationClient.class);
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    private final RestTemplate restTemplate;
    private final AuditionLogger auditionLogger;

    // ----------------- POSTS -----------------

    public List<AuditionPost> getPosts() {
        final String url = BASE_URL + "/posts";
        auditionLogger.info(LOG, "Calling upstream: GET {}", url);

        final ResponseEntity<List<AuditionPost>> response = executeRestCall(
                () -> restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {}),
                "posts"
        );

        final List<AuditionPost> posts = Optional.ofNullable(response.getBody()).orElse(Collections.emptyList());
        auditionLogger.info(LOG, "Upstream returned {} posts", posts.size());
        return posts;
    }

    public AuditionPost getPostById(final String id) {
        final String url = BASE_URL + "/posts/{id}";
        auditionLogger.info(LOG, "Calling upstream: GET {}", url.replace("{id}", id));

        final AuditionPost post = executeRestCall(() -> restTemplate.getForObject(url, AuditionPost.class, id), "post");

        if (post == null) {
            throw new SystemException("Post not found", HttpStatus.NOT_FOUND.value(), null);
        }

        auditionLogger.info(LOG, "Upstream returned post id={}", post.getId());
        return post;
    }

    // ----------------- COMMENTS -----------------

    public List<AuditionComment> getCommentsByPostId(final String postId) {
        final String url = BASE_URL + "/posts/{id}/comments";
        auditionLogger.info(LOG, "Calling upstream: GET {}", url.replace("{id}", postId));

        final AuditionComment[] arr = executeRestCall(() -> restTemplate.getForObject(url, AuditionComment[].class, postId), "comments");

        final List<AuditionComment> comments = Optional.ofNullable(arr).map(List::of).orElse(Collections.emptyList());
        auditionLogger.info(LOG, "Upstream returned {} comments for post {}", comments.size(), postId);
        return comments;
    }

    public List<AuditionComment> getCommentsByQuery(final String postId) {
        final String url = BASE_URL + "/comments?postId={id}";
        auditionLogger.info(LOG, "Calling upstream: GET {}", url.replace("{id}", postId));

        final ResponseEntity<List<AuditionComment>> response = executeRestCall(
                () -> restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {}, postId),
                "comments"
        );

        final List<AuditionComment> comments = Optional.ofNullable(response.getBody()).orElse(Collections.emptyList());
        auditionLogger.info(LOG, "Upstream returned {} comments (query) for post {}", comments.size(), postId);
        return comments;
    }

    // ----------------- HELPER -----------------

    private <T> T executeRestCall(final Supplier<T> call, final String type) {
        try {
            return call.get();
        } catch (RestClientException e) {
            throw translateRestException(e, type);
        }
    }

    private SystemException translateRestException(final RestClientException e, final String type) {
        if (e instanceof HttpClientErrorException clientError) {
            if (clientError.getStatusCode() == HttpStatus.NOT_FOUND && ("post".equals(type) || "comments".equals(type))) {
                auditionLogger.warn(LOG, "Upstream 404 for {}" + type);
                return new SystemException(capitalize(type) + " not found", 404, clientError);
            }
            auditionLogger.logErrorWithException(LOG, "Upstream 4xx fetching " + type, clientError);
            return new SystemException("Downstream client error fetching " + type, clientError.getStatusCode().value(), clientError);
        } else if (e instanceof HttpServerErrorException serverError) {
            auditionLogger.logErrorWithException(LOG, "Upstream 5xx fetching " + type, serverError);
            return new SystemException("Downstream server error fetching " + type, HttpStatus.BAD_GATEWAY.value(), serverError);
        } else if (e instanceof ResourceAccessException resourceError) {
            auditionLogger.logErrorWithException(LOG, "Upstream unreachable fetching " + type, resourceError);
            return new SystemException("Downstream unreachable", HttpStatus.SERVICE_UNAVAILABLE.value(), resourceError);
        } else {
            auditionLogger.logErrorWithException(LOG, "Unexpected RestClientException fetching " + type, e);
            return new SystemException("Unexpected downstream error fetching " + type, HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    private String capitalize(final String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

}
