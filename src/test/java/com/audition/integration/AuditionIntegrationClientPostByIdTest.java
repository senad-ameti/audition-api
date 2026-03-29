package com.audition.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AuditionIntegrationClientPostByIdTest {

    @Mock
    private RestTemplate restTemplate;

    @SuppressWarnings("PMD.UnusedPrivateField")
    @Mock
    private AuditionLogger auditionLogger;

    @InjectMocks
    private AuditionIntegrationClient client;

    private static final String POSTS_BY_ID_PATH = "/posts/{id}";

    private final AuditionPost mockPost = new AuditionPost(1, 1, "title", "body");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "url", AuditionIntegrationClientTest.BASE_URL);
    }

    @Test
    void postByIdReturnsPost() {
        when(restTemplate.getForObject(AuditionIntegrationClientTest.BASE_URL + POSTS_BY_ID_PATH, AuditionPost.class, "1"))
            .thenReturn(mockPost);

        assertThat(client.getPostById("1")).isEqualTo(mockPost);
    }

    @Test
    void postByIdOn404ThrowsSystemException404() {
        when(restTemplate.getForObject(AuditionIntegrationClientTest.BASE_URL + POSTS_BY_ID_PATH, AuditionPost.class, "999"))
            .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));

        assertThatThrownBy(() -> client.getPostById("999"))
            .isInstanceOf(SystemException.class)
            .hasMessageContaining("999")
            .matches(e -> ((SystemException) e).getStatusCode() == 404);
    }

    @Test
    void postByIdOnOtherHttpErrorThrowsSystemException() {
        when(restTemplate.getForObject(AuditionIntegrationClientTest.BASE_URL + POSTS_BY_ID_PATH, AuditionPost.class, "1"))
            .thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY, new byte[0], null));

        assertThatThrownBy(() -> client.getPostById("1"))
            .isInstanceOf(SystemException.class)
            .matches(e -> ((SystemException) e).getStatusCode() == 403);
    }

    @Test
    void postByIdOnGenericExceptionThrowsSystemException500() {
        when(restTemplate.getForObject(AuditionIntegrationClientTest.BASE_URL + POSTS_BY_ID_PATH, AuditionPost.class, "1"))
            .thenThrow(new RuntimeException("network error"));

        assertThatThrownBy(() -> client.getPostById("1"))
            .isInstanceOf(SystemException.class)
            .matches(e -> ((SystemException) e).getStatusCode() == 500);
    }
}
