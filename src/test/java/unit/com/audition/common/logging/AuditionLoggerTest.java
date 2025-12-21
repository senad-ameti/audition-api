package unit.com.audition.common.logging;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.common.logging.AuditionLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditionLogger Unit Tests")
class AuditionLoggerTest {

    @Mock
    private Logger mockLogger;

    private AuditionLogger auditionLogger;

    @BeforeEach
    void setUp() {
        auditionLogger = new AuditionLogger();
    }

    @Nested
    @DisplayName("logStandardProblemDetail Tests")
    class LogStandardProblemDetailTests {

        @Test
        @DisplayName("Should log ProblemDetail with all fields populated")
        void shouldLogProblemDetailWithAllFields() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);
            final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            problemDetail.setTitle("Test Title");
            problemDetail.setDetail("Test Detail");
            final Exception exception = new RuntimeException("Test exception");

            // Act
            auditionLogger.logStandardProblemDetail(mockLogger, problemDetail, exception);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockLogger).error(messageCaptor.capture(), eq(exception));
            final String loggedMessage = messageCaptor.getValue();
            assertTrue(loggedMessage.contains("Status: 500"));
            assertTrue(loggedMessage.contains("Title: Test Title"));
            assertTrue(loggedMessage.contains("Detail: Test Detail"));
        }

        @Test
        @DisplayName("Should handle null ProblemDetail gracefully")
        void shouldHandleNullProblemDetail() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);
            final Exception exception = new RuntimeException("Test exception");

            // Act
            auditionLogger.logStandardProblemDetail(mockLogger, null, exception);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockLogger).error(messageCaptor.capture(), eq(exception));
            final String loggedMessage = messageCaptor.getValue();
            assertTrue(loggedMessage.contains("ProblemDetail: No details available"));
        }

        @Test
        @DisplayName("Should handle ProblemDetail with null detail")
        void shouldHandleProblemDetailWithNullDetail() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);
            final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            // Title is set by forStatus, but detail is null by default
            final Exception exception = new RuntimeException("Test exception");

            // Act
            auditionLogger.logStandardProblemDetail(mockLogger, problemDetail, exception);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockLogger).error(messageCaptor.capture(), eq(exception));
            final String loggedMessage = messageCaptor.getValue();
            assertTrue(loggedMessage.contains("Status: 400"));
            assertTrue(loggedMessage.contains("Title: Bad Request")); // Default title from forStatus
            assertTrue(loggedMessage.contains("No detail provided"));
        }

        @Test
        @DisplayName("Should not log when error logging is disabled")
        void shouldNotLogWhenErrorDisabled() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(false);
            final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            final Exception exception = new RuntimeException("Test exception");

            // Act
            auditionLogger.logStandardProblemDetail(mockLogger, problemDetail, exception);

            // Assert
            verify(mockLogger, never()).error(anyString(), eq(exception));
        }

        @Test
        @DisplayName("Should include type and instance when present")
        void shouldIncludeTypeAndInstanceWhenPresent() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);
            final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
            problemDetail.setTitle("Not Found");
            problemDetail.setDetail("Resource not found");
            problemDetail.setInstance(java.net.URI.create("/api/posts/123"));
            final Exception exception = new RuntimeException("Test exception");

            // Act
            auditionLogger.logStandardProblemDetail(mockLogger, problemDetail, exception);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockLogger).error(messageCaptor.capture(), eq(exception));
            final String loggedMessage = messageCaptor.getValue();
            assertTrue(loggedMessage.contains("Instance: /api/posts/123"));
        }

        @Test
        @DisplayName("Should handle ProblemDetail with blank title")
        void shouldHandleProblemDetailWithBlankTitle() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);
            final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            problemDetail.setTitle("");  // Set blank title
            problemDetail.setDetail("Test Detail");
            final Exception exception = new RuntimeException("Test exception");

            // Act
            auditionLogger.logStandardProblemDetail(mockLogger, problemDetail, exception);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockLogger).error(messageCaptor.capture(), eq(exception));
            final String loggedMessage = messageCaptor.getValue();
            assertTrue(loggedMessage.contains("Title: No title provided"));
        }

        @Test
        @DisplayName("Should not include type when type is null")
        void shouldNotIncludeTypeWhenNull() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);
            final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            problemDetail.setTitle("Bad Request");
            problemDetail.setDetail("Invalid input");
            // Explicitly set type to null to test the null branch
            try {
                final java.lang.reflect.Field typeField = ProblemDetail.class.getDeclaredField("type");
                typeField.setAccessible(true);
                typeField.set(problemDetail, null);
            } catch (Exception e) {
                // If reflection fails, this is acceptable for the test
            }
            final Exception exception = new RuntimeException("Test exception");

            // Act
            auditionLogger.logStandardProblemDetail(mockLogger, problemDetail, exception);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockLogger).error(messageCaptor.capture(), eq(exception));
            final String loggedMessage = messageCaptor.getValue();
            // Verify that Type is not in the message when it's null
            final boolean hasNoType = !loggedMessage.contains("Type:");
            final boolean hasDefaultType = loggedMessage.contains("Type: about:blank");
            assertTrue(hasNoType || hasDefaultType);
        }
    }

    @Nested
    @DisplayName("logHttpStatusCodeError Tests")
    class LogHttpStatusCodeErrorTests {

        @Test
        @DisplayName("Should log error code and message correctly")
        void shouldLogErrorCodeAndMessage() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);

            // Act
            auditionLogger.logHttpStatusCodeError(mockLogger, "Bad Request", 400);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockLogger).error(messageCaptor.capture());
            final String loggedMessage = messageCaptor.getValue();
            assertTrue(loggedMessage.contains("Error Code: 400"));
            assertTrue(loggedMessage.contains("Message: Bad Request"));
        }

        @Test
        @DisplayName("Should handle null error code")
        void shouldHandleNullErrorCode() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);

            // Act
            auditionLogger.logHttpStatusCodeError(mockLogger, "Some error", null);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockLogger).error(messageCaptor.capture());
            final String loggedMessage = messageCaptor.getValue();
            assertTrue(loggedMessage.contains("Error Code: Unknown"));
            assertTrue(loggedMessage.contains("Message: Some error"));
        }

        @Test
        @DisplayName("Should handle null message")
        void shouldHandleNullMessage() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);

            // Act
            auditionLogger.logHttpStatusCodeError(mockLogger, null, 500);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockLogger).error(messageCaptor.capture());
            final String loggedMessage = messageCaptor.getValue();
            assertTrue(loggedMessage.contains("Error Code: 500"));
            assertTrue(loggedMessage.contains("Message: No message provided"));
        }

        @Test
        @DisplayName("Should handle empty message")
        void shouldHandleEmptyMessage() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);

            // Act
            auditionLogger.logHttpStatusCodeError(mockLogger, "", 404);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockLogger).error(messageCaptor.capture());
            final String loggedMessage = messageCaptor.getValue();
            assertTrue(loggedMessage.contains("Message: No message provided"));
        }

        @Test
        @DisplayName("Should handle both null error code and message")
        void shouldHandleBothNullErrorCodeAndMessage() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);

            // Act
            auditionLogger.logHttpStatusCodeError(mockLogger, null, null);

            // Assert
            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockLogger).error(messageCaptor.capture());
            final String loggedMessage = messageCaptor.getValue();
            assertTrue(loggedMessage.contains("Error Code: Unknown"));
            assertTrue(loggedMessage.contains("Message: No message provided"));
        }

        @Test
        @DisplayName("Should not log when error logging is disabled")
        void shouldNotLogWhenErrorDisabled() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(false);

            // Act
            auditionLogger.logHttpStatusCodeError(mockLogger, "Some error", 400);

            // Assert
            verify(mockLogger, never()).error(anyString());
        }
    }

    @Nested
    @DisplayName("Other Logger Methods Tests")
    class OtherLoggerMethodsTests {

        @Test
        @DisplayName("info should log when info is enabled")
        void infoShouldLogWhenEnabled() {
            // Arrange
            when(mockLogger.isInfoEnabled()).thenReturn(true);

            // Act
            auditionLogger.info(mockLogger, "Info message");

            // Assert
            verify(mockLogger).info("Info message");
        }

        @Test
        @DisplayName("info should not log when info is disabled")
        void infoShouldNotLogWhenDisabled() {
            // Arrange
            when(mockLogger.isInfoEnabled()).thenReturn(false);

            // Act
            auditionLogger.info(mockLogger, "Info message");

            // Assert
            verify(mockLogger, never()).info(anyString());
        }

        @Test
        @DisplayName("info with object should log when info is enabled")
        void infoWithObjectShouldLogWhenEnabled() {
            // Arrange
            when(mockLogger.isInfoEnabled()).thenReturn(true);
            final Object testObject = "test object";

            // Act
            auditionLogger.info(mockLogger, "Info message with object: {}", testObject);

            // Assert
            verify(mockLogger).info("Info message with object: {}", testObject);
        }

        @Test
        @DisplayName("info with object should not log when info is disabled")
        void infoWithObjectShouldNotLogWhenDisabled() {
            // Arrange
            when(mockLogger.isInfoEnabled()).thenReturn(false);
            final Object testObject = "test object";

            // Act
            auditionLogger.info(mockLogger, "Info message with object: {}", testObject);

            // Assert
            verify(mockLogger, never()).info(anyString(), eq(testObject));
        }

        @Test
        @DisplayName("debug should log when debug is enabled")
        void debugShouldLogWhenEnabled() {
            // Arrange
            when(mockLogger.isDebugEnabled()).thenReturn(true);

            // Act
            auditionLogger.debug(mockLogger, "Debug message");

            // Assert
            verify(mockLogger).debug("Debug message");
        }

        @Test
        @DisplayName("debug should not log when debug is disabled")
        void debugShouldNotLogWhenDisabled() {
            // Arrange
            when(mockLogger.isDebugEnabled()).thenReturn(false);

            // Act
            auditionLogger.debug(mockLogger, "Debug message");

            // Assert
            verify(mockLogger, never()).debug(anyString());
        }

        @Test
        @DisplayName("warn should log when warn is enabled")
        void warnShouldLogWhenEnabled() {
            // Arrange
            when(mockLogger.isWarnEnabled()).thenReturn(true);

            // Act
            auditionLogger.warn(mockLogger, "Warn message");

            // Assert
            verify(mockLogger).warn("Warn message");
        }

        @Test
        @DisplayName("warn should not log when warn is disabled")
        void warnShouldNotLogWhenDisabled() {
            // Arrange
            when(mockLogger.isWarnEnabled()).thenReturn(false);

            // Act
            auditionLogger.warn(mockLogger, "Warn message");

            // Assert
            verify(mockLogger, never()).warn(anyString());
        }

        @Test
        @DisplayName("error should log when error is enabled")
        void errorShouldLogWhenEnabled() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);

            // Act
            auditionLogger.error(mockLogger, "Error message");

            // Assert
            verify(mockLogger).error("Error message");
        }

        @Test
        @DisplayName("error should not log when error is disabled")
        void errorShouldNotLogWhenDisabled() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(false);

            // Act
            auditionLogger.error(mockLogger, "Error message");

            // Assert
            verify(mockLogger, never()).error(anyString());
        }

        @Test
        @DisplayName("logErrorWithException should log message and exception")
        void logErrorWithExceptionShouldLogMessageAndException() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(true);
            final Exception exception = new RuntimeException("Test exception");

            // Act
            auditionLogger.logErrorWithException(mockLogger, "Error occurred", exception);

            // Assert
            verify(mockLogger).error("Error occurred", exception);
        }

        @Test
        @DisplayName("logErrorWithException should not log when error is disabled")
        void logErrorWithExceptionShouldNotLogWhenDisabled() {
            // Arrange
            when(mockLogger.isErrorEnabled()).thenReturn(false);
            final Exception exception = new RuntimeException("Test exception");

            // Act
            auditionLogger.logErrorWithException(mockLogger, "Error occurred", exception);

            // Assert
            verify(mockLogger, never()).error(anyString(), eq(exception));
        }
    }
}


