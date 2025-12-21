package com.audition.web.advice;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


/**
 * Global exception handler for the application. This class extends ResponseEntityExceptionHandler to provide
 * centralized exception handling for REST controllers, converting exceptions into standardized ProblemDetail
 * responses.
 */
@ControllerAdvice
public class ExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    public static final String DEFAULT_TITLE = "API Error Occurred";
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionControllerAdvice.class);
    private static final String ERROR_MESSAGE = " Error Code from Exception could not be mapped to a valid HttpStatus Code - ";
    private static final String DEFAULT_MESSAGE = "API Error occurred. Please contact support or administrator.";

    @Autowired
    private transient AuditionLogger logger;

    /**
     * Handles HttpClientErrorException by creating a ProblemDetail response.
     *
     * @param e the HttpClientErrorException that was thrown
     * @return a ProblemDetail object representing the error
     */
    @ExceptionHandler(HttpClientErrorException.class)
    ProblemDetail handleHttpClientException(final HttpClientErrorException e) {
        return createProblemDetail(e, e.getStatusCode());

    }

    /**
     * Handles ConstraintViolationException by aggregating violation messages into a single error detail.
     *
     * @param e the ConstraintViolationException that was thrown
     * @return a ProblemDetail object with BAD_REQUEST status and violation messages
     */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolationException(final ConstraintViolationException e) {
        final String errorMessage = e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));

        final ProblemDetail problemDetail = ProblemDetail.forStatus(BAD_REQUEST);
        problemDetail.setDetail(errorMessage);
        problemDetail.setTitle("Invalid Request");
        return problemDetail;
    }


    /**
     * Handles general Exception by determining the appropriate HTTP status and logging the error.
     *
     * @param e the Exception that was thrown
     * @return a ProblemDetail object representing the error
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleMainException(final Exception e) {
        final HttpStatusCode status = getHttpStatusCodeFromException(e);
        final ProblemDetail problemDetail = createProblemDetail(e, status);
        logException(e, status, problemDetail);
        return problemDetail;

    }

    /**
     * Handles SystemException by extracting status code and logging the error.
     *
     * @param e the SystemException that was thrown
     * @return a ProblemDetail object representing the error
     */
    @ExceptionHandler(SystemException.class)
    ProblemDetail handleSystemException(final SystemException e) {
        final HttpStatusCode status = getHttpStatusCodeFromSystemException(e);
        final ProblemDetail problemDetail = createProblemDetail(e, status);
        logException(e, status, problemDetail);
        return problemDetail;

    }


    /**
     * Creates a ProblemDetail object from the given exception and status code.
     *
     * @param exception  the exception that occurred
     * @param statusCode the HTTP status code for the response
     * @return a ProblemDetail object with the appropriate details
     */
    private ProblemDetail createProblemDetail(final Exception exception,
        final HttpStatusCode statusCode) {
        final ProblemDetail problemDetail = ProblemDetail.forStatus(statusCode);
        problemDetail.setDetail(getMessageFromException(exception));
        if (exception instanceof SystemException) {
            problemDetail.setTitle(((SystemException) exception).getTitle());
        } else {
            problemDetail.setTitle(DEFAULT_TITLE);
        }
        return problemDetail;
    }

    /**
     * Retrieves the error message from the exception. Currently returns a default message.
     *
     * @param exception the exception
     * @return the error message string
     */
    private String getMessageFromException(final Exception exception) {
        return DEFAULT_MESSAGE;
    }

    /**
     * Logs the exception based on the HTTP status code. Client errors are logged using logHttpStatusCodeError, server
     * errors are logged using logStandardProblemDetail.
     *
     * @param exception     the exception to log
     * @param status        the HTTP status code
     * @param problemDetail the ProblemDetail object for structured logging
     */
    private void logException(final Exception exception, final HttpStatusCode status,
        final ProblemDetail problemDetail) {
        if (status.is4xxClientError()) {
            final String detail =
                StringUtils.isNotBlank(exception.getMessage()) ? exception.getMessage() : DEFAULT_MESSAGE;
            logger.logHttpStatusCodeError(LOG, detail, status.value());
        } else if (status.is5xxServerError()) {
            logger.logStandardProblemDetail(LOG, problemDetail, exception);
        }
    }

    /**
     * Extracts the HTTP status code from a SystemException. Falls back to INTERNAL_SERVER_ERROR if the code is
     * invalid.
     *
     * @param exception the SystemException
     * @return the corresponding HttpStatusCode
     */
    private HttpStatusCode getHttpStatusCodeFromSystemException(final SystemException exception) {
        try {
            return HttpStatusCode.valueOf(exception.getStatusCode());
        } catch (final IllegalArgumentException iae) {
            logger.info(LOG, ERROR_MESSAGE + exception.getStatusCode());
            return INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * Determines the HTTP status code from a general Exception. Maps specific exception types to appropriate status
     * codes.
     *
     * @param exception the Exception
     * @return the corresponding HttpStatusCode
     */
    private HttpStatusCode getHttpStatusCodeFromException(final Exception exception) {
        if (exception instanceof HttpClientErrorException) {
            return ((HttpClientErrorException) exception).getStatusCode();
        } else if (exception instanceof HttpRequestMethodNotSupportedException) {
            return METHOD_NOT_ALLOWED;
        } else if (exception instanceof MethodArgumentNotValidException
            || exception instanceof BindException
            || exception instanceof ConstraintViolationException
            || exception instanceof MissingServletRequestParameterException
            || exception instanceof MissingPathVariableException
            || exception instanceof MethodArgumentTypeMismatchException
            || exception instanceof ServletRequestBindingException
            || exception instanceof HttpMessageNotReadableException
            || exception instanceof HttpMediaTypeNotSupportedException) {
            return BAD_REQUEST;
        }
        final ResponseStatus responseStatus = exception.getClass().getAnnotation(ResponseStatus.class);
        if (responseStatus != null) {
            return HttpStatusCode.valueOf(responseStatus.code().value());
        }
        return INTERNAL_SERVER_ERROR;
    }
}
