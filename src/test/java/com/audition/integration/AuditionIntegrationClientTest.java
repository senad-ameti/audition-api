package com.audition.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AuditionIntegrationClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuditionLogger auditionLogger;

    @InjectMocks
    private AuditionIntegrationClient client;

    private static final String BASE_URL = "some_url";
    private final AuditionPost mockPost = new AuditionPost(1, 1, "title", "body");
    private final AuditionComment mockComment = new AuditionComment(1, 1, "name", "email", "body");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "url", BASE_URL);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "1"})
    void getPosts_withBlankOrNullUserId_callsWithoutParam(String userId) {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), eq(null),
            any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(List.of(mockPost)));

        List<AuditionPost> result = client.getPosts(userId);

        assertThat(result).containsExactly(mockPost);
    }

    @Test
    void getPosts_withNullBody_returnsEmptyList() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), eq(null),
            any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(null));

        assertThat(client.getPosts(null)).isEmpty();
    }

    @Test
    void getPostById_returnsPost() {
        when(restTemplate.getForObject(BASE_URL + "/posts/{id}", AuditionPost.class, "1"))
            .thenReturn(mockPost);

        assertThat(client.getPostById("1")).isEqualTo(mockPost);
    }

    @Test
    void getPostById_on404_throwsSystemException404() {
        when(restTemplate.getForObject(BASE_URL + "/posts/{id}", AuditionPost.class, "999"))
            .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));

        assertThatThrownBy(() -> client.getPostById("999"))
            .isInstanceOf(SystemException.class)
            .hasMessageContaining("999")
            .matches(e -> ((SystemException) e).getStatusCode() == 404);
    }

    @Test
    void getPostById_onOtherHttpError_throwsSystemException() {
        when(restTemplate.getForObject(BASE_URL + "/posts/{id}", AuditionPost.class, "1"))
            .thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY, new byte[0], null));

        assertThatThrownBy(() -> client.getPostById("1"))
            .isInstanceOf(SystemException.class)
            .matches(e -> ((SystemException) e).getStatusCode() == 403);
    }

    @Test
    void getPostById_onGenericException_throwsSystemException500() {
        when(restTemplate.getForObject(BASE_URL + "/posts/{id}", AuditionPost.class, "1"))
            .thenThrow(new RuntimeException("network error"));

        assertThatThrownBy(() -> client.getPostById("1"))
            .isInstanceOf(SystemException.class)
            .matches(e -> ((SystemException) e).getStatusCode() == 500);
    }

    @Test
    void getCommentsByPostId_returnsComments() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), eq(null),
            any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(List.of(mockComment)));

        assertThat(client.getCommentsByPostId("1")).containsExactly(mockComment);
    }

    @Test
    void getCommentsByPostId_withNullBody_returnsEmptyList() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), eq(null),
            any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(null));

        assertThat(client.getCommentsByPostId("1")).isEmpty();
    }

    @Test
    void getComments_withPostId_callsWithParam() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), eq(null),
            any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(List.of(mockComment)));

        assertThat(client.getComments("1")).containsExactly(mockComment);
    }

    @Test
    void getComments_withNullPostId_callsWithoutParam() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), eq(null),
            any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(List.of()));

        assertThat(client.getComments(null)).isEmpty();
    }

    @Test
    void getPostWithComments_combinesPostAndComments() {
        AuditionIntegrationClient spyClient = spy(client);
        doReturn(mockPost).when(spyClient).getPostById("1");
        doReturn(List.of(mockComment)).when(spyClient).getCommentsByPostId("1");

        AuditionPost result = spyClient.getPostWithComments("1");

        assertThat(result.id()).isEqualTo(1);
        assertThat(result.comments()).containsExactly(mockComment);
    }

    @Test
    void getPostWithComments_whenSystemExceptionThrown_rethrowsIt() {
        AuditionIntegrationClient spyClient = spy(client);
        doThrow(new SystemException("not found", "Resource Not Found", 404)).when(spyClient).getPostById("1");
        doReturn(List.of(mockComment)).when(spyClient).getCommentsByPostId("1");

        assertThatThrownBy(() -> spyClient.getPostWithComments("1"))
            .isInstanceOf(SystemException.class)
            .matches(e -> ((SystemException) e).getStatusCode() == 404);
    }

    @Test
    void getPostWithComments_whenNonSystemExceptionThrown_throwsSystemException500() {
        AuditionIntegrationClient spyClient = spy(client);
        doThrow(new RuntimeException("network error")).when(spyClient).getPostById("1");
        doReturn(List.of(mockComment)).when(spyClient).getCommentsByPostId("1");

        assertThatThrownBy(() -> spyClient.getPostWithComments("1"))
            .isInstanceOf(SystemException.class)
            .matches(e -> ((SystemException) e).getStatusCode() == 500);
    }

    @Test
    void getPostsFallback_logsAndReturnsEmpty() {
        List<AuditionPost> result = ReflectionTestUtils.invokeMethod(
            client, "getPostsFallback", new RuntimeException("circuit open"));
        assertThat(result).isEmpty();
        verify(auditionLogger).warn(any(), any());
    }

    @Test
    void getPostByIdFallback_logsAndThrows() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
            client, "getPostByIdFallback", "1", new RuntimeException("circuit open")))
            .isInstanceOf(SystemException.class)
            .matches(e -> ((SystemException) e).getStatusCode() == 503);
        verify(auditionLogger).warn(any(), any());
    }

    @Test
    void getPostWithCommentsFallback_logsAndThrows() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
            client, "getPostWithCommentsFallback", "1", new RuntimeException("circuit open")))
            .isInstanceOf(SystemException.class)
            .matches(e -> ((SystemException) e).getStatusCode() == 503);
        verify(auditionLogger).warn(any(), any());
    }

    @Test
    void getCommentsByPostIdFallback_logsAndReturnsEmpty() {
        List<AuditionComment> result = ReflectionTestUtils.invokeMethod(
            client, "getCommentsByPostIdFallback", "1", new RuntimeException("circuit open"));
        assertThat(result).isEmpty();
        verify(auditionLogger).warn(any(), any());
    }

    @Test
    void getCommentsFallback_logsAndReturnsEmpty() {
        List<AuditionComment> result = ReflectionTestUtils.invokeMethod(
            client, "getCommentsFallback", "1", new RuntimeException("circuit open"));
        assertThat(result).isEmpty();
        verify(auditionLogger).warn(any(), any());
    }
}
