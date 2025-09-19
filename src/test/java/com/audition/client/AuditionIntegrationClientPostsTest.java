package com.audition.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;


import static org.mockito.Mockito.any;


import java.util.List;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionPost;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuditionIntegrationClientPostsTest {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";
    private static final String POSTS_PATH = BASE_URL + "/posts";
    private static final String POST_BY_ID_PATH = BASE_URL + "/posts/{id}";
    private static final String POST_ID = "2";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuditionLogger auditionLogger;

    @InjectMocks
    private AuditionIntegrationClient client;

    // ----------------- getPosts -----------------

    @Test
    void shouldReturnPostsSuccessfully() {
        final AuditionPost post = new AuditionPost();
        post.setId(1);
        final ResponseEntity<List<AuditionPost>> resp = ResponseEntity.ok(List.of(post));

        when(restTemplate.exchange(eq(POSTS_PATH), eq(HttpMethod.GET), isNull(),
                any(ParameterizedTypeReference.class))).thenReturn(resp);

        final List<AuditionPost> result = client.getPosts();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    void shouldReturnEmptyListWhenPostsAreNull() {
        final ResponseEntity<List<AuditionPost>> resp = ResponseEntity.ok(null);
        when(restTemplate.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(resp);

        final List<AuditionPost> result = client.getPosts();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void shouldThrowSystemExceptionForPostsExceptions() {
        testGetPostsException(new HttpClientErrorException(HttpStatus.BAD_REQUEST), "downstream client");
        testGetPostsException(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR), "server");
        testGetPostsException(new ResourceAccessException("unreachable"), "unreachable");
        testGetPostsException(new RestClientException("oops"), "unexpected");
    }

    private void testGetPostsException(final RestClientException cause, final String expectedMessagePart) {
        when(restTemplate.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class)))
                .thenThrow(cause);

        final SystemException thrown = assertThrows(SystemException.class, client::getPosts);
        assertTrue(thrown.getMessage().toLowerCase().contains(expectedMessagePart));
        assertEquals(cause, thrown.getCause());
    }

    // ----------------- getPostById -----------------

    @Test
    void shouldReturnPostByIdSuccessfully() {
        final AuditionPost post = new AuditionPost();
        post.setId(2);

        when(restTemplate.getForObject(eq(POST_BY_ID_PATH), eq(AuditionPost.class), eq(POST_ID)))
                .thenReturn(post);

        final AuditionPost result = client.getPostById(POST_ID);
        assertNotNull(result);
        assertEquals(2, result.getId());
    }

    @Test
    void shouldThrowSystemExceptionWhenPostByIdNullOrNotFound() {
        when(restTemplate.getForObject(anyString(), eq(AuditionPost.class), anyString()))
                .thenReturn(null);
        final SystemException thrownNull = assertThrows(SystemException.class, () -> client.getPostById("100"));
        assertTrue(thrownNull.getMessage().toLowerCase().contains("post not found"));
        assertNull(thrownNull.getCause());

        testGetPostByIdException(new HttpClientErrorException(HttpStatus.NOT_FOUND), "post not found");
        testGetPostByIdException(new HttpClientErrorException(HttpStatus.BAD_REQUEST), "downstream client");
        testGetPostByIdException(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR), "downstream server");
        testGetPostByIdException(new ResourceAccessException("conn"), "downstream unreachable");
        testGetPostByIdException(new RestClientException("err"), "unexpected");
    }

    private void testGetPostByIdException(final RestClientException cause, final String expectedMessagePart) {
        when(restTemplate.getForObject(anyString(), eq(AuditionPost.class), anyString()))
                .thenThrow(cause);

        final SystemException thrown = assertThrows(SystemException.class, () -> client.getPostById("x"));
        assertTrue(thrown.getMessage().toLowerCase().contains(expectedMessagePart));
        assertEquals(cause, thrown.getCause());
    }
}
