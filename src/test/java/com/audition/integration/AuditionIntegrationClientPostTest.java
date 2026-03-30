package com.audition.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionPost;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AuditionIntegrationClientPostTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuditionLogger auditionLogger;

    @InjectMocks
    private AuditionIntegrationClient client;

    static final String BASE_URL = "some_url";

    private final AuditionPost mockPost = new AuditionPost(1, 1, "title", "body");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "1"})
    void postsWithBlankOrNullUserIdCallsWithoutParam(final String userId) {
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), eq(null),
            any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(List.of(mockPost)));

        final List<AuditionPost> result = client.getPosts(userId);

        assertEquals(List.of(mockPost), result);
    }

    @Test
    void postsWithNullBodyReturnsEmptyList() {
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), eq(null),
            any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(null));

        assertTrue(client.getPosts(null).isEmpty());
    }

    @Test
    void postsFallbackLogsAndReturnsEmpty() {
        final List<AuditionPost> result = ReflectionTestUtils.invokeMethod(
            client, "getPostsFallback", new RuntimeException("circuit open"));
        assertTrue(result.isEmpty());
        verify(auditionLogger).warn(any(), any());
    }
}
