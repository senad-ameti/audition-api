package com.audition.common.logging;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.http.ProblemDetail;

@ExtendWith(MockitoExtension.class)
class AuditionLoggerTest {

    @Mock
    private Logger mockLogger;

    private AuditionLogger auditionLogger;

    @BeforeEach
    void setUp() {
        auditionLogger = new AuditionLogger();
    }

    @Test
    void info_whenEnabled_logsMessage() {
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        auditionLogger.info(mockLogger, "test message");
        verify(mockLogger).info("test message");
    }

    @Test
    void info_whenDisabled_doesNotLog() {
        when(mockLogger.isInfoEnabled()).thenReturn(false);
        auditionLogger.info(mockLogger, "test message");
        verify(mockLogger, never()).info("test message");
    }

    @Test
    void infoWithArg_whenEnabled_logsMessage() {
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        auditionLogger.info(mockLogger, "test {}", "arg");
        verify(mockLogger).info("test {}", "arg");
    }

    @Test
    void infoWithArg_whenDisabled_doesNotLog() {
        when(mockLogger.isInfoEnabled()).thenReturn(false);
        auditionLogger.info(mockLogger, "test {}", "arg");
        verify(mockLogger, never()).info("test {}", "arg");
    }

    @Test
    void debug_whenEnabled_logsMessage() {
        when(mockLogger.isDebugEnabled()).thenReturn(true);
        auditionLogger.debug(mockLogger, "debug msg");
        verify(mockLogger).debug("debug msg");
    }

    @Test
    void debug_whenDisabled_doesNotLog() {
        when(mockLogger.isDebugEnabled()).thenReturn(false);
        auditionLogger.debug(mockLogger, "debug msg");
        verify(mockLogger, never()).debug("debug msg");
    }

    @Test
    void warn_whenEnabled_logsMessage() {
        when(mockLogger.isWarnEnabled()).thenReturn(true);
        auditionLogger.warn(mockLogger, "warn msg");
        verify(mockLogger).warn("warn msg");
    }

    @Test
    void warn_whenDisabled_doesNotLog() {
        when(mockLogger.isWarnEnabled()).thenReturn(false);
        auditionLogger.warn(mockLogger, "warn msg");
        verify(mockLogger, never()).warn("warn msg");
    }

    @Test
    void error_whenEnabled_logsMessage() {
        when(mockLogger.isErrorEnabled()).thenReturn(true);
        auditionLogger.error(mockLogger, "error msg");
        verify(mockLogger).error("error msg");
    }

    @Test
    void error_whenDisabled_doesNotLog() {
        when(mockLogger.isErrorEnabled()).thenReturn(false);
        auditionLogger.error(mockLogger, "error msg");
        verify(mockLogger, never()).error("error msg");
    }

    @Test
    void logErrorWithException_whenEnabled_logsWithException() {
        when(mockLogger.isErrorEnabled()).thenReturn(true);
        Exception ex = new RuntimeException("oops");
        auditionLogger.logErrorWithException(mockLogger, "error", ex);
        verify(mockLogger).error("error", ex);
    }

    @Test
    void logErrorWithException_whenDisabled_doesNotLog() {
        when(mockLogger.isErrorEnabled()).thenReturn(false);
        auditionLogger.logErrorWithException(mockLogger, "error", new RuntimeException());
        verify(mockLogger, never()).error(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(Exception.class));
    }

    @Test
    void logStandardProblemDetail_withNonNullInstance_logsFormatted() {
        when(mockLogger.isErrorEnabled()).thenReturn(true);
        ProblemDetail pd = ProblemDetail.forStatus(400);
        pd.setTitle("Title");
        pd.setDetail("Detail");
        pd.setInstance(URI.create("/test"));
        Exception ex = new RuntimeException("err");
        auditionLogger.logStandardProblemDetail(mockLogger, pd, ex);
        verify(mockLogger).error(org.mockito.ArgumentMatchers.contains("/test"), org.mockito.ArgumentMatchers.eq(ex));
    }

    @Test
    void logStandardProblemDetail_withNullInstance_logsNA() {
        when(mockLogger.isErrorEnabled()).thenReturn(true);
        ProblemDetail pd = ProblemDetail.forStatus(500);
        pd.setTitle("Title");
        pd.setDetail("Detail");
        // instance is null by default
        Exception ex = new RuntimeException("err");
        auditionLogger.logStandardProblemDetail(mockLogger, pd, ex);
        verify(mockLogger).error(org.mockito.ArgumentMatchers.contains("N/A"), org.mockito.ArgumentMatchers.eq(ex));
    }

    @Test
    void logStandardProblemDetail_whenDisabled_doesNotLog() {
        when(mockLogger.isErrorEnabled()).thenReturn(false);
        auditionLogger.logStandardProblemDetail(mockLogger, ProblemDetail.forStatus(500), new RuntimeException());
        verify(mockLogger, never()).error(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(Exception.class));
    }

    @Test
    void logHttpStatusCodeError_whenEnabled_logsFormatted() {
        when(mockLogger.isErrorEnabled()).thenReturn(true);
        auditionLogger.logHttpStatusCodeError(mockLogger, "Not Found", 404);
        verify(mockLogger).error(org.mockito.ArgumentMatchers.contains("404"));
    }

    @Test
    void logHttpStatusCodeError_whenDisabled_doesNotLog() {
        when(mockLogger.isErrorEnabled()).thenReturn(false);
        auditionLogger.logHttpStatusCodeError(mockLogger, "Not Found", 404);
        verify(mockLogger, never()).error(org.mockito.ArgumentMatchers.anyString());
    }
}
