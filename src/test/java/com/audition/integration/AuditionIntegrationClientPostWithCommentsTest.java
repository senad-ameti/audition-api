package com.audition.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuditionIntegrationClientPostWithCommentsTest {

    @Mock
    private AuditionLogger auditionLogger;

    @InjectMocks
    private AuditionIntegrationClient client;

    private static final String CIRCUIT_OPEN = "circuit open";

    private final AuditionPost mockPost = new AuditionPost(1, 1, "title", "body");
    private final AuditionComment mockComment = new AuditionComment(1, 1, "name", "email", "body");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "baseUrl", AuditionIntegrationClientTest.BASE_URL);
    }

    @Test
    void postWithCommentsCombinesPostAndComments() {
        final AuditionIntegrationClient spyClient = Mockito.spy(client);
        Mockito.doReturn(mockPost).when(spyClient).getPostById("1");
        Mockito.doReturn(List.of(mockComment)).when(spyClient).getCommentsByPostId("1");

        final AuditionPost result = spyClient.getPostWithComments("1");

        assertThat(result.id()).isEqualTo(1);
        assertThat(result.comments()).containsExactly(mockComment);
    }

    @Test
    void postWithCommentsWhenSystemExceptionThrownRethrowsIt() {
        final AuditionIntegrationClient spyClient = Mockito.spy(client);
        Mockito.doThrow(new SystemException("not found", "Resource Not Found", 404)).when(spyClient).getPostById("1");
        Mockito.doReturn(List.of(mockComment)).when(spyClient).getCommentsByPostId("1");

        assertThatThrownBy(() -> spyClient.getPostWithComments("1"))
            .isInstanceOf(SystemException.class)
            .matches(e -> ((SystemException) e).getStatusCode() == 404);
    }

    @Test
    void postWithCommentsWhenNonSystemExceptionThrownThrowsSystemException500() {
        final AuditionIntegrationClient spyClient = Mockito.spy(client);
        Mockito.doThrow(new RuntimeException("network error")).when(spyClient).getPostById("1");
        Mockito.doReturn(List.of(mockComment)).when(spyClient).getCommentsByPostId("1");

        assertThatThrownBy(() -> spyClient.getPostWithComments("1"))
            .isInstanceOf(SystemException.class)
            .matches(e -> ((SystemException) e).getStatusCode() == 500);
    }

    @Test
    void postWithCommentsFallbackLogsAndThrows() {
        final SystemException thrown = org.junit.jupiter.api.Assertions.assertThrows(SystemException.class,
            () -> ReflectionTestUtils.invokeMethod(
                client, "getPostWithCommentsFallback", "1", new RuntimeException(CIRCUIT_OPEN)));
        assertThat(thrown.getStatusCode()).isEqualTo(503);
        verify(auditionLogger).warn(any(), any());
    }
}
