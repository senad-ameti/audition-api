package com.audition.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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
        ReflectionTestUtils.setField(client, "baseUrl", AuditionIntegrationClientPostTest.BASE_URL);
    }

    @Test
    void postByIdReturnsPost() {
        when(restTemplate.getForObject(AuditionIntegrationClientPostTest.BASE_URL + POSTS_BY_ID_PATH, AuditionPost.class, "1"))
            .thenReturn(mockPost);

        assertEquals(mockPost, client.getPostById("1"));
    }

    @Test
    void postByIdOn404ThrowsSystemException404() {
        when(restTemplate.getForObject(AuditionIntegrationClientPostTest.BASE_URL + POSTS_BY_ID_PATH, AuditionPost.class, "999"))
            .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));

        final SystemException ex = assertThrows(SystemException.class, () -> client.getPostById("999"));
        assertTrue(ex.getMessage().contains("999"));
        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void postByIdOnNullResponseThrowsSystemException404() {
        when(restTemplate.getForObject(AuditionIntegrationClientPostTest.BASE_URL + POSTS_BY_ID_PATH, AuditionPost.class, "1"))
            .thenReturn(null);

        final SystemException ex = assertThrows(SystemException.class, () -> client.getPostById("1"));
        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void postByIdOnClientErrorThrowsSystemExceptionWithOriginalStatus() {
        when(restTemplate.getForObject(AuditionIntegrationClientPostTest.BASE_URL + POSTS_BY_ID_PATH, AuditionPost.class, "1"))
            .thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY, new byte[0], null));

        final SystemException ex = assertThrows(SystemException.class, () -> client.getPostById("1"));
        assertEquals(403, ex.getStatusCode());
    }

    @Test
    void postByIdOnServerErrorThrowsSystemExceptionWithOriginalStatus() {
        when(restTemplate.getForObject(AuditionIntegrationClientPostTest.BASE_URL + POSTS_BY_ID_PATH, AuditionPost.class, "1"))
            .thenThrow(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders.EMPTY, new byte[0], null));

        final SystemException ex = assertThrows(SystemException.class, () -> client.getPostById("1"));
        assertEquals(503, ex.getStatusCode());
    }

    @Test
    void postByIdOnNetworkErrorThrowsSystemException503() {
        when(restTemplate.getForObject(AuditionIntegrationClientPostTest.BASE_URL + POSTS_BY_ID_PATH, AuditionPost.class, "1"))
            .thenThrow(new ResourceAccessException("Connection refused"));

        final SystemException ex = assertThrows(SystemException.class, () -> client.getPostById("1"));
        assertTrue(ex.getMessage().contains("Connection refused"));
        assertEquals(503, ex.getStatusCode());
    }

    @Test
    void postByIdFallbackLogsAndThrows() {
        final SystemException thrown = assertThrows(SystemException.class,
            () -> ReflectionTestUtils.invokeMethod(
                client, "getPostByIdFallback", "1", new RuntimeException("circuit open")));
        assertEquals(503, thrown.getStatusCode());
        verify(auditionLogger).warn(any(), any());
    }
}
