package com.audition.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.audition.common.exception.SystemException;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import java.net.URI;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest
public class AuditionIntegrationClientTest {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com/posts";

    @Autowired
    transient AuditionIntegrationClient client;

    @MockBean
    transient RestTemplate restTemplate;

    @BeforeEach
    public void setup() {
        openMocks(this);
    }

    @Test
    public void getPosts_nullPosts_successful() {
        when(restTemplate.getForObject(anyString(), any())).thenReturn(null);

        List<AuditionPost> posts = client.getPosts();
        assertThat(posts).isEmpty();
    }

    @Test
    public void getPosts_validPosts_successful() {
        AuditionPost[] expect = {
            new AuditionPost(1, 1, "Title 1", "Body 1", List.of()),
            new AuditionPost(1, 2, "Title 2", "Body 2", List.of())
        };
        when(restTemplate.getForObject(anyString(), any())).thenReturn(expect);

        List<AuditionPost> actual = client.getPosts();
        assertThat(actual).containsExactlyInAnyOrderElementsOf(List.of(expect));
    }

    @Test
    public void getPosts_exception_failed() {
        HttpClientErrorException exception = HttpClientErrorException.create("Bad Request", HttpStatusCode.valueOf(400),
            "Bad Request", HttpHeaders.EMPTY, null, null);
        when(restTemplate.getForObject(anyString(), any())).thenThrow(exception);

        assertThrows(SystemException.class, () -> client.getPosts());
    }

    @Test
    public void getPostById_nullPost_successful() {
        when(restTemplate.getForObject(any(), any())).thenReturn(null);

        AuditionPost actual = client.getPostById(1);
        AuditionPost expected = new AuditionPost();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getPostById_validPost_successful() {
        AuditionPost expect =
            new AuditionPost(1, 1, "Title 1", "Body 1", List.of());
        when(restTemplate.getForObject(any(), any())).thenReturn(expect);

        AuditionPost actual = client.getPostById(1);
        assertThat(actual).isEqualTo(expect);
    }

    @Test
    public void getPostById_notFoundException_failed() {
        HttpClientErrorException exception = HttpClientErrorException.create("Not Found", HttpStatusCode.valueOf(404),
            "Not Found", HttpHeaders.EMPTY, null, null);
        when(restTemplate.getForObject(any(), any())).thenThrow(exception);

        Assertions.assertThatExceptionOfType(SystemException.class)
            .isThrownBy(() -> client.getPostById(1000))
            .withMessage("Cannot find a Post with id 1000");
    }

    @Test
    public void getPostById_generalException_failed() {
        HttpClientErrorException exception = HttpClientErrorException.create("Bad Request", HttpStatusCode.valueOf(400),
            "Bad Request", HttpHeaders.EMPTY, null, null);
        when(restTemplate.getForObject(any(), any())).thenThrow(exception);

        Assertions.assertThatExceptionOfType(SystemException.class)
            .isThrownBy(() -> client.getPostById(1000))
            .withMessage("Error occurred while fetching post with id 1000: Bad Request");
    }

    @Test
    public void getCommentsByPostId_nullPost_successful() {
        when(restTemplate.getForObject(any(), any())).thenReturn(null);

        List<AuditionComment> actual = client.getCommentsByPostId(1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void getCommentsByPostId_validPost_successful() {
        AuditionComment[] expect = {
            new AuditionComment(1, 1, "Name 1", "Email 1", "Body 1"),
            new AuditionComment(1, 2, "Name 2", "Email 2", "Body 2")};
        when(restTemplate.getForObject(any(), any())).thenReturn(expect);

        List<AuditionComment> actual = client.getCommentsByPostId(1);
        assertThat(actual).containsExactlyInAnyOrderElementsOf(List.of(expect));
    }

    @Test
    public void getCommentsByPostId_notFoundException_failed() {
        HttpClientErrorException exception = HttpClientErrorException.create("Not Found", HttpStatusCode.valueOf(404),
            "Not Found", HttpHeaders.EMPTY, null, null);
        when(restTemplate.getForObject(any(), any())).thenThrow(exception);

        Assertions.assertThatExceptionOfType(SystemException.class)
            .isThrownBy(() -> client.getCommentsByPostId(1000))
            .withMessage("Cannot find a Post with id 1000");
    }

    @Test
    public void getCommentsByPostId_generalException_failed() {
        HttpClientErrorException exception = HttpClientErrorException.create("Bad Request", HttpStatusCode.valueOf(400),
            "Bad Request", HttpHeaders.EMPTY, null, null);
        when(restTemplate.getForObject(any(), any())).thenThrow(exception);

        Assertions.assertThatExceptionOfType(SystemException.class)
            .isThrownBy(() -> client.getCommentsByPostId(1000))
            .withMessage("Error occurred while fetching comments for post with id 1000: Bad Request");
    }

    @Test
    public void getPostWithCommentsByPostId_validPost_successful() {
        AuditionComment[] expectComments = {
            new AuditionComment(1, 1, "Name 1", "Email 1", "Body 1"),
            new AuditionComment(1, 2, "Name 2", "Email 2", "Body 2")};
        AuditionPost expectPost =
            new AuditionPost(1, 1, "Title 1", "Body 1", List.of());
        final URI commentUrl = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/{id}/comments")
            .buildAndExpand(1)
            .encode()
            .toUri();
        final URI postUrl = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/{id}")
            .buildAndExpand(1)
            .encode()
            .toUri();
        when(restTemplate.getForObject(eq(postUrl), any())).thenReturn(expectPost);
        when(restTemplate.getForObject(eq(commentUrl), any())).thenReturn(expectComments);

        AuditionPost expect = new AuditionPost(1, 1, "Title 1", "Body 1", List.of(expectComments));

        AuditionPost actual = client.getPostWithCommentsByPostId(1);
        assertThat(actual).isEqualTo(expect);
    }
}
