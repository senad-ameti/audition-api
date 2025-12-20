package com.audition.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class PositiveIntegerValidator implements ConstraintValidator<PositiveInteger, String> {

    private String fieldName;
    private boolean optional;

    @Override
    public void initialize(final PositiveInteger constraintAnnotation) {
        this.fieldName = constraintAnnotation.fieldName();
        this.optional = constraintAnnotation.optional();
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            if (optional) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(fieldName + " cannot be null or empty")
                .addConstraintViolation();
            return false;
        }

        try {
            final int intValue = Integer.parseInt(value.trim());
            if (intValue <= 0) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(fieldName + " must be a positive integer")
                    .addConstraintViolation();
                return false;
            }
            return true;
        } catch (final NumberFormatException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(fieldName + " must be a valid integer")
                .addConstraintViolation();
            return false;
        }
    }
}

