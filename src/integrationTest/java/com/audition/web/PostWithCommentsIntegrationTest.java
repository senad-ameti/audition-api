package com.audition.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PostWithCommentsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    public PostWithCommentsIntegrationTest(TestRestTemplate restTemplate) {
        super(restTemplate);
    }

    @Test
    void getPostWithComments_returnsPostWithEmbeddedComments() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/posts/1/comments", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String expected = loadResource("response-post-with-comments-by-post-id-1.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.LENIENT);
    }

    @Test
    void getPostWithComments_withNonExistentId_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/posts/99999/comments", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Cannot find a Post");
    }
}
