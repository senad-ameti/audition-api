package com.audition.web.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
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

@SuppressWarnings("PMD.ExcessiveImports")
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
        assertEquals(404, result.getStatus());
        assertEquals(ExceptionControllerAdvice.DEFAULT_TITLE, result.getTitle());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mainExceptionCases")
    void handleMainException(final String name, final Exception ex, final int expectedStatus) {
        final ProblemDetail result = advice.handleMainException(ex);
        assertEquals(expectedStatus, result.getStatus());
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
        assertEquals(expectedStatus, result.getStatus());
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

        assertEquals(400, result.getStatus());
        Assertions.assertNotNull(result.getDetail());
        assertTrue(result.getDetail().contains("must not be blank"));
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

        Assertions.assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        final ProblemDetail pd = (ProblemDetail) response.getBody();
        Assertions.assertNotNull(pd);
        Assertions.assertNotNull(pd.getDetail());
        assertTrue(pd.getDetail().contains("at least 3 characters"));
        verify(logger).logStandardProblemDetail(any(), any(), eq(ex));
    }
}
