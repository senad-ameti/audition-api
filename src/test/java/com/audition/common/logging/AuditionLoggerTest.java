package com.audition.common.logging;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.http.ProblemDetail;

@ExtendWith(MockitoExtension.class)
class AuditionLoggerTest {

    private static final String ENABLED_CASES = "enabledCases";

    @Mock
    private Logger mockLogger;

    private AuditionLogger auditionLogger;

    @BeforeEach
    void setUp() {
        auditionLogger = new AuditionLogger();
    }

    static Stream<Arguments> enabledCases() {
        return Stream.of(Arguments.of(true, 1), Arguments.of(false, 0));
    }

    @ParameterizedTest
    @MethodSource(ENABLED_CASES)
    void infoWhenEnabledLogsMessage(final boolean enabled, final int expectedInvocations) {
        when(mockLogger.isInfoEnabled()).thenReturn(enabled);
        auditionLogger.info(mockLogger, "test message");
        verify(mockLogger, times(expectedInvocations)).info("test message");
    }

    @ParameterizedTest
    @MethodSource(ENABLED_CASES)
    void infoWithArgs(final boolean enabled, final int expectedInvocations) {
        when(mockLogger.isInfoEnabled()).thenReturn(enabled);
        auditionLogger.info(mockLogger, "test {}", "arg");
        verify(mockLogger, times(expectedInvocations)).info("test {}", "arg");
    }

    @ParameterizedTest
    @MethodSource(ENABLED_CASES)
    void debugLogsMessage(final boolean enabled, final int expectedInvocations) {
        when(mockLogger.isDebugEnabled()).thenReturn(enabled);
        auditionLogger.debug(mockLogger, "debug msg");
        verify(mockLogger, times(expectedInvocations)).debug("debug msg");
    }

    @ParameterizedTest
    @MethodSource(ENABLED_CASES)
    void warnLogsMessage(final boolean enabled, final int expectedInvocations) {
        when(mockLogger.isWarnEnabled()).thenReturn(enabled);
        auditionLogger.warn(mockLogger, "warn msg");
        verify(mockLogger, times(expectedInvocations)).warn("warn msg");
    }

    @ParameterizedTest
    @MethodSource(ENABLED_CASES)
    void errorLogsMessage(final boolean enabled, final int expectedInvocations) {
        when(mockLogger.isErrorEnabled()).thenReturn(enabled);
        auditionLogger.error(mockLogger, "error msg");
        verify(mockLogger, times(expectedInvocations)).error("error msg");
    }

    @ParameterizedTest
    @MethodSource(ENABLED_CASES)
    void logErrorWithExceptionLogsWithException(final boolean enabled, final int expectedInvocations) {
        when(mockLogger.isErrorEnabled()).thenReturn(enabled);
        final Exception ex = new RuntimeException("oops");
        auditionLogger.logErrorWithException(mockLogger, "error", ex);
        verify(mockLogger, times(expectedInvocations)).error("error", ex);
    }

    @ParameterizedTest
    @MethodSource("logStandardProblemDetailCases")
    void logStandardProblemDetailLogsFormatted(final String testName, final boolean enabled,
        final ProblemDetail pd, final String expectedContent) {
        when(mockLogger.isErrorEnabled()).thenReturn(enabled);
        final Exception ex = new RuntimeException("err");
        auditionLogger.logStandardProblemDetail(mockLogger, pd, ex);
        if (enabled) {
            verify(mockLogger).error(org.mockito.ArgumentMatchers.contains(expectedContent),
                org.mockito.ArgumentMatchers.eq(ex));
        } else {
            verify(mockLogger, never()).error(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Exception.class));
        }
    }

    static Stream<Arguments> logStandardProblemDetailCases() {
        final ProblemDetail pdWithInstance = ProblemDetail.forStatus(400);
        pdWithInstance.setTitle("Title");
        pdWithInstance.setDetail("Detail");
        pdWithInstance.setInstance(URI.create("/test"));

        final ProblemDetail pdWithoutInstance = ProblemDetail.forStatus(500);
        pdWithoutInstance.setTitle("Title");
        pdWithoutInstance.setDetail("Detail");

        return Stream.of(
            Arguments.of("withNonNullInstance", true, pdWithInstance, "/test"),
            Arguments.of("withNullInstance", true, pdWithoutInstance, "N/A"),
            Arguments.of("whenDisabled", false, ProblemDetail.forStatus(500), "")
        );
    }

    @ParameterizedTest
    @MethodSource(ENABLED_CASES)
    void logHttpStatusCodeErrorLogsFormatted(final boolean enabled, final int expectedInvocations) {
        when(mockLogger.isErrorEnabled()).thenReturn(enabled);
        auditionLogger.logHttpStatusCodeError(mockLogger, "Not Found", 404);
        verify(mockLogger, times(expectedInvocations)).error(org.mockito.ArgumentMatchers.contains("404"));
    }
}
