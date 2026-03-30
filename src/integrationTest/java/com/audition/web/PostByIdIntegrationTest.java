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

class PostByIdIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    public PostByIdIntegrationTest(final TestRestTemplate restTemplate) {
        super(restTemplate);
    }

    @Test
    void postByIdReturnsExpectedPost() throws JSONException, IOException {
        final ResponseEntity<String> response = restTemplate.getForEntity("/posts/1", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final String expected = loadResource("response-post-by-post-id-1.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.LENIENT);
    }

    @Test
    void postByIdWithNonExistentIdReturns404() {
        final ResponseEntity<String> response = restTemplate.getForEntity("/posts/99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Cannot find a Post");
    }

    @Test
    void postByIdWith403ReturnsForbiddenAndPreservesErrorMessage() {
        final ResponseEntity<String> response = restTemplate.getForEntity("/posts/403", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("Forbidden: insufficient permissions");
    }

    @Test
    void postByIdWith500ReturnsInternalServerError() {
        final ResponseEntity<String> response = restTemplate.getForEntity("/posts/500", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("Internal Server Error: database unavailable");
    }
}
