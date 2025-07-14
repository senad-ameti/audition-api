package com.audition.web.advice;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import io.micrometer.common.util.StringUtils;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Observed(name = "ExceptionControllerAdvice")
@ControllerAdvice
public class ExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    public static final String DEFAULT_TITLE = "API Error Occurred";
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionControllerAdvice.class);
    private static final String ERROR_MESSAGE = " Error Code from Exception could not be mapped to a valid HttpStatus Code - ";
    private static final String DEFAULT_MESSAGE = "API Error occurred. Please contact support or administrator.";

    private final AuditionLogger auditionLogger;

    private final Tracer tracer;

    public ExceptionControllerAdvice(final AuditionLogger auditionLogger, final Tracer tracer) {
        super();
        this.auditionLogger = auditionLogger;
        this.tracer = tracer;
    }

    @ExceptionHandler(HttpClientErrorException.class)
    ProblemDetail handleHttpClientException(final HttpClientErrorException e) {
        final ProblemDetail problemDetail = createProblemDetail(e, e.getStatusCode());
        if (LOG.isErrorEnabled()) {
            auditionLogger.error(LOG, problemDetail.toString());
        }
        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolationException(final ConstraintViolationException e) {
        final ProblemDetail problemDetail = createProblemDetail(e, HttpStatus.BAD_REQUEST);
        if (LOG.isErrorEnabled()) {
            auditionLogger.logErrorWithException(LOG, problemDetail.toString(), e);
        }
        return problemDetail;
    }

    @ExceptionHandler(Throwable.class)
    ProblemDetail handleMainException(final Throwable e) {
        final HttpStatusCode status = getHttpStatusCodeFromException(e);

        final ProblemDetail problemDetail = createProblemDetail(e, status);

        if (LOG.isErrorEnabled()) {
            auditionLogger.error(LOG, problemDetail.toString());
        }

        return problemDetail;
    }

    @ExceptionHandler(SystemException.class)
    ProblemDetail handleSystemException(final SystemException e) {
        final HttpStatusCode status = getHttpStatusCodeFromSystemException(e);
        final ProblemDetail problemDetail = createProblemDetail(e, status);

        if (LOG.isErrorEnabled()) {
            auditionLogger.error(LOG, problemDetail.toString());
        }
        return problemDetail;
    }

    private ProblemDetail createProblemDetail(
        final Throwable exception,
        final HttpStatusCode statusCode
    ) {
        final ProblemDetail problemDetail = ProblemDetail.forStatus(statusCode);
        problemDetail.setDetail(getMessageFromException(exception));

        if (exception instanceof SystemException systemException) {
            problemDetail.setTitle(systemException.getTitle());
        } else {
            problemDetail.setTitle(DEFAULT_TITLE);
        }

        problemDetail.setProperty("traceparent", getTraceParent());

        return problemDetail;
    }

    private String getMessageFromException(final Throwable exception) {
        if (exception instanceof SystemException systemException) {
            return systemException.getDetail();
        } else if (StringUtils.isNotBlank(exception.getMessage())) {
            return exception.getMessage();
        }
        return DEFAULT_MESSAGE;
    }

    private HttpStatusCode getHttpStatusCodeFromSystemException(final SystemException exception) {
        try {
            return HttpStatusCode.valueOf(exception.getStatusCode());
        } catch (final IllegalArgumentException iae) {
            if (LOG.isErrorEnabled()) {
                auditionLogger.error(LOG, ERROR_MESSAGE + exception.getStatusCode());
            }
            return INTERNAL_SERVER_ERROR;
        }
    }

    private HttpStatusCode getHttpStatusCodeFromException(final Throwable exception) {
        if (exception instanceof HttpStatusCodeException httpStatusCodeException) {
            return httpStatusCodeException.getStatusCode();
        } else if (exception instanceof HttpRequestMethodNotSupportedException) {
            return METHOD_NOT_ALLOWED;
        }

        return INTERNAL_SERVER_ERROR;
    }

    private String getTraceParent() {
        final Span span = tracer.currentSpan();
        if (span != null) {
            final String sampledFlag = Boolean.TRUE.equals(span.context().sampled()) ? "01" : "00";
            return "00-%s-%s-%s".formatted(span.context().traceId(), span.context().spanId(), sampledFlag);
        } else {
            return "00-00000000000000000000000000000000-0000000000000000-00";
        }
    }
}
