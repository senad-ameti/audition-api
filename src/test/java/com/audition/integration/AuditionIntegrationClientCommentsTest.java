package com.audition.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionComment;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AuditionIntegrationClientCommentsTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuditionLogger auditionLogger;

    @InjectMocks
    private AuditionIntegrationClient client;

    private final AuditionComment mockComment = new AuditionComment(1, 1, "name", "email", "body");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "baseUrl", AuditionIntegrationClientPostTest.BASE_URL);
    }

    @Test
    void commentsWithPostIdCallsWithParam() {
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), eq(null),
            any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(List.of(mockComment)));

        assertEquals(List.of(mockComment), client.getComments("1"));
    }

    @Test
    void commentsWithNullPostIdCallsWithoutParam() {
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), eq(null),
            any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(List.of()));

        assertTrue(client.getComments(null).isEmpty());
    }

    @Test
    void commentsFallbackLogsAndReturnsEmpty() {
        final List<AuditionComment> result = ReflectionTestUtils.invokeMethod(
            client, "getCommentsFallback", "1", new RuntimeException("circuit open"));
        assertTrue(result.isEmpty());
        verify(auditionLogger).warn(any(), any());
    }
}
