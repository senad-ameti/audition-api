package unit.com.audition.web.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.web.validation.PositiveInteger;
import com.audition.web.validation.PositiveIntegerValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PositiveIntegerValidator Unit Tests")
class PositiveIntegerValidatorTest {

    private PositiveIntegerValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private PositiveInteger constraintAnnotation;

    @BeforeEach
    void setUp() {
        validator = new PositiveIntegerValidator();
        when(constraintAnnotation.fieldName()).thenReturn("Post ID");
        when(constraintAnnotation.optional()).thenReturn(false);
        validator.initialize(constraintAnnotation);
    }

    @Nested
    @DisplayName("Valid Input Tests")
    class ValidInputTests {

        @ParameterizedTest
        @ValueSource(strings = {"1", "10", "100", "999", "2147483647"})
        @DisplayName("should return true for positive integers")
        void shouldReturnTrueForPositiveIntegers(String value) {
            boolean result = validator.isValid(value, context);

            assertTrue(result);
            verify(context, never()).disableDefaultConstraintViolation();
        }

        @ParameterizedTest
        @ValueSource(strings = {" 1", "1 ", " 1 ", "  10  "})
        @DisplayName("should return true for positive integers with whitespace")
        void shouldReturnTrueForPositiveIntegersWithWhitespace(String value) {
            boolean result = validator.isValid(value, context);

            assertTrue(result);
            verify(context, never()).disableDefaultConstraintViolation();
        }
    }

    @Nested
    @DisplayName("Null and Empty Input Tests")
    class NullAndEmptyInputTests {

        @ParameterizedTest
        @NullSource
        @DisplayName("should return false for null value")
        void shouldReturnFalseForNullValue(String value) {
            when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

            boolean result = validator.isValid(value, context);

            assertFalse(result);
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate("Post ID cannot be null or empty");
            verify(violationBuilder).addConstraintViolation();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        @DisplayName("should return false for empty or blank values")
        void shouldReturnFalseForEmptyOrBlankValues(String value) {
            when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

            boolean result = validator.isValid(value, context);

            assertFalse(result);
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate("Post ID cannot be null or empty");
            verify(violationBuilder).addConstraintViolation();
        }
    }

    @Nested
    @DisplayName("Non-Positive Integer Tests")
    class NonPositiveIntegerTests {

        @Test
        @DisplayName("should return false for zero")
        void shouldReturnFalseForZero() {
            when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

            boolean result = validator.isValid("0", context);

            assertFalse(result);
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate("Post ID must be a positive integer");
            verify(violationBuilder).addConstraintViolation();
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1", "-10", "-100", "-2147483648"})
        @DisplayName("should return false for negative integers")
        void shouldReturnFalseForNegativeIntegers(String value) {
            when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

            boolean result = validator.isValid(value, context);

            assertFalse(result);
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate("Post ID must be a positive integer");
            verify(violationBuilder).addConstraintViolation();
        }
    }

    @Nested
    @DisplayName("Invalid Format Tests")
    class InvalidFormatTests {

        @ParameterizedTest
        @ValueSource(strings = {"abc", "1.5", "1a", "a1", "1,000", "1_000", "one", "1e10"})
        @DisplayName("should return false for non-integer strings")
        void shouldReturnFalseForNonIntegerStrings(String value) {
            when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

            boolean result = validator.isValid(value, context);

            assertFalse(result);
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate("Post ID must be a valid integer");
            verify(violationBuilder).addConstraintViolation();
        }

        @ParameterizedTest
        @ValueSource(strings = {"2147483648", "9999999999999999999"})
        @DisplayName("should return false for values exceeding integer range")
        void shouldReturnFalseForValuesExceedingIntegerRange(String value) {
            when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

            boolean result = validator.isValid(value, context);

            assertFalse(result);
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate("Post ID must be a valid integer");
            verify(violationBuilder).addConstraintViolation();
        }
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("should initialize without errors")
        void shouldInitializeWithoutErrors() {
            PositiveIntegerValidator newValidator = new PositiveIntegerValidator();
            when(constraintAnnotation.fieldName()).thenReturn("Test Field");
            when(constraintAnnotation.optional()).thenReturn(false);
            newValidator.initialize(constraintAnnotation);
            // No exception should be thrown
            assertTrue(true);
        }

        @Test
        @DisplayName("should use custom field name in error messages")
        void shouldUseCustomFieldNameInErrorMessages() {
            PositiveIntegerValidator customValidator = new PositiveIntegerValidator();
            when(constraintAnnotation.fieldName()).thenReturn("Custom Field");
            when(constraintAnnotation.optional()).thenReturn(false);
            customValidator.initialize(constraintAnnotation);
            when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

            boolean result = customValidator.isValid("0", context);

            assertFalse(result);
            verify(context).buildConstraintViolationWithTemplate("Custom Field must be a positive integer");
        }

        @Test
        @DisplayName("should return true for null when optional is true")
        void shouldReturnTrueForNullWhenOptional() {
            PositiveIntegerValidator optionalValidator = new PositiveIntegerValidator();
            when(constraintAnnotation.fieldName()).thenReturn("Optional Field");
            when(constraintAnnotation.optional()).thenReturn(true);
            optionalValidator.initialize(constraintAnnotation);

            boolean result = optionalValidator.isValid(null, context);

            assertTrue(result);
            verify(context, never()).disableDefaultConstraintViolation();
        }

        @Test
        @DisplayName("should return true for blank when optional is true")
        void shouldReturnTrueForBlankWhenOptional() {
            PositiveIntegerValidator optionalValidator = new PositiveIntegerValidator();
            when(constraintAnnotation.fieldName()).thenReturn("Optional Field");
            when(constraintAnnotation.optional()).thenReturn(true);
            optionalValidator.initialize(constraintAnnotation);

            boolean result = optionalValidator.isValid("  ", context);

            assertTrue(result);
            verify(context, never()).disableDefaultConstraintViolation();
        }

        @Test
        @DisplayName("should still validate positive integer when optional is true and value provided")
        void shouldValidatePositiveIntegerWhenOptionalAndValueProvided() {
            PositiveIntegerValidator optionalValidator = new PositiveIntegerValidator();
            when(constraintAnnotation.fieldName()).thenReturn("Optional Field");
            when(constraintAnnotation.optional()).thenReturn(true);
            optionalValidator.initialize(constraintAnnotation);
            when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

            boolean result = optionalValidator.isValid("-1", context);

            assertFalse(result);
            verify(context).buildConstraintViolationWithTemplate("Optional Field must be a positive integer");
        }
    }
}

