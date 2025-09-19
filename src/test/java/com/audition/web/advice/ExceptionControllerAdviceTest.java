package com.audition.web.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.contains;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class ExceptionControllerAdviceTest {

    @Mock
    private AuditionLogger auditionLogger;

    @InjectMocks
    private ExceptionControllerAdvice advice;

    @Test
    void handleHttpClientExceptionReturnsProblemDetail() {
        final HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");

        final ProblemDetail result = advice.handleHttpClientException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getDetail()).isEqualTo("400 Bad Request");
        assertThat(result.getTitle()).isEqualTo(ExceptionControllerAdvice.DEFAULT_TITLE);
    }

    @Test
    @SuppressWarnings("PMD.GuardLogStatement")
    void handleMainExceptionReturnsInternalServerError() {
        final Exception ex = new Exception("Something went wrong");

        final ProblemDetail result = advice.handleMainException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getDetail()).isEqualTo("Something went wrong");
        assertThat(result.getTitle()).isEqualTo(ExceptionControllerAdvice.DEFAULT_TITLE);

        verify(auditionLogger).error(any(Logger.class), contains("Unhandled exception caught"));
    }

    @Test
    @SuppressWarnings("PMD.GuardLogStatement")
    void handleSystemExceptionReturnsCustomStatusAndTitle() {
        final SystemException ex = new SystemException("Custom error", "Custom Title", 400);

        final ProblemDetail result = advice.handleSystemException(ex);

        assertThat(result.getStatus()).isEqualTo(400);
        assertThat(result.getDetail()).isEqualTo("Custom error");
        assertThat(result.getTitle()).isEqualTo("Custom Title");

        verify(auditionLogger).error(any(Logger.class), contains("SystemException occurred"));
    }

    @Test
    @SuppressWarnings("PMD.GuardLogStatement")
    void handleSystemExceptionWithInvalidStatusReturnsInternalServerError() {
        final SystemException ex = new SystemException("Custom error", "Custom Title", 999);

        final ProblemDetail result = advice.handleSystemException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getDetail()).isEqualTo("Custom error");
        assertThat(result.getTitle()).isEqualTo("Custom Title");

        verify(auditionLogger).info(any(Logger.class), contains("Error Code from Exception could not be mapped"));
    }

    @Test
    void handleMainExceptionWithBlankMessageReturnsDefaultMessage() {
        final Exception ex = new Exception("   "); // blank message

        final ProblemDetail result = advice.handleMainException(ex);

        assertThat(result.getDetail()).isEqualTo(ExceptionControllerAdvice.DEFAULT_MESSAGE);
        assertThat(result.getTitle()).isEqualTo(ExceptionControllerAdvice.DEFAULT_TITLE);
    }

    @Test
    void handleSystemExceptionWithBlankMessageReturnsDefaultMessage() {
        final SystemException ex = new SystemException("   ", "System Title", 500);

        final ProblemDetail result = advice.handleSystemException(ex);

        assertThat(result.getDetail()).isEqualTo(ExceptionControllerAdvice.DEFAULT_MESSAGE);
        assertThat(result.getTitle()).isEqualTo("System Title");
    }
}
