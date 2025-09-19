package com.audition.integration;

import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditionControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl(final String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void postShouldReturnsAllPosts() {
        final ResponseEntity<List<AuditionPost>> response =
                restTemplate.exchange(
                        baseUrl("/posts"),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void postShouldReturnsSinglePost() {
        final ResponseEntity<AuditionPost> response =
                restTemplate.getForEntity(baseUrl("/posts/1"), AuditionPost.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1);
    }

    @Test
    void postShouldReturnsComments() {
        final ResponseEntity<List<AuditionComment>> response =
                restTemplate.exchange(
                        baseUrl("/posts/1/comments"),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get(0).getPostId()).isEqualTo(1);
    }

    @Test
    void postShouldReturnsCommentsWithQuery() {
        final ResponseEntity<List<AuditionComment>> response =
                restTemplate.exchange(
                        baseUrl("/posts/1/comments/query"),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get(0).getPostId()).isEqualTo(1);
    }
}
