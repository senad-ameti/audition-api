package com.audition.common.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.springframework.http.ProblemDetail;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;


@SuppressWarnings("PMD.TooManyMethods")
class AuditionLoggerTest {

    private AuditionLogger auditionLogger;

    @Mock
    private Logger logger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auditionLogger = new AuditionLogger();
    }

    @ParameterizedTest
    @CsvSource({
            "info,true",
            "info,false",
            "debug,true",
            "debug,false",
            "warn,true",
            "warn,false",
            "error,true",
            "error,false"
    })
    void logLevelTest(final String level, final boolean enabled) {
        final String msg = "test message";

        setLoggerLevel(level, enabled);
        logMessage(level, msg);
        verifyLogger(level, enabled, msg);

        verifyNoMoreInteractions(logger);
    }

    private void setLoggerLevel(final String level, final boolean enabled) {
        switch (level) {
            case "info" -> when(logger.isInfoEnabled()).thenReturn(enabled);
            case "debug" -> when(logger.isDebugEnabled()).thenReturn(enabled);
            case "warn" -> when(logger.isWarnEnabled()).thenReturn(enabled);
            case "error" -> when(logger.isErrorEnabled()).thenReturn(enabled);
            default -> throw new IllegalArgumentException("Unexpected log level: " + level);
        }
    }

    private void logMessage(final String level, final String msg) {
        switch (level) {
            case "info" -> auditionLogger.info(logger, msg);
            case "debug" -> auditionLogger.debug(logger, msg);
            case "warn" -> auditionLogger.warn(logger, msg);
            case "error" -> auditionLogger.error(logger, msg);
            default -> throw new IllegalArgumentException("Unexpected log level: " + level);
        }
    }

    private void verifyLogger(final String level, final boolean enabled, final String msg) {
        if ("info".equals(level)) {
            verify(logger).isInfoEnabled();
            verifyIfEnabled(enabled, () -> verify(logger).info(msg));
        } else if ("debug".equals(level)) {
            verify(logger).isDebugEnabled();
            verifyIfEnabled(enabled, () -> verify(logger).debug(msg));
        } else if ("warn".equals(level)) {
            verify(logger).isWarnEnabled();
            verifyIfEnabled(enabled, () -> verify(logger).warn(msg));
        } else if ("error".equals(level)) {
            verify(logger).isErrorEnabled();
            verifyIfEnabled(enabled, () -> verify(logger).error(msg));
        } else {
            throw new IllegalArgumentException("Unexpected log level: " + level);
        }
    }

    // Helper to reduce cyclomatic complexity
    private void verifyIfEnabled(final boolean enabled, final Runnable verification) {
        if (enabled) {
            verification.run();
        }
    }

    @Test
    void logErrorWithExceptionTest() {
        when(logger.isErrorEnabled()).thenReturn(true);
        final Exception e = new RuntimeException("boom");

        auditionLogger.logErrorWithException(logger, "error with exception", e);

        verify(logger).isErrorEnabled();
        verify(logger).error("error with exception", e);
        verifyNoMoreInteractions(logger);
    }

    @Test
    void logErrorWithExceptionDisabledTest() {
        when(logger.isErrorEnabled()).thenReturn(false);

        auditionLogger.logErrorWithException(logger, "error with exception", new RuntimeException());

        verify(logger).isErrorEnabled();
        verifyNoMoreInteractions(logger);
    }

    @Test
    void logStandardProblemDetailEnabledTest() {
        when(logger.isErrorEnabled()).thenReturn(true);
        final Exception e = new RuntimeException("problem");

        final ProblemDetail problemDetail = ProblemDetail.forStatus(404);
        problemDetail.setTitle("Not Found");
        problemDetail.setDetail("Resource missing");
        problemDetail.setInstance(java.net.URI.create("/test"));

        auditionLogger.logStandardProblemDetail(logger, problemDetail, e);

        verify(logger).isErrorEnabled();
        verify(logger).error(
                "Problem occurred [title=Not Found, status=404, detail=Resource missing, instance=/test]",
                e
        );
        verifyNoMoreInteractions(logger);
    }

    @Test
    void logStandardProblemDetailDisabledTest() {
        when(logger.isErrorEnabled()).thenReturn(false);
        final ProblemDetail problemDetail = ProblemDetail.forStatus(400);

        auditionLogger.logStandardProblemDetail(logger, problemDetail, new RuntimeException());

        verify(logger).isErrorEnabled();
        verifyNoMoreInteractions(logger);
    }

    @Test
    void logHttpStatusCodeErrorEnabledTest() {
        when(logger.isErrorEnabled()).thenReturn(true);

        auditionLogger.logHttpStatusCodeError(logger, "Bad request", 400);

        verify(logger).isErrorEnabled();
        verify(logger).error("Error [statusCode=400, message=Bad request]\n");
        verifyNoMoreInteractions(logger);
    }

    @Test
    void logHttpStatusCodeErrorDisabledTest() {
        when(logger.isErrorEnabled()).thenReturn(false);

        auditionLogger.logHttpStatusCodeError(logger, "Bad request", 400);

        verify(logger).isErrorEnabled();
        verifyNoMoreInteractions(logger);
    }
}
