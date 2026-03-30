package com.audition.integration;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.GuardLogStatement") // AuditionLogger already checks log level before invoking the logger
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "RestTemplate and AuditionLogger are Spring-managed beans injected by the container, not externally mutable objects"
)
public class AuditionIntegrationClient {

    private static final String AUDITION_SERVICE = "auditionService";
    private static final String PATH_POSTS = "/posts";
    private static final String PATH_COMMENTS = "/comments";
    private static final String PATH_POST_COMMENTS = "/posts/{postId}/comments";
    private static final String PARAM_USER_ID = "userId";
    private static final String PARAM_POST_ID = "postId";

    private final RestTemplate restTemplate;
    private final AuditionLogger auditionLogger;

    @Value("${audition_service_url}")
    private String baseUrl;

    @CircuitBreaker(name = AUDITION_SERVICE, fallbackMethod = "getPostsFallback")
    public List<AuditionPost> getPosts(final String userId) {

        auditionLogger.debug(log, "Retrieving posts for userId=" + userId);

        final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + PATH_POSTS);

        Optional.ofNullable(userId)
            .filter(StringUtils::isNotBlank)
            .ifPresent(id -> builder.queryParam(PARAM_USER_ID, id));

        final URI uri = builder.encode().build().toUri();

        try {
            final ResponseEntity<List<AuditionPost>> response =
                restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            return Optional.ofNullable(response.getBody()).orElse(List.of());

        } catch (RestClientException e) {
            auditionLogger.error(log, "Unexpected error occurs when retrieving posts for userId= " + userId);
            throw toSystemException(e);
        }
    }

    @CircuitBreaker(name = AUDITION_SERVICE, fallbackMethod = "getPostByIdFallback")
    public AuditionPost getPostById(final String id) {

        auditionLogger.debug(log, "Retrieving post by post id=" + id);

        try {
            final AuditionPost post = restTemplate.getForObject(baseUrl + PATH_POSTS + "/{id}", AuditionPost.class, id);
            if (post == null) {
                auditionLogger.error(log, "Post with id= " + id + " not found in response");
                throw new SystemException("Cannot find a Post with id " + id, "Resource Not Found", 404);
            }
            return post;
        } catch (HttpClientErrorException e) {
            auditionLogger.error(log, "Client error occurs when retrieving post for id= " + id + ", status code= " + e.getStatusCode());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new SystemException("Cannot find a Post with id " + id, "Resource Not Found", 404, e);
            }
            throw toSystemException(e);
        } catch (RestClientException e) {
            auditionLogger.error(log, "Unexpected error occurs when retrieving post for id= " + id);
            throw toSystemException(e);
        }
    }

    @CircuitBreaker(name = AUDITION_SERVICE, fallbackMethod = "getPostWithCommentsFallback")
    public AuditionPost getPostWithComments(final String postId) {
        auditionLogger.debug(log, "Retrieving post with comments by post id=" + postId);
        final CompletableFuture<AuditionPost> postFuture = CompletableFuture
            .supplyAsync(() -> getPostById(postId));

        final CompletableFuture<List<AuditionComment>> commentsFuture = CompletableFuture
            .supplyAsync(() -> getCommentsByPostId(postId));

        try {
            return postFuture
                .thenCombine(commentsFuture, AuditionPost::withComments)
                .join();
        } catch (CompletionException e) {
            auditionLogger.error(log, "Unexpected error occurs when retrieving post with comments for postId= " + postId);
            final Throwable cause = e.getCause();
            if (cause instanceof SystemException se) {
                throw se;
            }
            throw new SystemException(cause.getMessage(), "Unexpected Error", 500, e);
        }
    }

    @CircuitBreaker(name = AUDITION_SERVICE, fallbackMethod = "getCommentsByPostIdFallback")
    public List<AuditionComment> getCommentsByPostId(final String postId) {

        auditionLogger.debug(log, "Retrieving comments by post id=" + postId);

        final URI uri = UriComponentsBuilder
            .fromUriString(baseUrl)
            .path(PATH_POST_COMMENTS)
            .buildAndExpand(postId)
            .encode()
            .toUri();

        return getAuditionComments(uri);
    }

    @CircuitBreaker(name = AUDITION_SERVICE, fallbackMethod = "getCommentsFallback")
    public List<AuditionComment> getComments(final String postId) {

        auditionLogger.debug(log, "Retrieving comments with optional postId=" + postId);

        final UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(baseUrl)
            .path(PATH_COMMENTS);

        Optional.ofNullable(postId)
            .filter(StringUtils::isNotBlank)
            .ifPresent(id -> builder.queryParam(PARAM_POST_ID, id));

        final URI uri = builder.encode().build().toUri();

        return getAuditionComments(uri);
    }

    @NonNull
    private List<AuditionComment> getAuditionComments(final URI uri) {
        try {
            final ResponseEntity<List<AuditionComment>> response =
                restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            return Optional.ofNullable(response.getBody()).orElse(List.of());
        } catch (RestClientException e) {
            auditionLogger.error(log, "Unexpected error occurs when retrieving comments from uri= " + uri);
            throw toSystemException(e);
        }
    }

    private SystemException toSystemException(final RestClientException e) {
        if (e instanceof HttpClientErrorException hce) {
            return new SystemException(hce.getResponseBodyAsString(), "Client Error", hce.getStatusCode().value(), hce);
        }
        if (e instanceof HttpServerErrorException hse) {
            return new SystemException(hse.getResponseBodyAsString(), "External Service Error", hse.getStatusCode().value(), hse);
        }
        return new SystemException(e.getMessage(), "Service Unavailable", 503, e);
    }

    List<AuditionPost> getPostsFallback(final Throwable ex) {
        auditionLogger.warn(log, "Circuit breaker triggered for getPosts, " + getFallbackLogReason(ex));
        return List.of();
    }

    AuditionPost getPostByIdFallback(final String id, final Throwable ex) {
        auditionLogger.warn(log, "Circuit breaker triggered for getPostById id= " + id + getFallbackLogReason(ex));
        throw new SystemException("Service unavailable for post id " + id, "Service Unavailable", 503);
    }

    AuditionPost getPostWithCommentsFallback(final String postId, final Throwable ex) {
        auditionLogger.warn(log,
            "Circuit breaker triggered for getPostWithComments postId= " + postId + getFallbackLogReason(ex));
        throw new SystemException("Service unavailable for post id " + postId, "Service Unavailable", 503);
    }

    List<AuditionComment> getCommentsByPostIdFallback(final String postId, final Throwable ex) {
        auditionLogger.warn(log,
            "Circuit breaker triggered for getCommentsByPostId postId= " + postId + getFallbackLogReason(ex));
        return List.of();
    }

    List<AuditionComment> getCommentsFallback(final String postId, final Throwable ex) {
        auditionLogger.warn(log, "Circuit breaker triggered for getComments postId= " + postId + getFallbackLogReason(ex));
        return List.of();
    }

    private String getFallbackLogReason(final Throwable ex) {
        return ", reason= " + ex.getMessage();
    }
}