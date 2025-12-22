package unit.com.audition.web.validation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.audition.web.validation.PositiveInteger;
import com.audition.web.validation.PositiveIntegerValidator;
import jakarta.validation.Constraint;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PositiveInteger Annotation Tests")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PositiveIntegerTest {

    @Nested
    @DisplayName("Annotation Metadata Tests")
    class AnnotationMetadataTests {

        @Test
        @DisplayName("should have @Documented annotation")
        void shouldHaveDocumentedAnnotation() {
            final Documented documented = PositiveInteger.class.getAnnotation(Documented.class);
            assertNotNull(documented, "PositiveInteger should be annotated with @Documented");
        }

        @Test
        @DisplayName("should have @Constraint annotation with correct validator")
        void shouldHaveConstraintAnnotationWithCorrectValidator() {
            final Constraint constraint = PositiveInteger.class.getAnnotation(Constraint.class);
            assertNotNull(constraint, "PositiveInteger should be annotated with @Constraint");
            assertArrayEquals(
                new Class[]{PositiveIntegerValidator.class},
                constraint.validatedBy(),
                "Constraint should be validated by PositiveIntegerValidator"
            );
        }

        @Test
        @DisplayName("should have @Target annotation for PARAMETER and FIELD")
        void shouldHaveTargetAnnotationForParameterAndField() {
            final Target target = PositiveInteger.class.getAnnotation(Target.class);
            assertNotNull(target, "PositiveInteger should be annotated with @Target");
            final ElementType[] elementTypes = target.value();
            assertEquals(2, elementTypes.length, "Should target 2 element types");
            assertTrue(
                containsElementType(elementTypes, ElementType.PARAMETER),
                "Should target ElementType.PARAMETER"
            );
            assertTrue(
                containsElementType(elementTypes, ElementType.FIELD),
                "Should target ElementType.FIELD"
            );
        }

        @Test
        @DisplayName("should have @Retention annotation with RUNTIME policy")
        void shouldHaveRetentionAnnotationWithRuntimePolicy() {
            final Retention retention = PositiveInteger.class.getAnnotation(Retention.class);
            assertNotNull(retention, "PositiveInteger should be annotated with @Retention");
            assertEquals(
                RetentionPolicy.RUNTIME,
                retention.value(),
                "Retention policy should be RUNTIME"
            );
        }

        private boolean containsElementType(final ElementType[] types, final ElementType target) {
            for (final ElementType type : types) {
                if (type == target) {
                    return true;
                }
            }
            return false;
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("should have default message")
        void shouldHaveDefaultMessage() throws NoSuchMethodException {
            final Method messageMethod = PositiveInteger.class.getMethod("message");
            final Object defaultValue = messageMethod.getDefaultValue();
            assertEquals("must be a positive integer", defaultValue, "Default message should match");
        }

        @Test
        @DisplayName("should have empty groups as default")
        void shouldHaveEmptyGroupsAsDefault() throws NoSuchMethodException {
            final Method groupsMethod = PositiveInteger.class.getMethod("groups");
            final Object defaultValue = groupsMethod.getDefaultValue();
            assertNotNull(defaultValue);
            assertInstanceOf(Class[].class, defaultValue, "groups should return Class array");
            assertEquals(0, ((Class<?>[]) defaultValue).length, "groups should be empty by default");
        }

        @Test
        @DisplayName("should have empty payload as default")
        void shouldHaveEmptyPayloadAsDefault() throws NoSuchMethodException {
            final Method payloadMethod = PositiveInteger.class.getMethod("payload");
            final Object defaultValue = payloadMethod.getDefaultValue();
            assertNotNull(defaultValue);
            assertInstanceOf(Class[].class, defaultValue, "payload should return Class array");
            assertEquals(0, ((Class<?>[]) defaultValue).length, "payload should be empty by default");
        }

        @Test
        @DisplayName("should have default fieldName of 'Value'")
        void shouldHaveDefaultFieldName() throws NoSuchMethodException {
            final Method fieldNameMethod = PositiveInteger.class.getMethod("fieldName");
            final Object defaultValue = fieldNameMethod.getDefaultValue();
            assertEquals("Value", defaultValue, "Default fieldName should be 'Value'");
        }

        @Test
        @DisplayName("should have default optional value of false")
        void shouldHaveDefaultOptionalFalse() throws NoSuchMethodException {
            final Method optionalMethod = PositiveInteger.class.getMethod("optional");
            final Object defaultValue = optionalMethod.getDefaultValue();
            assertEquals(Boolean.FALSE, defaultValue, "Default optional should be false");
        }
    }

    @Nested
    @DisplayName("Annotation Application Tests")
    class AnnotationApplicationTests {

        @SuppressWarnings("unused")
        @PositiveInteger
        private String annotatedField;

        @Test
        @DisplayName("should be applicable to fields")
        void shouldBeApplicableToFields() throws NoSuchFieldException {
            final PositiveInteger annotation = AnnotationApplicationTests.class
                .getDeclaredField("annotatedField")
                .getAnnotation(PositiveInteger.class);
            assertNotNull(annotation, "Field should have @PositiveInteger annotation");
            assertEquals("must be a positive integer", annotation.message());
        }

        @Test
        @DisplayName("should be applicable to parameters")
        void shouldBeApplicableToParameters() throws NoSuchMethodException {
            final Annotation[][] parameterAnnotations = AnnotationApplicationTests.class
                .getDeclaredMethod("methodWithAnnotatedParameter", String.class)
                .getParameterAnnotations();
            assertEquals(1, parameterAnnotations.length);
            assertEquals(1, parameterAnnotations[0].length);
            assertInstanceOf(PositiveInteger.class, parameterAnnotations[0][0]);
        }

        @SuppressWarnings("unused")
        void methodWithAnnotatedParameter(@PositiveInteger final String value) {
            // Method for testing parameter annotation
        }
    }

    @Nested
    @DisplayName("Custom Message Tests")
    class CustomMessageTests {

        @SuppressWarnings("unused")
        @PositiveInteger(message = "Custom error message")
        private String fieldWithCustomMessage;

        @SuppressWarnings("unused")
        @PositiveInteger(fieldName = "User ID")
        private String fieldWithCustomFieldName;

        @SuppressWarnings("unused")
        @PositiveInteger(optional = true)
        private String fieldWithOptionalTrue;

        @Test
        @DisplayName("should support custom message")
        void shouldSupportCustomMessage() throws NoSuchFieldException {
            final PositiveInteger annotation = CustomMessageTests.class
                .getDeclaredField("fieldWithCustomMessage")
                .getAnnotation(PositiveInteger.class);
            assertEquals("Custom error message", annotation.message());
        }

        @Test
        @DisplayName("should support custom fieldName")
        void shouldSupportCustomFieldName() throws NoSuchFieldException {
            final PositiveInteger annotation = CustomMessageTests.class
                .getDeclaredField("fieldWithCustomFieldName")
                .getAnnotation(PositiveInteger.class);
            assertEquals("User ID", annotation.fieldName());
        }

        @Test
        @DisplayName("should support optional=true")
        void shouldSupportOptionalTrue() throws NoSuchFieldException {
            final PositiveInteger annotation = CustomMessageTests.class
                .getDeclaredField("fieldWithOptionalTrue")
                .getAnnotation(PositiveInteger.class);
            assertTrue(annotation.optional());
        }
    }
}

