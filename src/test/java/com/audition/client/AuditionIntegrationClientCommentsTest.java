package com.audition.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.any;
import java.util.List;
import java.util.stream.Stream;
import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionComment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("PMD.ExcessiveImports")
class AuditionIntegrationClientCommentsTest {

    @InjectMocks
    private AuditionIntegrationClient client;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuditionLogger auditionLogger;

    private static final String POST_ID = "123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ========================= getCommentsByPostId =========================

    @Test
    void returnCommentsWhenGetCommentsByPostIdSucceeds() {
        final AuditionComment[] comments = { new AuditionComment(), new AuditionComment() };
        when(restTemplate.getForObject(anyString(), eq(AuditionComment[].class), anyString()))
                .thenReturn(comments);

        final List<AuditionComment> result = client.getCommentsByPostId(POST_ID);

        assertEquals(2, result.size());
    }

    @Test
    void returnEmptyListWhenGetCommentsByPostIdReturnsNull() {
        when(restTemplate.getForObject(anyString(), eq(AuditionComment[].class), anyString()))
                .thenReturn(null);

        final List<AuditionComment> result = client.getCommentsByPostId(POST_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideExceptionsForPostId")
    void throwSystemExceptionForGetCommentsByPostId(final Exception ex, final int expectedStatus) {
        when(restTemplate.getForObject(anyString(), eq(AuditionComment[].class), anyString()))
                .thenThrow(ex);

        final SystemException thrown = assertThrows(SystemException.class,
                () -> client.getCommentsByPostId(POST_ID));

        assertEquals(expectedStatus, thrown.getStatusCode());
    }

    private static Stream<Object[]> provideExceptionsForPostId() {
        return Stream.of(
                new Object[]{HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null), 404},
                new Object[]{HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null), 400},
                new Object[]{HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", null, null, null), 502},
                new Object[]{new ResourceAccessException("Resource unreachable"), 503},
                new Object[]{new RestClientException("Unexpected error"), 500}
        );
    }

    // ========================= getCommentsByQuery =========================

    @Test
    void returnCommentsWhenGetCommentsByQuerySucceeds() {
        final List<AuditionComment> comments = List.of(new AuditionComment(), new AuditionComment());
        final ResponseEntity<List<AuditionComment>> response = new ResponseEntity<>(comments, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
                any(ParameterizedTypeReference.class), anyString())).thenReturn(response);

        final List<AuditionComment> result = client.getCommentsByQuery(POST_ID);

        assertEquals(2, result.size());
    }

    @Test
    void returnEmptyListWhenGetCommentsByQueryReturnsNull() {
        final ResponseEntity<List<AuditionComment>> response = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
                any(ParameterizedTypeReference.class), anyString())).thenReturn(response);

        final List<AuditionComment> result = client.getCommentsByQuery(POST_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideExceptionsForQuery")
    void throwSystemExceptionForGetCommentsByQuery(final Exception ex, final int expectedStatus) {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
                any(ParameterizedTypeReference.class), anyString())).thenThrow(ex);

        final SystemException thrown = assertThrows(SystemException.class,
                () -> client.getCommentsByQuery(POST_ID));

        assertEquals(expectedStatus, thrown.getStatusCode());
    }

    private static Stream<Object[]> provideExceptionsForQuery() {
        return Stream.of(
                new Object[]{HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null), 400},
                new Object[]{HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", null, null, null), 502},
                new Object[]{new ResourceAccessException("Resource unreachable"), 503},
                new Object[]{new RestClientException("Unexpected error"), 500}
        );
    }
}
