package com.audition.web.advice;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.service.AuditionService;
import com.audition.web.AuditionController;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

class ExceptionControllerAdviceTest {

    private MockMvc mockMvc;

    private final AuditionService auditionService = Mockito.mock(AuditionService.class);
    private final AuditionLogger auditionLogger = Mockito.mock(AuditionLogger.class);

    private final Tracer tracer = Mockito.mock(Tracer.class);

    @BeforeEach
    public void setup() {
        // Instantiate the controller and the ControllerAdvice
        final AuditionController auditionController = new AuditionController(auditionService);
        final ExceptionControllerAdvice auditionControllerAdvice = new ExceptionControllerAdvice(auditionLogger,
            tracer);

        // Build MockMvc with the controller and the advice
        mockMvc = MockMvcBuilders.standaloneSetup(auditionController)
            .setControllerAdvice(auditionControllerAdvice)
            .build();
    }

    @Test
    void shouldHandleSystemExceptionAndReturnProblemDetail() throws Exception {
        final Integer statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        when(auditionService.getPostById(51))
            .thenThrow(new SystemException("Fetching comments for Post with id 51", statusCode,
                new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)));

        mockMvc.perform(get("/posts/51"))
            .andExpectAll(
                status().is5xxServerError(), // Expect the status code set by the advice
                jsonPath("$.title").value("API Error Occurred"),
                jsonPath("$.status").value(statusCode),
                jsonPath("$.detail").value("Fetching comments for Post with id 51"),
                jsonPath("$.instance").value("/posts/51")
            );
    }

    @Test
    void shouldHandleExceptionAndReturnProblemDetail() throws Exception {
        final Integer statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        when(auditionService.getPosts(-1))
            .thenThrow(new IllegalArgumentException("-1 is not a valid userId."));

        mockMvc.perform(get("/posts?userId=-1"))
            .andExpectAll(
                status().is5xxServerError(), // Expect the status code set by the advice
                jsonPath("$.title").value("API Error Occurred"),
                jsonPath("$.status").value(statusCode),
                jsonPath("$.detail").value("-1 is not a valid userId."),
                jsonPath("$.instance").value("/posts")
            );
    }

    @Test
    void shouldHandleHttpClientErrorExceptionAndReturnProblemDetail() throws Exception {
        final Integer statusCode = HttpStatus.BAD_REQUEST.value();

        when(auditionService.getPosts(-1))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "-1 is not a valid userId."));

        mockMvc.perform(get("/posts?userId=-1"))
            .andExpectAll(
                status().is4xxClientError(), // Expect the status code set by the advice
                jsonPath("$.title").value("API Error Occurred"),
                jsonPath("$.status").value(statusCode),
                jsonPath("$.detail").value("400 -1 is not a valid userId."),
                jsonPath("$.instance").value("/posts")
            );
    }
}
