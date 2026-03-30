package com.audition.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PostsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    public PostsIntegrationTest(final TestRestTemplate restTemplate) {
        super(restTemplate);
    }

    static Stream<Arguments> postFilterCases() {
        return Stream.of(
            Arguments.of(null, null, "response-all-posts.json"),
            Arguments.of("1", null, "response-post-by-userid-1.json"),
            Arguments.of(null, "vero", "response-post-by-title-search-vero.json")
        );
    }

    @ParameterizedTest(name = "userId={0} title={1}")
    @MethodSource("postFilterCases")
    void postsReturnsExpectedJson(final String userId, final String title, final String responseFile)
        throws JSONException, IOException {
        final StringBuilder url = new StringBuilder("/posts?");
        if (userId != null) {
            url.append("userId=").append(userId).append('&');
        }
        if (title != null) {
            url.append("title=").append(title).append('&');
        }

        final ResponseEntity<String> response = restTemplate.getForEntity(url.toString(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final String expected = loadResource(responseFile);
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.LENIENT);
    }

    @Test
    void postsWithTitleTooShortReturns400() {
        final ResponseEntity<String> response = restTemplate.getForEntity("/posts?title=vr", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("3 characters");
    }

    @Test
    void postsWithInvalidMethodReturns405() {
        final ResponseEntity<String> response = restTemplate.postForEntity("/posts", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }
}
