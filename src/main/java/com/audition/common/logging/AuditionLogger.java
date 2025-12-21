package com.audition.common.logging;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * A utility component for logging messages using SLF4J Logger with level checks to avoid unnecessary string
 * operations.
 */
@Component
public class AuditionLogger {

    /**
     * Logs an info message if the info level is enabled.
     *
     * @param logger  the SLF4J logger to use for logging
     * @param message the message to log
     */
    public void info(final Logger logger, final String message) {
        if (logger.isInfoEnabled()) {
            logger.info(message);
        }
    }

    /**
     * Logs an info message with an object parameter if the info level is enabled.
     *
     * @param logger  the SLF4J logger to use for logging
     * @param message the message to log
     * @param object  the object to include in the log message
     */
    public void info(final Logger logger, final String message, final Object object) {
        if (logger.isInfoEnabled()) {
            logger.info(message, object);
        }
    }

    /**
     * Logs a debug message if the debug level is enabled.
     *
     * @param logger  the SLF4J logger to use for logging
     * @param message the message to log
     */
    public void debug(final Logger logger, final String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }

    /**
     * Logs a warn message if the warn level is enabled.
     *
     * @param logger  the SLF4J logger to use for logging
     * @param message the message to log
     */
    public void warn(final Logger logger, final String message) {
        if (logger.isWarnEnabled()) {
            logger.warn(message);
        }
    }

    /**
     * Logs an error message if the error level is enabled.
     *
     * @param logger  the SLF4J logger to use for logging
     * @param message the message to log
     */
    public void error(final Logger logger, final String message) {
        if (logger.isErrorEnabled()) {
            logger.error(message);
        }
    }

    /**
     * Logs an error message with an exception if the error level is enabled.
     *
     * @param logger  the SLF4J logger to use for logging
     * @param message the message to log
     * @param e       the exception to include in the log
     */
    public void logErrorWithException(final Logger logger, final String message, final Exception e) {
        if (logger.isErrorEnabled()) {
            logger.error(message, e);
        }
    }

    /**
     * Logs a standard ProblemDetail as an error with an exception if the error level is enabled.
     *
     * @param logger        the SLF4J logger to use for logging
     * @param problemDetail the ProblemDetail to log
     * @param e             the exception to include in the log
     */
    public void logStandardProblemDetail(final Logger logger, final ProblemDetail problemDetail, final Exception e) {
        if (logger.isErrorEnabled()) {
            final var message = createStandardProblemDetailMessage(problemDetail);
            logger.error(message, e);
        }
    }

    /**
     * Logs an HTTP status code error message if the error level is enabled.
     *
     * @param logger    the SLF4J logger to use for logging
     * @param message   the message to log
     * @param errorCode the HTTP error code
     */
    public void logHttpStatusCodeError(final Logger logger, final String message, final Integer errorCode) {
        if (logger.isErrorEnabled()) {
            logger.error(createBasicErrorResponseMessage(errorCode, message) + "\n");
        }
    }

    /**
     * Creates a standard message string from a ProblemDetail.
     *
     * @param standardProblemDetail the ProblemDetail to format
     * @return the formatted message string
     */
    private String createStandardProblemDetailMessage(final ProblemDetail standardProblemDetail) {
        if (standardProblemDetail == null) {
            return "ProblemDetail: No details available";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Status: ").append(standardProblemDetail.getStatus());
        sb.append(" | Title: ").append(
            StringUtils.isNotBlank(standardProblemDetail.getTitle())
                ? standardProblemDetail.getTitle() : "No title provided");
        sb.append(" | Detail: ").append(
            StringUtils.isNotBlank(standardProblemDetail.getDetail())
                ? standardProblemDetail.getDetail() : "No detail provided");

        if (standardProblemDetail.getType() != null) {
            sb.append(" | Type: ").append(standardProblemDetail.getType());
        }
        if (standardProblemDetail.getInstance() != null) {
            sb.append(" | Instance: ").append(standardProblemDetail.getInstance());
        }

        return sb.toString();
    }

    /**
     * Creates a basic error response message with error code and message.
     *
     * @param errorCode the error code
     * @param message   the message
     * @return the formatted error message string
     */
    private String createBasicErrorResponseMessage(final Integer errorCode, final String message) {
        final String codeStr = errorCode != null ? String.valueOf(errorCode) : "Unknown";
        final String msgStr = StringUtils.isNotBlank(message) ? message : "No message provided";
        return "Error Code: " + codeStr + " | Message: " + msgStr;
    }
}
