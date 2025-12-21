package com.audition.common.logging;

import org.apache.commons.lang3.StringUtils;
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

    private String createBasicErrorResponseMessage(final Integer errorCode, final String message) {
        final String codeStr = errorCode != null ? String.valueOf(errorCode) : "Unknown";
        final String msgStr = StringUtils.isNotBlank(message) ? message : "No message provided";
        return "Error Code: " + codeStr + " | Message: " + msgStr;
    }
}
