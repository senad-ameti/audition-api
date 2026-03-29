package com.audition.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CommentsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    public CommentsIntegrationTest(TestRestTemplate restTemplate) {
        super(restTemplate);
    }

    @Test
    void getComments_withPostId_returnsExpectedComments() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/comments?postId=1", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String expected = loadResource("response-comments-for-post-id-1.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.LENIENT);
    }

    @Test
    void getComments_withoutPostId_returnsNonEmptyList() {
        ResponseEntity<String> response = restTemplate.getForEntity("/comments", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).startsWith("[");
        assertThat(response.getBody()).contains("postId");
    }
}
