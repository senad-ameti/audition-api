package com.audition.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.common.logging.AuditionLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public class AuditionLoggerTest {

    private final AuditionLogger auditionLogger = new AuditionLogger();
    private final Logger logger = mock(Logger.class);

    @BeforeEach
    void setup() {
        when(logger.isInfoEnabled()).thenReturn(true);
        when(logger.isDebugEnabled()).thenReturn(true);
        when(logger.isWarnEnabled()).thenReturn(true);
        when(logger.isErrorEnabled()).thenReturn(true);
    }

    @Test
    void info_enabledAndDisabled_successful() {
        auditionLogger.info(logger, "hello");
        verify(logger).info("hello");

        // disabled check
        when(logger.isInfoEnabled()).thenReturn(false);
        auditionLogger.info(logger, "shouldNotLog");
        verify(logger, never()).info("shouldNotLog");
    }

    @Test
    void infoWithObject_enabled_successful() {
        final Object obj = new Object();
        auditionLogger.info(logger, "msg {}", obj);
        verify(logger).info("msg {}", obj);
    }

    @Test
    void debugAndWarnAndError_enabledAndDisabled_successful() {
        auditionLogger.debug(logger, "debug");
        verify(logger).debug("debug");

        auditionLogger.warn(logger, "warn");
        verify(logger).warn("warn");

        auditionLogger.error(logger, "error");
        verify(logger).error("error");

        // disabled checks
        when(logger.isDebugEnabled()).thenReturn(false);
        auditionLogger.debug(logger, "debugDisabled");
        verify(logger, never()).debug("debugDisabled");

        when(logger.isWarnEnabled()).thenReturn(false);
        auditionLogger.warn(logger, "warnDisabled");
        verify(logger, never()).warn("warnDisabled");

        when(logger.isErrorEnabled()).thenReturn(false);
        auditionLogger.error(logger, "errorDisabled");
        verify(logger, never()).error("errorDisabled");
    }

    @Test
    void logErrorWithException_validException_successful() {
        final RuntimeException exception = new RuntimeException("unexpected error");
        auditionLogger.logErrorWithException(logger, "error", exception);

        verify(logger).error("error", exception);
    }

    @Test
    void logStandardProblemDetail_validException_successful() {
        final ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Bad Request");
        pd.setDetail("bad detail");

        final RuntimeException exception = new RuntimeException("bad request exception");

        auditionLogger.logStandardProblemDetail(logger, pd, exception);

        final ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).error(msgCaptor.capture(), eq(exception));

        final String logged = msgCaptor.getValue();
        assertThat(logged).isNotNull();
        assertThat(logged).contains("Bad Request").contains("bad detail");
    }

    @Test
    void logHttpStatusCodeError_formatsMessage_andAppendsNewline_whenErrorEnabled() {
        auditionLogger.logHttpStatusCodeError(logger, "server error", 500);

        final ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).error(msgCaptor.capture());

        final String logged = msgCaptor.getValue();
        assertThat(logged).contains("Error Code: 500");
        assertThat(logged).contains("server error");
    }
}
