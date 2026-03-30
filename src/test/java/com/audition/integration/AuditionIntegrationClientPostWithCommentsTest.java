package com.audition.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private final AuditionPost mockPost = new AuditionPost(1, 1, "title", "body");
    private final AuditionComment mockComment = new AuditionComment(1, 1, "name", "email", "body");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "baseUrl", AuditionIntegrationClientPostTest.BASE_URL);
    }

    @Test
    void postWithCommentsCombinesPostAndComments() {
        final AuditionIntegrationClient spyClient = Mockito.spy(client);
        Mockito.doReturn(mockPost).when(spyClient).getPostById("1");
        Mockito.doReturn(List.of(mockComment)).when(spyClient).getCommentsByPostId("1");

        final AuditionPost result = spyClient.getPostWithComments("1");

        assertEquals(1, result.id());
        assertEquals(List.of(mockComment), result.comments());
    }

    @Test
    void postWithCommentsWhenSystemExceptionThrownRethrowsIt() {
        final AuditionIntegrationClient spyClient = Mockito.spy(client);
        Mockito.doThrow(new SystemException("not found", "Resource Not Found", 404)).when(spyClient).getPostById("1");
        Mockito.doReturn(List.of(mockComment)).when(spyClient).getCommentsByPostId("1");

        final SystemException ex = assertThrows(SystemException.class, () -> spyClient.getPostWithComments("1"));
        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void postWithCommentsWhenNonSystemExceptionThrownThrowsSystemException500() {
        final AuditionIntegrationClient spyClient = Mockito.spy(client);
        Mockito.doThrow(new RuntimeException("network error")).when(spyClient).getPostById("1");
        Mockito.doReturn(List.of(mockComment)).when(spyClient).getCommentsByPostId("1");

        final SystemException ex = assertThrows(SystemException.class, () -> spyClient.getPostWithComments("1"));
        assertEquals(500, ex.getStatusCode());
    }

    @Test
    void postWithCommentsFallbackLogsAndThrows() {
        final SystemException thrown = assertThrows(SystemException.class,
            () -> ReflectionTestUtils.invokeMethod(
                client, "getPostWithCommentsFallback", "1", new RuntimeException("circuit open")));
        assertEquals(503, thrown.getStatusCode());
        verify(auditionLogger).warn(any(), any());
    }
}
