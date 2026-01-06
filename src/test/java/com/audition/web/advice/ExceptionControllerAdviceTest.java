package com.audition.web.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
public class ExceptionControllerAdviceTest {

    @InjectMocks
    private ExceptionControllerAdvice advice;

    @Mock
    private AuditionLogger logger;

    @Test
    void handleHttpClientException_validException_successful() {
        final String message = "resource missing";
        final HttpClientErrorException exception = HttpClientErrorException.create(message,
            HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null);

        final ProblemDetail pd = advice.handleHttpClientException(exception);

        assertThat(pd).isNotNull();
        assertThat(pd.getDetail()).isEqualTo(message);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        verifyNoInteractions(logger);
    }

    @Test
    void handleMainException_clientErrorException_successful() {
        final HttpClientErrorException exception = HttpClientErrorException.create("client error",
            HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, null, null);

        final ProblemDetail pd = advice.handleMainException(exception);

        assertThat(pd).isNotNull();
        assertThat(pd.getDetail()).isEqualTo("client error");
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getTitle()).isEqualTo(ExceptionControllerAdvice.DEFAULT_TITLE);
        verify(logger).logStandardProblemDetail(any(), any(), any());
    }

    @Test
    void handleMainException_methodNotAllowedException_successful() {
        final HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("DELETE");

        final ProblemDetail pd = advice.handleMainException(exception);

        assertThat(pd).isNotNull();
        assertThat(pd.getDetail()).isEqualTo("Request method 'DELETE' is not supported");
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
        assertThat(pd.getTitle()).isEqualTo(ExceptionControllerAdvice.DEFAULT_TITLE);
        verify(logger).logStandardProblemDetail(any(), any(), any());
    }

    @Test
    void handleMainException_emptyMessageException_successful() {
        final Exception exception = new Exception("");

        final ProblemDetail pd = advice.handleMainException(exception);

        assertThat(pd).isNotNull();
        assertThat(pd.getDetail()).isEqualTo("API Error occurred. Please contact support or administrator.");
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(pd.getTitle()).isEqualTo(ExceptionControllerAdvice.DEFAULT_TITLE);
        verify(logger).logStandardProblemDetail(any(), any(), any());
    }

    @Test
    void handleMainException_validException_successful() {
        final Exception exception = new Exception("something went wrong");

        final ProblemDetail pd = advice.handleMainException(exception);

        assertThat(pd).isNotNull();
        assertThat(pd.getDetail()).isEqualTo("something went wrong");
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(pd.getTitle()).isEqualTo(ExceptionControllerAdvice.DEFAULT_TITLE);
        verify(logger).logStandardProblemDetail(any(), any(), any());
    }

    @Test
    void handleSystemException_validException_successful() {
        final SystemException exception = new SystemException("System Detail", "System Title",
            HttpStatus.BAD_REQUEST.value());

        final ProblemDetail pd = advice.handleSystemException(exception);

        assertThat(pd).isNotNull();
        assertThat(pd.getDetail()).isEqualTo("System Detail");
        assertThat(pd.getTitle()).isEqualTo("System Title");
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        verify(logger).logStandardProblemDetail(any(), any(), any());
    }

    @Test
    void handleSystemException_invalidStatus_failed() {
        final int invalidStatus = 1000;
        final SystemException exception = new SystemException("bad status", "Bad", invalidStatus);

        final ProblemDetail pd = advice.handleSystemException(exception);

        assertThat(pd).isNotNull();
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        verify(logger).logStandardProblemDetail(any(), any(), any());
    }
}
