package com.audition.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PostByIdIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    public PostByIdIntegrationTest(TestRestTemplate restTemplate) {
        super(restTemplate);
    }

    @Test
    void getPostById_returnsExpectedPost() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/posts/1", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String expected = loadResource("response-post-by-post-id-1.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.LENIENT);
    }

    @Test
    void getPostById_withNonExistentId_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/posts/99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Cannot find a Post");
    }
}
