package com.audition.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.audition.common.exception.SystemException;
import com.audition.model.AuditionPost;
import com.audition.model.PostComment;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AuditionIntegrationClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AuditionIntegrationClient auditionIntegrationClient;

    private final HttpClientErrorException clientException = new HttpClientErrorException(HttpStatus.NOT_FOUND,
        "not found");

    @Test
    void getPostsShouldReturnNullWhenNoPostsFound() {
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            eq(null),
            ArgumentMatchers.<ParameterizedTypeReference<List<AuditionPost>>>any())
        ).thenReturn(ResponseEntity.ofNullable(null));

        assertThat(auditionIntegrationClient.getPosts(1)).isNull();
    }

    @Test
    void getPostsShouldThrowSystemException() {
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            eq(null),
            ArgumentMatchers.<ParameterizedTypeReference<List<AuditionPost>>>any())
        ).thenThrow(clientException);

        assertThatThrownBy(() -> auditionIntegrationClient.getPosts(null))
            .isInstanceOf(SystemException.class)
            .extracting("statusCode").isEqualTo(404);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            eq(null),
            ArgumentMatchers.<ParameterizedTypeReference<List<AuditionPost>>>any())
        ).thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> auditionIntegrationClient.getPosts(1))
            .isInstanceOf(SystemException.class)
            .extracting("detail", "statusCode").containsExactly("Fetching Posts with userId 1", 502);
    }

    @Test
    void getPostsShouldReturnCorrectResult() {
        final AuditionPost post1 = new AuditionPost(1, 1, "title1", "body1");
        final AuditionPost post2 = new AuditionPost(1, 2, "title2", "body2");

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            eq(null),
            ArgumentMatchers.<ParameterizedTypeReference<List<AuditionPost>>>any())
        ).thenReturn(ResponseEntity.ok(List.of(post1, post2)));

        assertThat(auditionIntegrationClient.getPosts(1))
            .hasSize(2)
            .containsExactly(post1, post2);
    }

    @Test
    void getPostByIdShouldReturnCorrectResult() {
        final AuditionPost post1 = new AuditionPost(1, 1, "title1", "body1");

        when(restTemplate.getForEntity(anyString(), eq(AuditionPost.class)))
            .thenReturn(ResponseEntity.ok(post1));

        assertThat(auditionIntegrationClient.getPostById(1))
            .isEqualTo(post1);
    }

    @Test
    void getPostByIdShouldThrowSystemExceptionWhenExceptionReceived() {
        when(restTemplate.getForEntity(anyString(), eq(AuditionPost.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> auditionIntegrationClient.getPostById(299))
            .as("Fetching a Post with id 299")
            .isInstanceOf(SystemException.class)
            .extracting("statusCode").isEqualTo(404);

        when(restTemplate.getForEntity(anyString(), eq(AuditionPost.class)))
            .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> auditionIntegrationClient.getPostById(298))
            .as("Fetching a Post with id 298")
            .isInstanceOf(SystemException.class)
            .extracting("statusCode").isEqualTo(502);
    }

    @Test
    void getCommentsByPostIdShouldReturnCorrectResult() {
        final PostComment comment1 = new PostComment(1, 1, "name1", "email2", "body1");
        final PostComment comment2 = new PostComment(1, 2, "name2", "email2", "body2");

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            eq(null),
            ArgumentMatchers.<ParameterizedTypeReference<List<PostComment>>>any())
        ).thenReturn(ResponseEntity.ok(List.of(comment1, comment2)));

        assertThat(auditionIntegrationClient.getCommentsByPostId(1))
            .hasSize(2)
            .containsExactly(comment1, comment2);
    }

    @Test
    void getCommentsByPostIdShouldThrowSystemExceptionWhenExceptionReceived() {
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            eq(null),
            ArgumentMatchers.<ParameterizedTypeReference<List<PostComment>>>any())
        ).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> auditionIntegrationClient.getCommentsByPostId(300))
            .isInstanceOf(SystemException.class)
            .extracting("statusCode").isEqualTo(404);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            eq(null),
            ArgumentMatchers.<ParameterizedTypeReference<List<PostComment>>>any())
        ).thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> auditionIntegrationClient.getCommentsByPostId(300))
            .isInstanceOf(SystemException.class)
            .extracting("statusCode").isEqualTo(502);
    }
}
