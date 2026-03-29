package com.audition.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionComment;
import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostFilter;
import com.audition.service.AuditionService;
import io.micrometer.tracing.Tracer;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AuditionController.class)
class AuditionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditionService auditionService;

    @MockitoBean
    private Tracer tracer;

    @MockitoBean
    private AuditionLogger auditionLogger;

    private AuditionPost post1;
    private AuditionPost post2;

    @BeforeEach
    void setUp() {
        post1 = new AuditionPost(1, 1, "sunt aut facere", "body1");
        post2 = new AuditionPost(2, 2, "qui est esse", "body2");
    }

    // --- GET /posts ---

    @ParameterizedTest(name = "userId={0} title={1} → HTTP {2}")
    @MethodSource("postParams")
    void getPosts_withVariousParams(String userId, String title, int expectedStatus) throws Exception {
        when(auditionService.getPosts(any(AuditionPostFilter.class))).thenReturn(List.of(post1, post2));

        MockHttpServletRequestBuilder request = get("/posts").accept(MediaType.APPLICATION_JSON);
        if (userId != null) {
            request = request.param("userId", userId);
        }
        if (title != null) {
            request = request.param("title", title);
        }

        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    static Stream<Arguments> postParams() {
        return Stream.of(
            Arguments.of(null, null, 200),
            Arguments.of("1", null, 200),
            Arguments.of(null, "vero", 200),
            Arguments.of(null, "v", 400)      // < 3 chars → BindException → 400
        );
    }

    @Test
    void getPosts_withNoParams_returnsList() throws Exception {
        when(auditionService.getPosts(any(AuditionPostFilter.class))).thenReturn(List.of(post1));

        mockMvc.perform(get("/posts").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].title").value("sunt aut facere"));
    }

    // --- GET /posts/{id} ---

    @Test
    void getPostById_withValidId_returnsPost() throws Exception {
        when(auditionService.getPostById("1")).thenReturn(post1);

        mockMvc.perform(get("/posts/1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getPostById_whenServiceThrows_propagatesError() throws Exception {
        when(auditionService.getPostById("999")).thenThrow(
            new com.audition.common.exception.SystemException("Cannot find a Post with id 999", "Resource Not Found", 404));

        mockMvc.perform(get("/posts/999").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    // --- GET /posts/{id}/comments ---

    @Test
    void getPostWithComments_returnsPostWithComments() throws Exception {
        List<AuditionComment> comments = List.of(new AuditionComment(1, 1, "name", "email", "body"));
        AuditionPost postWithComments = post1.withComments(comments);
        when(auditionService.getPostWithComments("1")).thenReturn(postWithComments);

        mockMvc.perform(get("/posts/1/comments").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.comments[0].id").value(1));
    }

    @Test
    void getPostWithComments_whenServiceThrows_propagatesError() throws Exception {
        when(auditionService.getPostWithComments("999")).thenThrow(
            new com.audition.common.exception.SystemException("Cannot find a Post with id 999", "Resource Not Found", 404));

        mockMvc.perform(get("/posts/999/comments").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    // --- GET /comments ---

    @Test
    void getComments_withPostId_returnsComments() throws Exception {
        List<AuditionComment> comments = List.of(new AuditionComment(1, 1, "name", "email", "body"));
        when(auditionService.getComments("1")).thenReturn(comments);

        mockMvc.perform(get("/comments").param("postId", "1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].postId").value(1));
    }

    @Test
    void getComments_withoutPostId_returnsAllComments() throws Exception {
        when(auditionService.getComments(null)).thenReturn(List.of());

        mockMvc.perform(get("/comments").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
