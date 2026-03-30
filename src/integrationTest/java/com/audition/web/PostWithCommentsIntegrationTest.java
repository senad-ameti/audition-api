package com.audition.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PostWithCommentsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    public PostWithCommentsIntegrationTest(final TestRestTemplate restTemplate) {
        super(restTemplate);
    }

    @Test
    void postWithCommentsReturnsPostWithEmbeddedComments() throws JSONException, IOException {
        final ResponseEntity<String> response = restTemplate.getForEntity("/posts/1/comments", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final String expected = loadResource("response-post-with-comments-by-post-id-1.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.LENIENT);
    }

    @Test
    void postWithCommentsWithNonExistentIdReturns404() {
        final ResponseEntity<String> response = restTemplate.getForEntity("/posts/99999/comments", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Cannot find a Post");
    }

    @Test
    void postWithCommentsWhenDownstreamReturns500ReturnsError() {
        final ResponseEntity<String> response = restTemplate.getForEntity("/posts/500/comments", String.class);

        assertThat(response.getStatusCode().value()).isGreaterThanOrEqualTo(500);
    }
}
