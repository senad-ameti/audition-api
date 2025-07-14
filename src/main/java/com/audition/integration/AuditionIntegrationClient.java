package com.audition.integration;

import com.audition.common.exception.SystemException;
import com.audition.model.AuditionPost;
import com.audition.model.PostComment;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "I prefer to suppress these FindBugs warnings"
)
@Component
public class AuditionIntegrationClient {

    @Value("${audition.service.url}")
    private String serviceUrl;

    private final RestTemplate restTemplate;

    public AuditionIntegrationClient(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<AuditionPost> getPosts(final Integer userId) {
        String url = serviceUrl + "/posts";
        if (userId != null) {
            url += "?userId=" + userId;
        }

        try {
            final ResponseEntity<List<AuditionPost>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
            );

            return responseEntity.getBody();
        } catch (final HttpClientErrorException | HttpServerErrorException e) {
            throw new SystemException("Fetching Posts with userId " + userId, e.getStatusCode().value(), e);
        } catch (final RestClientException e) {
            throw new SystemException("Fetching Posts with userId " + userId, 500, e);
        }
    }

    public AuditionPost getPostById(final Integer id) {
        try {
            final ResponseEntity<AuditionPost> responseEntity = restTemplate.getForEntity(serviceUrl + "/posts/" + id,
                AuditionPost.class);

            return responseEntity.getBody();
        } catch (final HttpClientErrorException | HttpServerErrorException e) {
            throw new SystemException("Fetching a Post with id " + id, e.getStatusCode().value(), e);
        } catch (final RestClientException e) {
            throw new SystemException("Fetching a Post with id " + id, 500, e);
        }
    }

    public List<PostComment> getCommentsByPostId(final Integer postId) {
        try {
            final ResponseEntity<List<PostComment>> responseEntity = restTemplate.exchange(
                serviceUrl + "/comments?postId=" + postId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
            );

            return responseEntity.getBody();
        } catch (final HttpClientErrorException | HttpServerErrorException e) {
            throw new SystemException("Fetching comments for Post with id " + postId, e.getStatusCode().value(), e);
        } catch (final RestClientException e) {
            throw new SystemException("Fetching comments for Post with id " + postId, 500, e);
        }
    }
}
