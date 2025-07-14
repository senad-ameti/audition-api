package com.audition.common.logging;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class AuditionLoggerTest {

    private final AuditionLogger auditionLogger = new AuditionLogger();

    private final Logger logger = mock(Logger.class);

    @AfterEach
    void clear() {
        Mockito.clearInvocations(logger);
    }

    @Test
    void shouldCallDebugWithCorrectMessage() {
        when(logger.isDebugEnabled()).thenReturn(true);

        auditionLogger.debug(logger, "debug");
        verify(logger, times(1)).isDebugEnabled();
        verify(logger, times(1)).debug("debug");

        auditionLogger.debug(logger, "debug {}", 1);
        verify(logger, times(1)).debug("debug {}", 1);
    }

    @Test
    void shouldCallInfoWithCorrectMessage() {
        when(logger.isInfoEnabled()).thenReturn(true);

        auditionLogger.info(logger, "info");
        verify(logger, times(1)).isInfoEnabled();
        verify(logger, times(1)).info("info");

        auditionLogger.info(logger, "info {}", 1);
        verify(logger, times(1)).info("info {}", 1);
    }

    @Test
    void shouldCallWarnWithCorrectMessage() {
        when(logger.isWarnEnabled()).thenReturn(true);

        auditionLogger.warn(logger, "warn");
        verify(logger, times(1)).isWarnEnabled();
        verify(logger, times(1)).warn("warn");
    }

    @Test
    void shouldCallErrorWithCorrectMessage() {
        when(logger.isErrorEnabled()).thenReturn(true);

        auditionLogger.error(logger, "error");
        verify(logger, times(1)).isErrorEnabled();
        verify(logger, times(1)).error("error");

        when(logger.isErrorEnabled()).thenReturn(false);
        auditionLogger.error(logger, "error1");
        verify(logger, times(0)).error("error1");
    }

    @Test
    void shouldCallLogErrorWithExceptionWithCorrectMessage() {
        when(logger.isErrorEnabled()).thenReturn(true);
        final IllegalArgumentException e = new IllegalArgumentException();

        auditionLogger.logErrorWithException(logger, "error", e);
        verify(logger, times(1)).error("error", e);
    }

    @Test
    void shouldCallLogStandardProblemDetailWithCorrectMessage() {
        when(logger.isErrorEnabled()).thenReturn(true);

        final IllegalArgumentException e = new IllegalArgumentException();
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "detail");

        auditionLogger.logStandardProblemDetail(logger, problemDetail, e);

        verify(logger, times(1)).error(problemDetail.toString(), e);
    }

    @Test
    void shouldCallLogHttpStatusCodeErrorWithCorrectMessage() {
        when(logger.isErrorEnabled()).thenReturn(true);

        auditionLogger.logHttpStatusCodeError(logger, "problemDetail", 500);

        verify(logger, times(1)).error(contains("{}"), eq("errorCode: 500, message: problemDetail"));
    }

}
