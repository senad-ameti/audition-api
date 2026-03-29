package com.audition.common.logging;

import java.net.URI;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class AuditionLogger {

    public void info(final Logger logger, final String message) {
        if (logger.isInfoEnabled()) {
            logger.info(message);
        }
    }

    public void info(final Logger logger, final String message, final Object object) {
        if (logger.isInfoEnabled()) {
            logger.info(message, object);
        }
    }

    public void debug(final Logger logger, final String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }

    public void warn(final Logger logger, final String message) {
        if (logger.isWarnEnabled()) {
            logger.warn(message);
        }
    }

    public void error(final Logger logger, final String message) {
        if (logger.isErrorEnabled()) {
            logger.error(message);
        }
    }

    public void logErrorWithException(final Logger logger, final String message, final Exception e) {
        if (logger.isErrorEnabled()) {
            logger.error(message, e);
        }
    }

    public void logStandardProblemDetail(final Logger logger, final ProblemDetail problemDetail, final Exception e) {
        if (logger.isErrorEnabled()) {
            final var message = createStandardProblemDetailMessage(problemDetail);
            logger.error(message, e);
        }
    }

    public void logHttpStatusCodeError(final Logger logger, final String message, final Integer errorCode) {
        if (logger.isErrorEnabled()) {
            logger.error(createBasicErrorResponseMessage(errorCode, message) + "\n");
        }
    }

    private String createStandardProblemDetailMessage(final ProblemDetail standardProblemDetail) {
        return String.format("Problem Detail Error: status=%d, title=%s, detail=%s, instance=%s",
            standardProblemDetail.getStatus(),
            Optional.ofNullable(standardProblemDetail.getTitle()).orElse("N/A"),
            Optional.ofNullable(standardProblemDetail.getDetail()).orElse("N/A"),
            Optional.ofNullable(standardProblemDetail.getInstance())
                .map(URI::toString)
                .orElse("N/A")
        );
    }

    private String createBasicErrorResponseMessage(final Integer errorCode, final String message) {
        return String.format("Error Code=%d, Message=%s", errorCode, message);
    }
}
