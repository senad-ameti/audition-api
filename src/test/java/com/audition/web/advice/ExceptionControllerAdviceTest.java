package com.audition.web.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.ServletWebRequest;

@ExtendWith(MockitoExtension.class)
class ExceptionControllerAdviceTest {

    @Mock
    private AuditionLogger logger;

    @InjectMocks
    private ExceptionControllerAdvice advice;

    @Test
    void handleHttpClientException() {
        final HttpClientErrorException ex = HttpClientErrorException.create(
            HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null);
        final ProblemDetail result = advice.handleHttpClientException(ex);
        assertThat(result.getStatus()).isEqualTo(404);
        assertThat(result.getTitle()).isEqualTo(ExceptionControllerAdvice.DEFAULT_TITLE);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mainExceptionCases")
    void handleMainException(final String name, final Exception ex, final int expectedStatus) {
        final ProblemDetail result = advice.handleMainException(ex);
        assertThat(result.getStatus()).isEqualTo(expectedStatus);
        verify(logger).logErrorWithException(any(), any(String.class), eq(ex));
    }

    static Stream<Arguments> mainExceptionCases() {
        return Stream.of(
            Arguments.of("generic exception → 500",
                new RuntimeException("generic"), 500),
            Arguments.of("HttpClientErrorException → 400",
                HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad", HttpHeaders.EMPTY, new byte[0], null), 400)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("systemExceptionCases")
    void handleSystemException(final String name, final SystemException ex, final int expectedStatus) {
        final ProblemDetail result = advice.handleSystemException(ex);
        assertThat(result.getStatus()).isEqualTo(expectedStatus);
        verify(logger).logStandardProblemDetail(any(), any(), eq(ex));
    }

    static Stream<Arguments> systemExceptionCases() {
        return Stream.of(
            Arguments.of("valid 404",
                new SystemException("not found", "Resource Not Found", 404), 404),
            Arguments.of("valid 500",
                new SystemException("server error", "Internal Error", 500), 500),
            Arguments.of("null status code → 500",
                new SystemException("message"), 500)
        );
    }

    @Test
    void handleConstraintViolationException() {
        final ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
        Mockito.when(violation.getMessage()).thenReturn("must not be blank");
        final ConstraintViolationException ex = new ConstraintViolationException("CVE", Set.of(violation));

        final ProblemDetail result = advice.handleConstraintViolationException(ex);

        assertThat(result.getStatus()).isEqualTo(400);
        assertThat(result.getDetail()).contains("must not be blank");
        verify(logger).logStandardProblemDetail(any(), any(), eq(ex));
    }

    @Test
    void handleMethodArgumentNotValid() {
        final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "filter");
        bindingResult.addError(new FieldError("filter", "title", "Title must be at least 3 characters"));
        final MethodParameter methodParameter = Mockito.mock(MethodParameter.class);
        final MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        final var response = advice.handleMethodArgumentNotValid(
            ex, new HttpHeaders(), HttpStatus.BAD_REQUEST,
            new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        final ProblemDetail pd = (ProblemDetail) response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getDetail()).contains("at least 3 characters");
        verify(logger).logStandardProblemDetail(any(), any(), eq(ex));
    }
}
