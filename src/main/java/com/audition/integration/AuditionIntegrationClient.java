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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "RestTemplate and AuditionLogger are Spring-managed beans injected by the container, not externally mutable objects"
)
public class AuditionIntegrationClient {

    private final RestTemplate restTemplate;
    private final AuditionLogger auditionLogger;

    @Value("${audition_service_url}")
    private String url;

    @CircuitBreaker(name = "auditionService", fallbackMethod = "getPostsFallback")
    public List<AuditionPost> getPosts(final String userId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url + "/posts");

        Optional.ofNullable(userId)
            .filter(id -> !id.isBlank())
            .ifPresent(id -> builder.queryParam("userId", id));

        URI uri = builder.encode().build().toUri();

        ResponseEntity<List<AuditionPost>> response =
            restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

        return Optional.ofNullable(response.getBody()).orElse(List.of());
    }

    @CircuitBreaker(name = "auditionService", fallbackMethod = "getPostByIdFallback")
    public AuditionPost getPostById(final String id) {
        try {
            return restTemplate.getForObject(url + "/posts/{id}", AuditionPost.class, id);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new SystemException("Cannot find a Post with id " + id, "Resource Not Found", 404);
            }
            throw new SystemException(e.getResponseBodyAsString(), "External Service Error", e.getStatusCode().value());
        } catch (Exception e) {
            throw new SystemException(e.getMessage(), "Unexpected Error", 500);
        }
    }

    @CircuitBreaker(name = "auditionService", fallbackMethod = "getPostWithCommentsFallback")
    public AuditionPost getPostWithComments(final String postId) {

        CompletableFuture<AuditionPost> postFuture = CompletableFuture
            .supplyAsync(() -> getPostById(postId));

        CompletableFuture<List<AuditionComment>> commentsFuture = CompletableFuture
            .supplyAsync(() -> getCommentsByPostId(postId));

        try {
            return postFuture
                .thenCombine(commentsFuture, AuditionPost::withComments)
                .join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SystemException) {
                throw (SystemException) cause;
            }
            throw new SystemException(cause.getMessage(), "Unexpected Error", 500);
        }
    }

    @CircuitBreaker(name = "auditionService", fallbackMethod = "getCommentsByPostIdFallback")
    public List<AuditionComment> getCommentsByPostId(final String postId) {
        URI uri = UriComponentsBuilder
            .fromUriString(url)
            .path("/posts/{postId}/comments")
            .buildAndExpand(postId)
            .encode()
            .toUri();

        ResponseEntity<List<AuditionComment>> response =
            restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

        return Optional.ofNullable(response.getBody()).orElse(List.of());
    }

    @CircuitBreaker(name = "auditionService", fallbackMethod = "getCommentsFallback")
    public List<AuditionComment> getComments(final String postId) {
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(url)
            .path("/comments");

        Optional.ofNullable(postId)
            .filter(id -> !id.isBlank())
            .ifPresent(id -> builder.queryParam("postId", id));

        URI uri = builder.encode().build().toUri();

        ResponseEntity<List<AuditionComment>> response =
            restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

        return Optional.ofNullable(response.getBody()).orElse(List.of());
    }

    private List<AuditionPost> getPostsFallback(Throwable ex) {
        auditionLogger.warn(log, "Circuit breaker triggered for getPosts, reason= " + ex.getMessage());
        return List.of();
    }

    private AuditionPost getPostByIdFallback(String id, Throwable ex) {
        auditionLogger.warn(log, "Circuit breaker triggered for getPostById id= " + id + ", reason= " + ex.getMessage());
        throw new SystemException("Service unavailable for post id " + id, "Service Unavailable", 503);
    }

    private AuditionPost getPostWithCommentsFallback(String postId, Throwable ex) {
        auditionLogger.warn(log, "Circuit breaker triggered for getPostWithComments postId= " + postId + ", reason= " + ex.getMessage());
        throw new SystemException("Service unavailable for post id " + postId, "Service Unavailable", 503);
    }

    private List<AuditionComment> getCommentsByPostIdFallback(String postId, Throwable ex) {
        auditionLogger.warn(log, "Circuit breaker triggered for getCommentsByPostId postId= " + postId + ", reason= " + ex.getMessage());
        return List.of();
    }

    private List<AuditionComment> getCommentsFallback(String postId, Throwable ex) {
        auditionLogger.warn(log, "Circuit breaker triggered for getComments postId= " + postId + ", reason= " + ex.getMessage());
        return List.of();
    }
}